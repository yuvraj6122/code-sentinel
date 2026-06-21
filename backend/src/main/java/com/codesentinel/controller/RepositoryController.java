package com.codesentinel.controller;

import com.codesentinel.dto.CloneRepositoryRequest;
import com.codesentinel.dto.CloneRepositoryResponse;
import com.codesentinel.dto.RepositoryMetadataDto;
import com.codesentinel.service.RepositoryCloneService;
import com.codesentinel.service.RepositoryScannerService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

	private final RepositoryCloneService repositoryCloneService;
	private final RepositoryScannerService repositoryScannerService;

	public RepositoryController(
			RepositoryCloneService repositoryCloneService,
			RepositoryScannerService repositoryScannerService) {
		this.repositoryCloneService = repositoryCloneService;
		this.repositoryScannerService = repositoryScannerService;
	}

	@PostMapping("/clone")
	public CloneRepositoryResponse clone(@Valid @RequestBody CloneRepositoryRequest request) {
		log.info("Clone request for {}", request.getGithubUrl());
		Path localPath = repositoryCloneService.cloneRepository(request.getGithubUrl());
		log.info("Clone done — files are at {}", localPath);
		return CloneRepositoryResponse.success(localPath.toString());
	}

	@PostMapping("/analyze")
	public RepositoryMetadataDto analyze(@Valid @RequestBody CloneRepositoryRequest request) {
		log.info("Analyze request for {}", request.getGithubUrl());
		RepositoryMetadataDto result = repositoryScannerService.analyze(request.getGithubUrl());
		log.info(
				"Analysis complete for {} — {} project, {} build, {} java files ({} tests)",
				result.getRepositoryName(),
				result.getLanguage(),
				result.getBuildTool(),
				result.getJavaFileCount(),
				result.getTestFileCount());
		return result;
	}
}