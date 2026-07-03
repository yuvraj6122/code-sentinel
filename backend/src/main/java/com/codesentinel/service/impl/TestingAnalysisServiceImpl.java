package com.codesentinel.service.impl;

import com.codesentinel.agent.TestingAnalysisAgent;
import com.codesentinel.agent.TestingAnalysisResult;
import com.codesentinel.dto.TestingAnalysisResponse;
import com.codesentinel.model.Analysis;
import com.codesentinel.model.AnalysisStatus;
import com.codesentinel.model.Finding;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.repository.AnalysisRepository;
import com.codesentinel.repository.FindingRepository;
import com.codesentinel.repository.RepositoryEntityRepository;
import com.codesentinel.service.RepositoryCloneService;
import com.codesentinel.service.TestingAnalysisService;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TestingAnalysisServiceImpl implements TestingAnalysisService {

	private final RepositoryCloneService repositoryCloneService;
	private final TestingAnalysisAgent testingAnalysisAgent;
	private final RepositoryEntityRepository repositoryEntityRepository;
	private final AnalysisRepository analysisRepository;
	private final FindingRepository findingRepository;

	public TestingAnalysisServiceImpl(
			RepositoryCloneService repositoryCloneService,
			TestingAnalysisAgent testingAnalysisAgent,
			RepositoryEntityRepository repositoryEntityRepository,
			AnalysisRepository analysisRepository,
			FindingRepository findingRepository) {
		this.repositoryCloneService = repositoryCloneService;
		this.testingAnalysisAgent = testingAnalysisAgent;
		this.repositoryEntityRepository = repositoryEntityRepository;
		this.analysisRepository = analysisRepository;
		this.findingRepository = findingRepository;
	}

	@Override
	@Transactional
	public TestingAnalysisResponse analyze(String githubUrl) {
		log.info("Starting testing analysis pipeline for {}", githubUrl);

		Path clonedPath = repositoryCloneService.cloneRepository(githubUrl);
		RepositoryEntity repository = persistRepository(githubUrl, clonedPath);
		Analysis analysis = startAnalysis(repository);

		TestingAnalysisResult result = testingAnalysisAgent.analyze(clonedPath);
		List<Finding> findings = result.getFindings();
		findings.forEach(finding -> finding.setAnalysis(analysis));
		findingRepository.saveAll(findings);

		analysis.setStatus(AnalysisStatus.COMPLETED);
		analysis.setCompletedAt(LocalDateTime.now());
		analysisRepository.save(analysis);

		log.info(
				"Testing analysis #{} stored {} findings; maturity score {} ({})",
				analysis.getId(),
				findings.size(),
				result.getMetrics().getMaturityScore(),
				result.getMetrics().getMaturitySummary());
		return TestingAnalysisResponse.from(analysis.getId(), findings, result.getMetrics());
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
