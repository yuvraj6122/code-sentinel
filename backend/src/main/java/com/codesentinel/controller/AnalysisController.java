package com.codesentinel.controller;

import com.codesentinel.dto.CloneRepositoryRequest;
import com.codesentinel.dto.UnifiedAnalysisResponse;
import com.codesentinel.service.UnifiedAnalysisService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entry point for the unified analysis pipeline: a single request clones the
 * repository, runs the scanner and every analysis agent, aggregates the results,
 * and returns a repository-level summary.
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

	private final UnifiedAnalysisService unifiedAnalysisService;

	public AnalysisController(UnifiedAnalysisService unifiedAnalysisService) {
		this.unifiedAnalysisService = unifiedAnalysisService;
	}

	@PostMapping("/analyze")
	public UnifiedAnalysisResponse analyze(@Valid @RequestBody CloneRepositoryRequest request) {
		log.info("Unified analysis request for {}", request.getGithubUrl());
		UnifiedAnalysisResponse response = unifiedAnalysisService.analyze(request.getGithubUrl());
		log.info(
				"Unified analysis #{} responded {} — {} findings ({} critical, {} high, {} medium, {} low)",
				response.getAnalysisId(),
				response.getStatus(),
				response.getTotalFindings(),
				response.getCriticalSeverity(),
				response.getHighSeverity(),
				response.getMediumSeverity(),
				response.getLowSeverity());
		return response;
	}
}
