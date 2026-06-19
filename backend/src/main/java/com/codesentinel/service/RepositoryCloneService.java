package com.codesentinel.service;

import java.nio.file.Path;

public interface RepositoryCloneService {

	Path cloneRepository(String githubUrl);
}