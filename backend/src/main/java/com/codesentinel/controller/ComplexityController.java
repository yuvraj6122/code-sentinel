package com.codesentinel.controller;

import com.codesentinel.dto.CloneRepositoryRequest;
import com.codesentinel.dto.ComplexityAnalysisResponse;
import com.codesentinel.service.ComplexityAnalysisService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/complexity")
public class ComplexityController {

	private final ComplexityAnalysisService complexityAnalysisService;

	public ComplexityController(ComplexityAnalysisService complexityAnalysisService) {
		this.complexityAnalysisService = complexityAnalysisService;
	}

	@PostMapping("/analyze")
	public ComplexityAnalysisResponse analyze(@Valid @RequestBody CloneRepositoryRequest request) {
		log.info("Complexity analysis request for {}", request.getGithubUrl());
		ComplexityAnalysisResponse response =
				complexityAnalysisService.analyze(request.getGithubUrl());
		log.info(
				"Complexity analysis complete — {} findings ({} high, {} medium, {} low)",
				response.getTotalFindings(),
				response.getHighSeverity(),
				response.getMediumSeverity(),
				response.getLowSeverity());
		return response;
	}
}
