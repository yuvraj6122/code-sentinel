package com.codesentinel.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codesentinel.config.PlanningProperties;
import com.codesentinel.dto.RecommendationReportResponse;
import com.codesentinel.exception.AnalysisNotFoundException;
import com.codesentinel.exception.PlanningAgentException;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Analysis;
import com.codesentinel.model.AnalysisStatus;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Recommendation;
import com.codesentinel.model.RecommendationImpact;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.model.Severity;
import com.codesentinel.planning.OpenAiClient;
import com.codesentinel.planning.PromptBuilder;
import com.codesentinel.planning.RecommendationValidator;
import com.codesentinel.repository.AnalysisRepository;
import com.codesentinel.repository.RecommendationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class PlanningAgentServiceImplTest {

	private static final long ANALYSIS_ID = 7L;
	private static final String VALID_JSON =
			"""
			{
			  "overallAssessment": "Security needs attention.",
			  "recommendations": [
			    {"priority": 1, "title": "Fix SQLi", "description": "Parameterize queries",
			     "reason": "Injection sink", "impact": "HIGH", "affectedCategories": ["SECURITY"]},
			    {"priority": 2, "title": "Add tests", "description": "Cover payment module",
			     "reason": "Low coverage", "impact": "MEDIUM", "affectedCategories": ["TESTING"]}
			  ]
			}
			""";

	private final AnalysisRepository analysisRepository = mock(AnalysisRepository.class);
	private final RecommendationRepository recommendationRepository =
			mock(RecommendationRepository.class);
	private final OpenAiClient openAiClient = mock(OpenAiClient.class);

	private PlanningAgentServiceImpl service;

	@BeforeEach
	void setUp() {
		PlanningProperties properties = new PlanningProperties();
		service = new PlanningAgentServiceImpl(
				analysisRepository,
				recommendationRepository,
				new PromptBuilder(properties),
				openAiClient,
				new RecommendationValidator(),
				new ObjectMapper());

		when(analysisRepository.save(any(Analysis.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(recommendationRepository.saveAll(anyList()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void generatesValidatesAndPersistsRecommendations() {
		Analysis analysis = analysisWithFindings();
		when(analysisRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
		when(openAiClient.complete(anyString(), anyString())).thenReturn(VALID_JSON);

		RecommendationReportResponse response = service.generate(ANALYSIS_ID, false);

		assertEquals(ANALYSIS_ID, response.getAnalysisId());
		assertEquals("Security needs attention.", response.getOverallAssessment());
		assertEquals(2, response.getTotalRecommendations());
		assertEquals(1, response.getRecommendations().get(0).getPriority());
		assertEquals("HIGH", response.getRecommendations().get(0).getImpact());

		// The assessment is persisted on the analysis, and each recommendation is linked.
		assertEquals("Security needs attention.", analysis.getOverallAssessment());
		ArgumentCaptor<List<Recommendation>> saved = recommendationCaptor();
		verify(recommendationRepository).saveAll(saved.capture());
		assertEquals(2, saved.getValue().size());
		assertTrue(saved.getValue().stream().allMatch(r -> r.getAnalysis() == analysis));
		assertEquals(RecommendationImpact.HIGH, saved.getValue().get(0).getImpact());
		assertEquals(List.of(AgentType.SECURITY), saved.getValue().get(0).getAffectedCategories());
	}

	@Test
	void returnsStoredWithoutCallingOpenAiWhenAlreadyGeneratedAndNotForced() {
		Analysis analysis = analysisWithFindings();
		analysis.setOverallAssessment("Existing assessment.");
		when(analysisRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
		when(recommendationRepository.existsByAnalysisId(ANALYSIS_ID)).thenReturn(true);
		when(recommendationRepository.findByAnalysisIdOrderByPriorityAsc(ANALYSIS_ID))
				.thenReturn(List.of(storedRecommendation()));

		RecommendationReportResponse response = service.generate(ANALYSIS_ID, false);

		assertEquals(1, response.getTotalRecommendations());
		assertEquals("Existing assessment.", response.getOverallAssessment());
		verify(openAiClient, never()).complete(anyString(), anyString());
		verify(recommendationRepository, never()).saveAll(anyList());
	}

	@Test
	void regeneratesAndClearsPriorRecommendationsWhenForced() {
		Analysis analysis = analysisWithFindings();
		when(analysisRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
		when(recommendationRepository.existsByAnalysisId(ANALYSIS_ID)).thenReturn(true);
		when(recommendationRepository.findByAnalysisIdOrderByPriorityAsc(ANALYSIS_ID))
				.thenReturn(List.of(storedRecommendation()));
		when(openAiClient.complete(anyString(), anyString())).thenReturn(VALID_JSON);

		RecommendationReportResponse response = service.generate(ANALYSIS_ID, true);

		verify(recommendationRepository).deleteAll(anyList());
		verify(openAiClient).complete(anyString(), anyString());
		assertEquals(2, response.getTotalRecommendations());
	}

	@Test
	void rejectsMalformedJsonWithoutPersisting() {
		Analysis analysis = analysisWithFindings();
		when(analysisRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
		when(openAiClient.complete(anyString(), anyString())).thenReturn("this is not json");

		assertThrows(PlanningAgentException.class, () -> service.generate(ANALYSIS_ID, false));

		verify(recommendationRepository, never()).saveAll(anyList());
	}

	@Test
	void rejectsResponseMissingAssessmentWithoutPersisting() {
		Analysis analysis = analysisWithFindings();
		when(analysisRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
		when(openAiClient.complete(anyString(), anyString()))
				.thenReturn("{\"recommendations\": []}");

		assertThrows(PlanningAgentException.class, () -> service.generate(ANALYSIS_ID, false));

		verify(recommendationRepository, never()).saveAll(anyList());
	}

	@Test
	void persistsAssessmentWhenResponseHasNoRecommendations() {
		Analysis analysis = analysisWithFindings();
		when(analysisRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
		when(openAiClient.complete(anyString(), anyString()))
				.thenReturn("{\"overallAssessment\": \"All clear.\", \"recommendations\": []}");

		RecommendationReportResponse response = service.generate(ANALYSIS_ID, false);

		assertEquals(0, response.getTotalRecommendations());
		assertEquals("All clear.", response.getOverallAssessment());
		assertEquals("All clear.", analysis.getOverallAssessment());
	}

	@Test
	void throwsWhenAnalysisDoesNotExist() {
		when(analysisRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(AnalysisNotFoundException.class, () -> service.generate(ANALYSIS_ID, false));
		assertThrows(
				AnalysisNotFoundException.class, () -> service.getRecommendations(ANALYSIS_ID));
	}

	@Test
	void getRecommendationsReturnsStoredReport() {
		Analysis analysis = analysisWithFindings();
		analysis.setOverallAssessment("Stored assessment.");
		when(analysisRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(analysis));
		when(recommendationRepository.findByAnalysisIdOrderByPriorityAsc(ANALYSIS_ID))
				.thenReturn(List.of(storedRecommendation()));

		RecommendationReportResponse response = service.getRecommendations(ANALYSIS_ID);

		assertEquals("Stored assessment.", response.getOverallAssessment());
		assertEquals(1, response.getTotalRecommendations());
		verify(openAiClient, never()).complete(anyString(), anyString());
	}

	private Analysis analysisWithFindings() {
		RepositoryEntity repository = new RepositoryEntity();
		repository.setName("demo");
		repository.setLanguage("JAVA");
		repository.setBuildTool("GRADLE");

		Analysis analysis = new Analysis();
		analysis.setId(ANALYSIS_ID);
		analysis.setRepository(repository);
		analysis.setStatus(AnalysisStatus.COMPLETED);
		analysis.getFindings().add(finding(AgentType.SECURITY, Severity.CRITICAL));
		analysis.getFindings().add(finding(AgentType.TESTING, Severity.LOW));
		return analysis;
	}

	private Finding finding(AgentType agentType, Severity severity) {
		Finding finding = new Finding();
		finding.setAgentType(agentType);
		finding.setSeverity(severity);
		finding.setTitle(agentType.name() + " issue");
		finding.setDescription("description");
		return finding;
	}

	private Recommendation storedRecommendation() {
		Recommendation recommendation = new Recommendation();
		recommendation.setPriority(1);
		recommendation.setTitle("Stored");
		recommendation.setDescription("Stored description");
		recommendation.setReason("Stored reason");
		recommendation.setImpact(RecommendationImpact.MEDIUM);
		recommendation.setCreatedAt(LocalDateTime.now());
		return recommendation;
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<List<Recommendation>> recommendationCaptor() {
		return ArgumentCaptor.forClass(List.class);
	}
}
