package com.codesentinel.agent.impl;

import com.codesentinel.agent.RepositoryScannerAgent;
import com.codesentinel.dto.RepositoryMetadataDto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RepositoryScannerAgentImpl implements RepositoryScannerAgent {

	private static final Pattern ANALYSIS_DIR_PATTERN = Pattern.compile("^analysis-\\d+$");
	private static final Pattern GITHUB_REPO_PATTERN =
			Pattern.compile("github\\.com/[^/]+/([\\w.-]+?)(?:\\.git)?/?$");

	@Override
	public RepositoryMetadataDto scan(Path repositoryPath) {
		log.info("Scanning repo at {}", repositoryPath);

		String repositoryName = detectRepositoryName(repositoryPath);
		String buildTool = detectBuildTool(repositoryPath);
		int javaFileCount = countJavaFiles(repositoryPath);
		String language = javaFileCount > 0 ? "JAVA" : "UNKNOWN";
		int testFileCount = countTestFiles(repositoryPath);

		log.info(
				"Found {} — {} / {}, {} java files, {} test files",
				repositoryName,
				language,
				buildTool,
				javaFileCount,
				testFileCount);

		if ("UNKNOWN".equals(buildTool)) {
			log.debug("No pom.xml or build.gradle found in {}", repositoryPath);
		}
		if ("UNKNOWN".equals(language)) {
			log.debug("Didn't find any .java files — language left as UNKNOWN");
		}

		return new RepositoryMetadataDto(
				repositoryName, language, buildTool, javaFileCount, testFileCount);
	}

	private String detectRepositoryName(Path repositoryPath) {
		String dirName = repositoryPath.getFileName().toString();
		if (!ANALYSIS_DIR_PATTERN.matcher(dirName).matches()) {
			return dirName;
		}

		String fromGitRemote = extractNameFromGitRemote(repositoryPath);
		if (fromGitRemote != null) {
			log.debug("Pulled repo name '{}' from git remote", fromGitRemote);
			return fromGitRemote;
		}

		try (Stream<Path> children = Files.list(repositoryPath)) {
			return children.filter(Files::isDirectory)
					.filter(path -> !path.getFileName().toString().equals(".git"))
					.map(path -> path.getFileName().toString())
					.findFirst()
					.orElse(dirName);
		} catch (IOException e) {
			return dirName;
		}
	}

	private String extractNameFromGitRemote(Path repositoryPath) {
		Path gitConfig = repositoryPath.resolve(".git/config");
		if (!Files.isRegularFile(gitConfig)) {
			return null;
		}

		try {
			for (String line : Files.readAllLines(gitConfig)) {
				String trimmed = line.trim();
				if (trimmed.startsWith("url =")) {
					String url = trimmed.substring("url =".length()).trim();
					Matcher matcher = GITHUB_REPO_PATTERN.matcher(url);
					if (matcher.find()) {
						return matcher.group(1);
					}
				}
			}
		} catch (IOException ignored) {
			// Fall through to other detection strategies.
		}
		return null;
	}

	private String detectBuildTool(Path repositoryPath) {
		if (Files.isRegularFile(repositoryPath.resolve("pom.xml"))) {
			return "MAVEN";
		}
		if (Files.isRegularFile(repositoryPath.resolve("build.gradle"))
				|| Files.isRegularFile(repositoryPath.resolve("build.gradle.kts"))) {
			return "GRADLE";
		}
		return "UNKNOWN";
	}

	private int countJavaFiles(Path repositoryPath) {
		try (Stream<Path> paths = walkRepository(repositoryPath)) {
			return (int) paths.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(".java"))
					.count();
		} catch (IOException e) {
			return 0;
		}
	}

	private int countTestFiles(Path repositoryPath) {
		Set<Path> testFiles = new HashSet<>();
		try (Stream<Path> paths = walkRepository(repositoryPath)) {
			paths.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(".java"))
					.forEach(path -> {
						if (isUnderSrcTest(path, repositoryPath)
								|| path.getFileName().toString().endsWith("Test.java")) {
							testFiles.add(path);
						}
					});
		} catch (IOException e) {
			return 0;
		}
		return testFiles.size();
	}

	private boolean isUnderSrcTest(Path file, Path repositoryRoot) {
		String relative = repositoryRoot.relativize(file).toString().replace('\\', '/');
		return relative.contains("src/test/");
	}

	private Stream<Path> walkRepository(Path repositoryPath) throws IOException {
		return Files.walk(repositoryPath).filter(path -> !isUnderGit(path, repositoryPath));
	}

	private boolean isUnderGit(Path path, Path repositoryRoot) {
		Path relative = repositoryRoot.relativize(path);
		if (relative.getNameCount() == 0) {
			return false;
		}
		return relative.getName(0).toString().equals(".git");
	}
}
