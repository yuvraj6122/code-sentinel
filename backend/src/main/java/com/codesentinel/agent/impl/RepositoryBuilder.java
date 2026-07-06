package com.codesentinel.agent.impl;

import com.codesentinel.config.SecurityProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Best-effort compilation of a cloned repository so SpotBugs has bytecode to
 * analyze (SpotBugs works on {@code .class} files, not source). Detects the
 * build tool, runs a time-boxed compile, and returns the class output
 * directories that were produced.
 *
 * <p>Building arbitrary third-party repositories is inherently unreliable, so
 * every failure mode (no build tool, non-zero exit, timeout, missing binary) is
 * handled gracefully by returning whatever class directories already exist —
 * possibly none. The caller decides what to do with an empty result.
 */
@Slf4j
@Component
public class RepositoryBuilder {

	private static final boolean IS_WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	private final SecurityProperties properties;

	public RepositoryBuilder(SecurityProperties properties) {
		this.properties = properties;
	}

	/**
	 * @return the class output directories containing compiled bytecode, or an
	 *     empty list if the repository could not be built.
	 */
	public List<Path> build(Path repositoryPath) {
		if (properties.isBuildEnabled()) {
			runBuild(repositoryPath);
		} else {
			log.info("Repository build disabled; analyzing pre-existing bytecode only");
		}
		return locateClassDirectories(repositoryPath);
	}

	private void runBuild(Path repositoryPath) {
		List<String> command = buildCommand(repositoryPath);
		if (command.isEmpty()) {
			log.info("No Maven/Gradle build detected in {}; skipping build step", repositoryPath);
			return;
		}

		log.info("Building repository with: {}", String.join(" ", command));
		try {
			Process process = new ProcessBuilder(command)
					.directory(repositoryPath.toFile())
					.redirectErrorStream(true)
					.start();

			boolean finished =
					process.waitFor(properties.getBuildTimeoutSeconds(), TimeUnit.SECONDS);
			if (!finished) {
				log.warn("Build timed out after {}s; abandoning", properties.getBuildTimeoutSeconds());
				process.destroyForcibly();
				return;
			}
			if (process.exitValue() != 0) {
				log.warn("Build exited with code {}; analyzing whatever compiled",
						process.exitValue());
			} else {
				log.info("Build completed successfully");
			}
		} catch (IOException e) {
			log.warn("Could not start build ({}); analyzing pre-existing bytecode only",
					e.getMessage());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Build interrupted; analyzing pre-existing bytecode only");
		}
	}

	private List<String> buildCommand(Path repositoryPath) {
		if (Files.isRegularFile(repositoryPath.resolve("pom.xml"))) {
			String maven = executable(repositoryPath, "mvnw", "mvn");
			return List.of(maven, "-B", "-q", "-DskipTests", "compile");
		}
		if (Files.isRegularFile(repositoryPath.resolve("build.gradle"))
				|| Files.isRegularFile(repositoryPath.resolve("build.gradle.kts"))) {
			String gradle = executable(repositoryPath, "gradlew", "gradle");
			return List.of(gradle, "compileJava", "-x", "test", "-q", "--console=plain");
		}
		return List.of();
	}

	/**
	 * Prefer the project's wrapper if present (it pins the tool version), else
	 * fall back to a system-installed binary.
	 */
	private String executable(Path repositoryPath, String wrapper, String fallback) {
		String wrapperName = IS_WINDOWS ? wrapper + ".bat" : wrapper;
		Path wrapperPath = repositoryPath.resolve(wrapperName);
		if (Files.isRegularFile(wrapperPath)) {
			return wrapperPath.toAbsolutePath().toString();
		}
		return fallback;
	}

	private List<Path> locateClassDirectories(Path repositoryPath) {
		List<Path> classDirs = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(repositoryPath)) {
			paths.filter(Files::isDirectory)
					.filter(this::isClassOutputDir)
					.filter(this::containsClassFiles)
					.forEach(classDirs::add);
		} catch (IOException e) {
			log.warn("Could not scan {} for class directories: {}", repositoryPath, e.getMessage());
		}

		log.info("Found {} compiled class directory(ies) under {}", classDirs.size(), repositoryPath);
		return classDirs;
	}

	private boolean isClassOutputDir(Path dir) {
		String normalized = dir.toString().replace('\\', '/');
		return normalized.endsWith("/target/classes")
				|| normalized.endsWith("/build/classes/java/main");
	}

	private boolean containsClassFiles(Path dir) {
		try (Stream<Path> paths = Files.walk(dir)) {
			return paths.anyMatch(path -> path.getFileName().toString().endsWith(".class"));
		} catch (IOException e) {
			return false;
		}
	}
}
