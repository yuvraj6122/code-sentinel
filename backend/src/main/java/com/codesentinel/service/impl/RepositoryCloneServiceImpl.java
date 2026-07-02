package com.codesentinel.service.impl;

import com.codesentinel.exception.InvalidRepositoryUrlException;
import com.codesentinel.exception.RepositoryCloneException;
import com.codesentinel.service.RepositoryCloneService;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RepositoryCloneServiceImpl implements RepositoryCloneService {

	private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
			"^https://github\\.com/[\\w.-]+/[\\w.-]+/?(\\.git)?$");

	private static final Pattern ANALYSIS_DIR_PATTERN = Pattern.compile("analysis-(\\d+)");

	private final AtomicLong analysisCounter;
	private final Path basePath;

	public RepositoryCloneServiceImpl(
			@Value("${codesentinel.clone.base-path:/tmp/codesentinel}") String basePath) {
		this.basePath = Path.of(basePath);
		// Seed the counter past any directories left over from previous runs so the
		// next id is immediately free — otherwise, after a restart, the first clone
		// would have to skip every existing "analysis-N" one at a time.
		this.analysisCounter = new AtomicLong(findHighestExistingId());
		log.info(
				"Clone service ready — repos will land in {} (next id {})",
				this.basePath,
				this.analysisCounter.get() + 1);
	}

	private long findHighestExistingId() {
		if (!Files.isDirectory(basePath)) {
			return 0;
		}
		try (var entries = Files.list(basePath)) {
			return entries
					.map(path -> ANALYSIS_DIR_PATTERN.matcher(path.getFileName().toString()))
					.filter(Matcher::matches)
					.mapToLong(matcher -> Long.parseLong(matcher.group(1)))
					.max()
					.orElse(0);
		} catch (IOException e) {
			log.warn("Could not scan {} for existing clone directories — {}", basePath, e.getMessage());
			return 0;
		}
	}

	@Override
	public Path cloneRepository(String githubUrl) {
		String normalizedUrl = validateAndNormalizeUrl(githubUrl);
		Path targetDir = createUniqueDirectory();

		log.info("Cloning {} into {}", normalizedUrl, targetDir);
		try {
			Git.cloneRepository()
					.setURI(normalizedUrl)
					.setDirectory(targetDir.toFile())
					.call()
					.close();
			log.debug("Clone finished without errors");
			return targetDir;
		} catch (GitAPIException e) {
			log.warn("Clone failed for {} — {}", normalizedUrl, e.getMessage());
			cleanupDirectory(targetDir);
			throw new RepositoryCloneException("Failed to clone repository: " + e.getMessage(), e);
		}
	}

	private String validateAndNormalizeUrl(String githubUrl) {
		if (githubUrl == null || githubUrl.isBlank()) {
			log.debug("Rejected clone — URL was blank");
			throw new InvalidRepositoryUrlException("GitHub URL must not be blank");
		}

		String trimmed = githubUrl.trim();
		if (!GITHUB_URL_PATTERN.matcher(trimmed).matches()) {
			log.debug("Rejected clone — URL doesn't look like a GitHub repo: {}", trimmed);
			throw new InvalidRepositoryUrlException(
					"Invalid GitHub URL. Expected format: https://github.com/owner/repo");
		}

		return trimmed.endsWith(".git") ? trimmed : trimmed.replaceAll("/$", "") + ".git";
	}

	private Path createUniqueDirectory() {
		try {
			Files.createDirectories(basePath);
		} catch (IOException e) {
			throw new RepositoryCloneException("Failed to create clone base directory", e);
		}

		// The counter is seeded past existing directories at startup, so this
		// normally succeeds on the first attempt. The loop is a safety net that
		// atomically claims the next free name if a directory already exists
		// (e.g. a concurrent request), guaranteeing JGit a fresh, empty directory.
		while (true) {
			long id = analysisCounter.incrementAndGet();
			Path targetDir = basePath.resolve("analysis-" + id);
			try {
				Files.createDirectory(targetDir);
				return targetDir;
			} catch (FileAlreadyExistsException e) {
				log.debug("Clone directory {} already exists, trying next id", targetDir);
			} catch (IOException e) {
				throw new RepositoryCloneException("Failed to create clone directory", e);
			}
		}
	}

	private void cleanupDirectory(Path targetDir) {
		if (!Files.exists(targetDir)) {
			return;
		}

		log.debug("Removing partial clone at {}", targetDir);
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
