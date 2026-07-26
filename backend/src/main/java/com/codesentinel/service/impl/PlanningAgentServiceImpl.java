package com.codesentinel.service.impl;

import com.codesentinel.dto.RecommendationReportResponse;
import com.codesentinel.exception.AnalysisNotFoundException;
import com.codesentinel.exception.PlanningAgentException;
import com.codesentinel.model.Analysis;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Recommendation;
import com.codesentinel.planning.OpenAiClient;
import com.codesentinel.planning.PlanningResult;
import com.codesentinel.planning.PromptBuilder;
import com.codesentinel.planning.RecommendationValidator;
import com.codesentinel.planning.ValidatedPlan;
import com.codesentinel.repository.AnalysisRepository;
import com.codesentinel.repository.RecommendationRepository;
import com.codesentinel.service.PlanningAgentService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Default Planning Agent orchestration: load analysis + findings, build a prompt,
 * call OpenAI, validate the structured JSON, and persist the recommendations. The
 * OpenAI call and validation happen before any write, so an invalid response is
 * rejected without persisting partial data.
 */
@Slf4j
@Service
public class PlanningAgentServiceImpl implements PlanningAgentService {

	private final AnalysisRepository analysisRepository;
	private final RecommendationRepository recommendationRepository;
	private final PromptBuilder promptBuilder;
	private final OpenAiClient openAiClient;
	private final RecommendationValidator validator;
	private final ObjectMapper objectMapper;

	public PlanningAgentServiceImpl(
			AnalysisRepository analysisRepository,
			RecommendationRepository recommendationRepository,
			PromptBuilder promptBuilder,
			OpenAiClient openAiClient,
			RecommendationValidator validator,
			ObjectMapper objectMapper) {
		this.analysisRepository = analysisRepository;
		this.recommendationRepository = recommendationRepository;
		this.promptBuilder = promptBuilder;
		this.openAiClient = openAiClient;
		this.validator = validator;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public RecommendationReportResponse generate(Long analysisId, boolean force) {
		log.info("Recommendation generation started for analysis #{} (force={})", analysisId, force);
		Analysis analysis = loadAnalysis(analysisId);

		if (!force && recommendationRepository.existsByAnalysisId(analysisId)) {
			log.info("Analysis #{} already has recommendations — returning stored set", analysisId);
			return storedReport(analysis);
		}

		clearExisting(analysis);

		List<Finding> findings = analysis.getFindings();
		PromptBuilder.Prompt prompt = promptBuilder.build(analysis.getRepository(), findings);
		log.info(
				"Built planning prompt for analysis #{} — {} findings, prompt {} chars",
				analysisId,
				findings.size(),
				prompt.user().length());

		log.info("Calling OpenAI for analysis #{}", analysisId);
		String content = openAiClient.complete(prompt.system(), prompt.user());

		ValidatedPlan plan = validate(content);
		log.info(
				"Validated OpenAI response for analysis #{} — {} recommendations",
				analysisId,
				plan.recommendations().size());

		List<Recommendation> saved = persist(analysis, plan);
		log.info(
				"Recommendation generation complete for analysis #{} — persisted {} recommendations",
				analysisId,
				saved.size());
		return RecommendationReportResponse.from(analysisId, analysis.getOverallAssessment(), saved);
	}

	@Override
	@Transactional(readOnly = true)
	public RecommendationReportResponse getRecommendations(Long analysisId) {
		Analysis analysis = loadAnalysis(analysisId);
		return storedReport(analysis);
	}

	private ValidatedPlan validate(String content) {
		PlanningResult result;
		try {
			result = objectMapper.readValue(content, PlanningResult.class);
		} catch (JacksonException ex) {
			throw new PlanningAgentException(
					"OpenAI returned malformed JSON: " + ex.getOriginalMessage(), ex);
		}
		return validator.validate(result);
	}

	private List<Recommendation> persist(Analysis analysis, ValidatedPlan plan) {
		analysis.setOverallAssessment(plan.overallAssessment());
		analysisRepository.save(analysis);

		plan.recommendations().forEach(recommendation -> recommendation.setAnalysis(analysis));
		return recommendationRepository.saveAll(plan.recommendations());
	}

	/** Removes any prior recommendations (entity-level, so category rows are cleaned up too). */
	private void clearExisting(Analysis analysis) {
		List<Recommendation> existing =
				recommendationRepository.findByAnalysisIdOrderByPriorityAsc(analysis.getId());
		if (!existing.isEmpty()) {
			log.info("Removing {} prior recommendations for analysis #{}", existing.size(),
					analysis.getId());
			recommendationRepository.deleteAll(existing);
		}
	}

	private RecommendationReportResponse storedReport(Analysis analysis) {
		List<Recommendation> recommendations =
				recommendationRepository.findByAnalysisIdOrderByPriorityAsc(analysis.getId());
		return RecommendationReportResponse.from(
				analysis.getId(), analysis.getOverallAssessment(), recommendations);
	}

	private Analysis loadAnalysis(Long analysisId) {
		return analysisRepository
				.findById(analysisId)
				.orElseThrow(() -> new AnalysisNotFoundException("No analysis found with id " + analysisId));
	}
}
