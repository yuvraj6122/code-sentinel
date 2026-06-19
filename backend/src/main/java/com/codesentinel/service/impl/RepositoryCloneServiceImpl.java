package com.codesentinel.service.impl;

import com.codesentinel.exception.InvalidRepositoryUrlException;
import com.codesentinel.exception.RepositoryCloneException;
import com.codesentinel.service.RepositoryCloneService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RepositoryCloneServiceImpl implements RepositoryCloneService {

	private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
			"^https://github\\.com/[\\w.-]+/[\\w.-]+/?(\\.git)?$");

	private final AtomicLong analysisCounter = new AtomicLong();
	private final Path basePath;

	public RepositoryCloneServiceImpl(
			@Value("${codesentinel.clone.base-path:/tmp/codesentinel}") String basePath) {
		this.basePath = Path.of(basePath);
	}

	@Override
	public Path cloneRepository(String githubUrl) {
		String normalizedUrl = validateAndNormalizeUrl(githubUrl);
		Path targetDir = createUniqueDirectory();

		try {
			Git.cloneRepository()
					.setURI(normalizedUrl)
					.setDirectory(targetDir.toFile())
					.call()
					.close();
			return targetDir;
		} catch (GitAPIException e) {
			cleanupDirectory(targetDir);
			throw new RepositoryCloneException("Failed to clone repository: " + e.getMessage(), e);
		}
	}

	private String validateAndNormalizeUrl(String githubUrl) {
		if (githubUrl == null || githubUrl.isBlank()) {
			throw new InvalidRepositoryUrlException("GitHub URL must not be blank");
		}

		String trimmed = githubUrl.trim();
		if (!GITHUB_URL_PATTERN.matcher(trimmed).matches()) {
			throw new InvalidRepositoryUrlException(
					"Invalid GitHub URL. Expected format: https://github.com/owner/repo");
		}

		return trimmed.endsWith(".git") ? trimmed : trimmed.replaceAll("/$", "") + ".git";
	}

	private Path createUniqueDirectory() {
		try {
			Files.createDirectories(basePath);
			long id = analysisCounter.incrementAndGet();
			Path targetDir = basePath.resolve("analysis-" + id);
			Files.createDirectories(targetDir);
			return targetDir;
		} catch (IOException e) {
			throw new RepositoryCloneException("Failed to create clone directory", e);
		}
	}

	private void cleanupDirectory(Path targetDir) {
		if (!Files.exists(targetDir)) {
			return;
		}

		try (var paths = Files.walk(targetDir)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
					// Best-effort cleanup after a failed clone.
				}
			});
		} catch (IOException ignored) {
			// Best-effort cleanup after a failed clone.
		}
	}
}
