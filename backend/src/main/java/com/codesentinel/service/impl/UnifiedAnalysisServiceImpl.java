package com.codesentinel.service.impl;

import com.codesentinel.agent.ComplexityAnalysisAgent;
import com.codesentinel.agent.DuplicateCodeAnalysisAgent;
import com.codesentinel.agent.RepositoryScannerAgent;
import com.codesentinel.agent.SecurityAnalysisAgent;
import com.codesentinel.agent.TestingAnalysisAgent;
import com.codesentinel.dto.RepositoryMetadataDto;
import com.codesentinel.dto.UnifiedAnalysisResponse;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Analysis;
import com.codesentinel.model.AnalysisStatus;
import com.codesentinel.model.Finding;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.repository.AnalysisRepository;
import com.codesentinel.repository.FindingRepository;
import com.codesentinel.repository.RepositoryEntityRepository;
import com.codesentinel.service.AgentExecutionResult;
import com.codesentinel.service.RepositoryCloneService;
import com.codesentinel.service.UnifiedAnalysisService;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unified orchestrator for the full analysis pipeline.
 *
 * <p>Rather than duplicating agent logic, it drives the existing agents through a
 * single workflow — clone once, scan, run every agent, aggregate findings under
 * one {@link Analysis}, persist, and summarize. Each agent runs independently:
 * if one fails, the failure is logged, the remaining agents still run, the
 * successful findings are still persisted, and a partial result is returned.
 */
@Slf4j
@Service
public class UnifiedAnalysisServiceImpl implements UnifiedAnalysisService {

	private final RepositoryCloneService repositoryCloneService;
	private final RepositoryScannerAgent repositoryScannerAgent;
	private final ComplexityAnalysisAgent complexityAnalysisAgent;
	private final DuplicateCodeAnalysisAgent duplicateCodeAnalysisAgent;
	private final TestingAnalysisAgent testingAnalysisAgent;
	private final SecurityAnalysisAgent securityAnalysisAgent;
	private final RepositoryEntityRepository repositoryEntityRepository;
	private final AnalysisRepository analysisRepository;
	private final FindingRepository findingRepository;

	public UnifiedAnalysisServiceImpl(
			RepositoryCloneService repositoryCloneService,
			RepositoryScannerAgent repositoryScannerAgent,
			ComplexityAnalysisAgent complexityAnalysisAgent,
			DuplicateCodeAnalysisAgent duplicateCodeAnalysisAgent,
			TestingAnalysisAgent testingAnalysisAgent,
			SecurityAnalysisAgent securityAnalysisAgent,
			RepositoryEntityRepository repositoryEntityRepository,
			AnalysisRepository analysisRepository,
			FindingRepository findingRepository) {
		this.repositoryCloneService = repositoryCloneService;
		this.repositoryScannerAgent = repositoryScannerAgent;
		this.complexityAnalysisAgent = complexityAnalysisAgent;
		this.duplicateCodeAnalysisAgent = duplicateCodeAnalysisAgent;
		this.testingAnalysisAgent = testingAnalysisAgent;
		this.securityAnalysisAgent = securityAnalysisAgent;
		this.repositoryEntityRepository = repositoryEntityRepository;
		this.analysisRepository = analysisRepository;
		this.findingRepository = findingRepository;
	}

	@Override
	@Transactional
	public UnifiedAnalysisResponse analyze(String githubUrl) {
		log.info("Starting unified repository analysis for {}", githubUrl);

		Path clonedPath = repositoryCloneService.cloneRepository(githubUrl);
		RepositoryMetadataDto metadata = scanRepository(clonedPath);
		RepositoryEntity repository = persistRepository(githubUrl, clonedPath, metadata);
		Analysis analysis = startAnalysis(repository);

		List<AgentExecutionResult> executions = runAgents(clonedPath);
		List<Finding> findings = persistFindings(analysis, executions);

		completeAnalysis(analysis);
		log.info(
				"Unified analysis #{} complete — {} findings across {} agents ({} failed)",
				analysis.getId(),
				findings.size(),
				executions.size(),
				executions.stream().filter(execution -> !execution.isSuccess()).count());
		return UnifiedAnalysisResponse.from(analysis.getId(), repository, executions);
	}

