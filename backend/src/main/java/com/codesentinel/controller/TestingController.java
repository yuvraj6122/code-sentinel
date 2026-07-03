package com.codesentinel.controller;

import com.codesentinel.dto.CloneRepositoryRequest;
import com.codesentinel.dto.TestingAnalysisResponse;
import com.codesentinel.service.TestingAnalysisService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/testing")
public class TestingController {

	private final TestingAnalysisService testingAnalysisService;

	public TestingController(TestingAnalysisService testingAnalysisService) {
		this.testingAnalysisService = testingAnalysisService;
	}

	@PostMapping("/analyze")
	public TestingAnalysisResponse analyze(@Valid @RequestBody CloneRepositoryRequest request) {
		log.info("Testing analysis request for {}", request.getGithubUrl());
		TestingAnalysisResponse response =
				testingAnalysisService.analyze(request.getGithubUrl());
		log.info(
				"Testing analysis complete — {} findings ({} high, {} medium, {} low), "
						+ "maturity {} ({})",
				response.getTotalFindings(),
				response.getHighSeverity(),
				response.getMediumSeverity(),
				response.getLowSeverity(),
				response.getMaturityScore(),
				response.getMaturitySummary());
		return response;
	}
}
