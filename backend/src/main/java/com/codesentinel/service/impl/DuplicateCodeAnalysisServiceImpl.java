package com.codesentinel.service.impl;

import com.codesentinel.agent.DuplicateCodeAnalysisAgent;
import com.codesentinel.dto.DuplicateCodeAnalysisResponse;
import com.codesentinel.model.Analysis;
import com.codesentinel.model.AnalysisStatus;
import com.codesentinel.model.Finding;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.repository.AnalysisRepository;
import com.codesentinel.repository.FindingRepository;
import com.codesentinel.repository.RepositoryEntityRepository;
import com.codesentinel.service.DuplicateCodeAnalysisService;
import com.codesentinel.service.RepositoryCloneService;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DuplicateCodeAnalysisServiceImpl implements DuplicateCodeAnalysisService {

	private final RepositoryCloneService repositoryCloneService;
	private final DuplicateCodeAnalysisAgent duplicateCodeAnalysisAgent;
	private final RepositoryEntityRepository repositoryEntityRepository;
	private final AnalysisRepository analysisRepository;
	private final FindingRepository findingRepository;

	public DuplicateCodeAnalysisServiceImpl(
			RepositoryCloneService repositoryCloneService,
			DuplicateCodeAnalysisAgent duplicateCodeAnalysisAgent,
			RepositoryEntityRepository repositoryEntityRepository,
			AnalysisRepository analysisRepository,
			FindingRepository findingRepository) {
		this.repositoryCloneService = repositoryCloneService;
		this.duplicateCodeAnalysisAgent = duplicateCodeAnalysisAgent;
		this.repositoryEntityRepository = repositoryEntityRepository;
		this.analysisRepository = analysisRepository;
		this.findingRepository = findingRepository;
	}

	@Override
	@Transactional
	public DuplicateCodeAnalysisResponse analyze(String githubUrl) {
		log.info("Starting duplicate code analysis pipeline for {}", githubUrl);

		Path clonedPath = repositoryCloneService.cloneRepository(githubUrl);
		RepositoryEntity repository = persistRepository(githubUrl, clonedPath);
		Analysis analysis = startAnalysis(repository);

		List<Finding> findings = duplicateCodeAnalysisAgent.analyze(clonedPath);
		findings.forEach(finding -> finding.setAnalysis(analysis));
		findingRepository.saveAll(findings);

		analysis.setStatus(AnalysisStatus.COMPLETED);
		analysis.setCompletedAt(LocalDateTime.now());
		analysisRepository.save(analysis);

		log.info("Duplicate code analysis #{} stored {} findings", analysis.getId(), findings.size());
		return DuplicateCodeAnalysisResponse.from(analysis.getId(), findings);
	}

	private RepositoryEntity persistRepository(String githubUrl, Path clonedPath) {
		String normalizedUrl = githubUrl.trim().replaceAll("/$", "").replaceAll("\\.git$", "");
		RepositoryEntity entity = new RepositoryEntity();
		entity.setGithubUrl(normalizedUrl);
		entity.setName(repositoryNameFromUrl(normalizedUrl));
		entity.setFolderName(clonedPath.getFileName().toString());
		entity.setLanguage("JAVA");
		entity.setCreatedAt(LocalDateTime.now());
		return repositoryEntityRepository.save(entity);
	}

	private String repositoryNameFromUrl(String normalizedUrl) {
		int lastSlash = normalizedUrl.lastIndexOf('/');
		return lastSlash >= 0 && lastSlash < normalizedUrl.length() - 1
				? normalizedUrl.substring(lastSlash + 1)
				: normalizedUrl;
	}

	private Analysis startAnalysis(RepositoryEntity repository) {
		Analysis analysis = new Analysis();
		analysis.setRepository(repository);
		analysis.setStatus(AnalysisStatus.RUNNING);
		analysis.setStartedAt(LocalDateTime.now());
		return analysisRepository.save(analysis);
	}
}