	private List<AgentExecutionResult> runAgents(Path repositoryPath) {
		return List.of(
				runAgent(
						"Complexity Agent",
						AgentType.COMPLEXITY,
						() -> complexityAnalysisAgent.analyze(repositoryPath)),
				runAgent(
						"Duplicate Code Agent",
						AgentType.DUPLICATE_CODE,
						() -> duplicateCodeAnalysisAgent.analyze(repositoryPath)),
				runAgent(
						"Testing Agent",
						AgentType.TESTING,
						() -> testingAnalysisAgent.analyze(repositoryPath).getFindings()),
				runAgent(
						"Security Agent",
						AgentType.SECURITY,
						() -> securityAnalysisAgent.analyze(repositoryPath)));
	}

	/**
	 * Runs a single agent, isolating its failure so one broken agent can't abort
	 * the whole analysis. Agents only read the filesystem (no DB writes), so
	 * catching here is safe within the surrounding transaction.
	 */
	private AgentExecutionResult runAgent(
			String label, AgentType agentType, Supplier<List<Finding>> task) {
		log.info("Running {}...", label);
		try {
			List<Finding> findings = task.get();
			log.info("{} completed — {} findings", label, findings.size());
			return AgentExecutionResult.completed(agentType, findings);
		} catch (Exception ex) {
			log.error(
					"{} failed — {}. Continuing with the remaining agents.", label, ex.getMessage(), ex);
			return AgentExecutionResult.failed(agentType, ex.getMessage());
		}
	}

	private List<Finding> persistFindings(
			Analysis analysis, List<AgentExecutionResult> executions) {
		List<Finding> findings = executions.stream()
				.flatMap(execution -> execution.getFindings().stream())
				.toList();
		findings.forEach(finding -> finding.setAnalysis(analysis));
		findingRepository.saveAll(findings);
		return findings;
	}

	private RepositoryMetadataDto scanRepository(Path clonedPath) {
		log.info("Running repository scan...");
		try {
			RepositoryMetadataDto metadata = repositoryScannerAgent.scan(clonedPath);
			log.info(
					"Repository scan complete — {} ({} build), {} java files ({} tests)",
					metadata.getRepositoryName(),
					metadata.getBuildTool(),
					metadata.getJavaFileCount(),
					metadata.getTestFileCount());
			return metadata;
		} catch (Exception ex) {
			log.error(
					"Repository scan failed — {}. Proceeding with limited repository metadata.",
					ex.getMessage(),
					ex);
			return null;
		}
	}

	private RepositoryEntity persistRepository(
			String githubUrl, Path clonedPath, RepositoryMetadataDto metadata) {
		String normalizedUrl = githubUrl.trim().replaceAll("/$", "").replaceAll("\\.git$", "");
		RepositoryEntity entity = new RepositoryEntity();
		entity.setGithubUrl(normalizedUrl);
		entity.setName(
				metadata != null ? metadata.getRepositoryName() : repositoryNameFromUrl(normalizedUrl));
		entity.setFolderName(clonedPath.getFileName().toString());
		entity.setLanguage(
				metadata != null && metadata.getLanguage() != null ? metadata.getLanguage() : "JAVA");
		if (metadata != null) {
			entity.setBuildTool(metadata.getBuildTool());
		}
		entity.setCreatedAt(LocalDateTime.now());
		return repositoryEntityRepository.save(entity);
	}

	private String repositoryNameFromUrl(String normalizedUrl) {
		int lastSlash = normalizedUrl.lastIndexOf('/');
		return lastSlash >= 0 && lastSlash < normalizedUrl.length() - 1
				? normalizedUrl.substring(lastSlash + 1)
				: normalizedUrl;
	}

	private Analysis startAnalysis(RepositoryEntity repository) {
		Analysis analysis = new Analysis();
		analysis.setRepository(repository);
		analysis.setStatus(AnalysisStatus.RUNNING);
		analysis.setStartedAt(LocalDateTime.now());
		return analysisRepository.save(analysis);
	}

	private void completeAnalysis(Analysis analysis) {
		analysis.setStatus(AnalysisStatus.COMPLETED);
		analysis.setCompletedAt(LocalDateTime.now());
		analysisRepository.save(analysis);
	}
}
