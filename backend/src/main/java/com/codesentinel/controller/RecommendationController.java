package com.codesentinel.controller;

import com.codesentinel.dto.RecommendationReportResponse;
import com.codesentinel.service.PlanningAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Planning Agent's recommendations for a completed analysis.
 * Generation is analysis-scoped and idempotent: repeated POSTs return the stored
 * set unless {@code force=true} is supplied.
 */
@Slf4j
@RestController
public class RecommendationController {

	private final PlanningAgentService planningAgentService;

	public RecommendationController(PlanningAgentService planningAgentService) {
		this.planningAgentService = planningAgentService;
	}

	@PostMapping("/api/analyses/{analysisId}/recommendations")
	public RecommendationReportResponse generate(
			@PathVariable Long analysisId,
			@RequestParam(name = "force", defaultValue = "false") boolean force) {
		log.info("Recommendation request for analysis #{} (force={})", analysisId, force);
		RecommendationReportResponse response = planningAgentService.generate(analysisId, force);
		log.info(
				"Recommendation response for analysis #{} — {} recommendations",
				analysisId,
				response.getTotalRecommendations());
		return response;
	}

	@GetMapping("/api/analyses/{analysisId}/recommendations")
	public RecommendationReportResponse get(@PathVariable Long analysisId) {
		log.info("Fetching stored recommendations for analysis #{}", analysisId);
		return planningAgentService.getRecommendations(analysisId);
	}
}
