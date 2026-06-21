package com.codesentinel.service;

import com.codesentinel.dto.RepositoryMetadataDto;

public interface RepositoryScannerService {

	RepositoryMetadataDto analyze(String githubUrl);
}
