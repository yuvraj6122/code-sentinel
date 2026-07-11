package com.codesentinel.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codesentinel.agent.ComplexityAnalysisAgent;
import com.codesentinel.agent.DuplicateCodeAnalysisAgent;
import com.codesentinel.agent.RepositoryScannerAgent;
import com.codesentinel.agent.SecurityAnalysisAgent;
import com.codesentinel.agent.TestingAnalysisAgent;
import com.codesentinel.agent.TestingAnalysisResult;
import com.codesentinel.agent.TestingMetrics;
import com.codesentinel.dto.RepositoryMetadataDto;
import com.codesentinel.dto.UnifiedAnalysisResponse;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Analysis;
import com.codesentinel.model.AnalysisStatus;
import com.codesentinel.model.Finding;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.model.Severity;
import com.codesentinel.repository.AnalysisRepository;
import com.codesentinel.repository.FindingRepository;
import com.codesentinel.repository.RepositoryEntityRepository;
import com.codesentinel.service.RepositoryCloneService;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UnifiedAnalysisServiceImplTest {

	private static final Path CLONE_PATH = Path.of("/tmp/codesentinel/analysis-1");

	private final RepositoryEntityRepository repositoryEntityRepository =
			mock(RepositoryEntityRepository.class);
	private final AnalysisRepository analysisRepository = mock(AnalysisRepository.class);
	private final FindingRepository findingRepository = mock(FindingRepository.class);

	private final RepositoryCloneService cloneService = url -> CLONE_PATH;
	private final RepositoryScannerAgent scannerAgent =
			path -> new RepositoryMetadataDto("repo", "JAVA", "GRADLE", 12, 4);

	@BeforeEach
	void stubPersistence() {
		when(repositoryEntityRepository.save(any(RepositoryEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
			Analysis analysis = invocation.getArgument(0);
			if (analysis.getId() == null) {
				analysis.setId(99L);
			}
			return analysis;
		});
		when(findingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void runsEveryAgentUnderASingleAnalysisAndAggregatesFindings() {
		Finding complexity = finding(AgentType.COMPLEXITY, Severity.HIGH);
		Finding duplicate = finding(AgentType.DUPLICATE_CODE, Severity.MEDIUM);
		Finding testing = finding(AgentType.TESTING, Severity.LOW);
		Finding security = finding(AgentType.SECURITY, Severity.CRITICAL);

		UnifiedAnalysisServiceImpl service = service(
				path -> List.of(complexity),
				path -> List.of(duplicate),
				path -> testingResult(List.of(testing)),
				path -> List.of(security));

		UnifiedAnalysisResponse response = service.analyze("https://github.com/owner/repo");

		assertEquals(99L, response.getAnalysisId());
		assertEquals("COMPLETED", response.getStatus());
		assertEquals(4, response.getTotalFindings());
		assertEquals(1, response.getCriticalSeverity());
		assertEquals(1, response.getHighSeverity());

		// A single repository + analysis record backs the whole run.
		verify(repositoryEntityRepository, times(1)).save(any(RepositoryEntity.class));

		// All four agents' findings persist together, each linked to the one analysis.
		ArgumentCaptor<List<Finding>> savedFindings = findingCaptor();
		verify(findingRepository).saveAll(savedFindings.capture());
		assertEquals(4, savedFindings.getValue().size());
		Analysis analysis = savedFindings.getValue().get(0).getAnalysis();
		assertTrue(savedFindings.getValue().stream().allMatch(f -> f.getAnalysis() == analysis));

		// The analysis is completed at the end.
		ArgumentCaptor<Analysis> savedAnalysis = ArgumentCaptor.forClass(Analysis.class);
		verify(analysisRepository, times(2)).save(savedAnalysis.capture());
		assertEquals(AnalysisStatus.COMPLETED, savedAnalysis.getValue().getStatus());
	}

	@Test
	void continuesAndReturnsPartialWhenOneAgentFails() {
		Finding complexity = finding(AgentType.COMPLEXITY, Severity.HIGH);
		Finding duplicate = finding(AgentType.DUPLICATE_CODE, Severity.MEDIUM);
		Finding testing = finding(AgentType.TESTING, Severity.LOW);

		UnifiedAnalysisServiceImpl service = service(
				path -> List.of(complexity),
				path -> List.of(duplicate),
				path -> testingResult(List.of(testing)),
				path -> {
					throw new RuntimeException("SpotBugs exploded");
				});

		UnifiedAnalysisResponse response = service.analyze("https://github.com/owner/repo");

		assertEquals("PARTIAL", response.getStatus());
		// The three successful agents' findings are still aggregated and persisted.
		assertEquals(3, response.getTotalFindings());

		ArgumentCaptor<List<Finding>> savedFindings = findingCaptor();
		verify(findingRepository).saveAll(savedFindings.capture());
		assertEquals(3, savedFindings.getValue().size());

		// The failing agent is reported as FAILED with its message; others COMPLETED.
		var securityExecution = response.getAgentExecutions().stream()
				.filter(execution -> execution.getAgent().equals(AgentType.SECURITY.name()))
				.findFirst()
				.orElseThrow();
		assertEquals("FAILED", securityExecution.getStatus());
		assertEquals("SpotBugs exploded", securityExecution.getMessage());
		assertTrue(response.getAgentExecutions().stream()
				.filter(execution -> !execution.getAgent().equals(AgentType.SECURITY.name()))
				.allMatch(execution -> execution.getStatus().equals("COMPLETED")));

		// The run still completes despite the failure.
		ArgumentCaptor<Analysis> savedAnalysis = ArgumentCaptor.forClass(Analysis.class);
		verify(analysisRepository, times(2)).save(savedAnalysis.capture());
		assertEquals(AnalysisStatus.COMPLETED, savedAnalysis.getValue().getStatus());
	}

	@Test
	void proceedsWithLimitedMetadataWhenTheScannerFails() {
		RepositoryScannerAgent failingScanner = path -> {
			throw new RuntimeException("scan failed");
		};
		UnifiedAnalysisServiceImpl service = new UnifiedAnalysisServiceImpl(
				cloneService,
				failingScanner,
				path -> List.of(),
				path -> List.of(),
				path -> testingResult(List.of()),
				path -> List.of(),
				repositoryEntityRepository,
				analysisRepository,
				findingRepository);

		UnifiedAnalysisResponse response = service.analyze("https://github.com/owner/repo");

		// Repository name falls back to the URL segment when scan metadata is absent.
		assertEquals("repo", response.getRepositoryName());
		assertEquals("COMPLETED", response.getStatus());

		ArgumentCaptor<RepositoryEntity> savedRepository =
				ArgumentCaptor.forClass(RepositoryEntity.class);
		verify(repositoryEntityRepository).save(savedRepository.capture());
		assertEquals("JAVA", savedRepository.getValue().getLanguage());
	}

	private UnifiedAnalysisServiceImpl service(
			ComplexityAnalysisAgent complexityAgent,
			DuplicateCodeAnalysisAgent duplicateAgent,
			TestingAnalysisAgent testingAgent,
			SecurityAnalysisAgent securityAgent) {
		return new UnifiedAnalysisServiceImpl(
				cloneService,
				scannerAgent,
				complexityAgent,
				duplicateAgent,
				testingAgent,
				securityAgent,
				repositoryEntityRepository,
				analysisRepository,
				findingRepository);
	}

	private TestingAnalysisResult testingResult(List<Finding> findings) {
		TestingMetrics metrics =
				new TestingMetrics(List.of(), List.of(), false, false, 0, 0, 0, 0, "n/a");
		return new TestingAnalysisResult(findings, metrics);
	}

	private Finding finding(AgentType agentType, Severity severity) {
		Finding finding = new Finding();
		finding.setAgentType(agentType);
		finding.setSeverity(severity);
		finding.setTitle(agentType.name() + " issue");
		finding.setDescription("description");
		return finding;
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<List<Finding>> findingCaptor() {
		return ArgumentCaptor.forClass(List.class);
	}
}
