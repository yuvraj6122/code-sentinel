package com.codesentinel.service.impl;

import com.codesentinel.agent.RepositoryScannerAgent;
import com.codesentinel.dto.RepositoryMetadataDto;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.repository.RepositoryEntityRepository;
import com.codesentinel.service.RepositoryCloneService;
import com.codesentinel.service.RepositoryScannerService;
import java.nio.file.Path;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RepositoryScannerServiceImpl implements RepositoryScannerService {

	private final RepositoryCloneService repositoryCloneService;
	private final RepositoryScannerAgent repositoryScannerAgent;
	private final RepositoryEntityRepository repositoryEntityRepository;

	public RepositoryScannerServiceImpl(
			RepositoryCloneService repositoryCloneService,
			RepositoryScannerAgent repositoryScannerAgent,
			RepositoryEntityRepository repositoryEntityRepository) {
		this.repositoryCloneService = repositoryCloneService;
		this.repositoryScannerAgent = repositoryScannerAgent;
		this.repositoryEntityRepository = repositoryEntityRepository;
	}

	@Override
	@Transactional
	public RepositoryMetadataDto analyze(String githubUrl) {
		log.info("Starting analysis pipeline for {}", githubUrl);

		Path clonedPath = repositoryCloneService.cloneRepository(githubUrl);
		log.debug("Clone step done, handing off to scanner at {}", clonedPath);

		RepositoryMetadataDto metadata = repositoryScannerAgent.scan(clonedPath);
		log.debug("Scanner finished for {}", metadata.getRepositoryName());

		persistRepository(githubUrl, clonedPath, metadata);
		return metadata;
	}

	private void persistRepository(
			String githubUrl, Path clonedPath, RepositoryMetadataDto metadata) {
		RepositoryEntity entity = new RepositoryEntity();
		entity.setGithubUrl(githubUrl.trim().replaceAll("/$", "").replaceAll("\\.git$", ""));
		entity.setName(metadata.getRepositoryName());
		entity.setFolderName(clonedPath.getFileName().toString());
		entity.setLanguage(metadata.getLanguage());
		entity.setBuildTool(metadata.getBuildTool());
		entity.setCreatedAt(LocalDateTime.now());
		repositoryEntityRepository.save(entity);
		log.info("Saved {} to the database", metadata.getRepositoryName());
	}
}
