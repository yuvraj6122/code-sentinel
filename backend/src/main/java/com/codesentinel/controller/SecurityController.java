package com.codesentinel.controller;

import com.codesentinel.dto.CloneRepositoryRequest;
import com.codesentinel.dto.SecurityAnalysisResponse;
import com.codesentinel.service.SecurityAnalysisService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/security")
public class SecurityController {

	private final SecurityAnalysisService securityAnalysisService;

	public SecurityController(SecurityAnalysisService securityAnalysisService) {
		this.securityAnalysisService = securityAnalysisService;
	}

	@PostMapping("/analyze")
	public SecurityAnalysisResponse analyze(@Valid @RequestBody CloneRepositoryRequest request) {
		log.info("Security analysis request for {}", request.getGithubUrl());
		SecurityAnalysisResponse response =
				securityAnalysisService.analyze(request.getGithubUrl());
		log.info(
				"Security analysis complete — {} findings ({} high, {} medium, {} low)",
				response.getTotalFindings(),
				response.getHighSeverity(),
				response.getMediumSeverity(),
				response.getLowSeverity());
		return response;
	}
}
