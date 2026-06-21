package com.codesentinel.agent;

import com.codesentinel.dto.RepositoryMetadataDto;
import java.nio.file.Path;

public interface RepositoryScannerAgent {

	RepositoryMetadataDto scan(Path repositoryPath);
}
