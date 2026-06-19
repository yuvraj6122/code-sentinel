package com.codesentinel.controller;

import com.codesentinel.dto.CloneRepositoryRequest;
import com.codesentinel.dto.CloneRepositoryResponse;
import com.codesentinel.service.RepositoryCloneService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

	private final RepositoryCloneService repositoryCloneService;

	public RepositoryController(RepositoryCloneService repositoryCloneService) {
		this.repositoryCloneService = repositoryCloneService;
	}

	@PostMapping("/clone")
	public CloneRepositoryResponse clone(@Valid @RequestBody CloneRepositoryRequest request) {
		Path localPath = repositoryCloneService.cloneRepository(request.getGithubUrl());
		return CloneRepositoryResponse.success(localPath.toString());
	}
}