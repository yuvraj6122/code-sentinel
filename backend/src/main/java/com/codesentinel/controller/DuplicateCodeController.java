package com.codesentinel.controller;

import com.codesentinel.dto.CloneRepositoryRequest;
import com.codesentinel.dto.DuplicateCodeAnalysisResponse;
import com.codesentinel.service.DuplicateCodeAnalysisService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/duplication")
public class DuplicateCodeController {

	private final DuplicateCodeAnalysisService duplicateCodeAnalysisService;

	public DuplicateCodeController(DuplicateCodeAnalysisService duplicateCodeAnalysisService) {
		this.duplicateCodeAnalysisService = duplicateCodeAnalysisService;
	}

	@PostMapping("/analyze")
	public DuplicateCodeAnalysisResponse analyze(@Valid @RequestBody CloneRepositoryRequest request) {
		log.info("Duplicate code analysis request for {}", request.getGithubUrl());
		DuplicateCodeAnalysisResponse response =
				duplicateCodeAnalysisService.analyze(request.getGithubUrl());
		log.info(
				"Duplicate code analysis complete — {} findings ({} high, {} medium, {} low)",
				response.getTotalFindings(),
				response.getHighSeverity(),
				response.getMediumSeverity(),
				response.getLowSeverity());
		return response;
	}
}
