package com.codesentinel.service;

import com.codesentinel.dto.RecommendationReportResponse;

/**
 * Coordinates the AI Planning Agent: reads a completed analysis's persisted
 * findings, generates prioritized recommendations via OpenAI, validates and
 * persists them, and exposes stored recommendations. It never analyzes source
 * code and runs independently of the analysis pipeline.
 */
public interface PlanningAgentService {

	/**
	 * Generates recommendations for an analysis. When recommendations already
	 * exist and {@code force} is false, the stored set is returned without calling
	 * OpenAI; {@code force} regenerates from scratch.
	 */
	RecommendationReportResponse generate(Long analysisId, boolean force);

	/** Returns the stored recommendations (and assessment) for an analysis. */
	RecommendationReportResponse getRecommendations(Long analysisId);
}
