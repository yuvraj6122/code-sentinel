package com.codesentinel.agent.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Detects which testing and mocking frameworks a repository declares. Two
 * complementary signals are combined: the build files ({@code pom.xml},
 * {@code build.gradle}, {@code build.gradle.kts}) and the imports observed in
 * the repository's test sources (a fallback for repos that inherit dependencies
 * from a parent/BOM and don't list them locally).
 */
@Slf4j
@Component
public class TestFrameworkDetector {

	private static final List<String> BUILD_FILES =
			List.of("pom.xml", "build.gradle", "build.gradle.kts");

	public FrameworkDetection detect(Path repositoryPath, Set<String> testImports) {
		String build = readBuildFiles(repositoryPath).toLowerCase();

		List<String> testing = new ArrayList<>();
		if (usesJUnit5(build, testImports)) {
			testing.add("JUnit 5");
		}
		if (usesJUnit4(build, testImports)) {
			testing.add("JUnit 4");
		}
		if (usesTestNg(build, testImports)) {
			testing.add("TestNG");
		}

		List<String> mocking = new ArrayList<>();
		if (build.contains("mockito") || importStartsWith(testImports, "org.mockito")) {
			mocking.add("Mockito");
		}
		if (build.contains("easymock") || importStartsWith(testImports, "org.easymock")) {
			mocking.add("EasyMock");
		}
		if (build.contains("mockk") || importStartsWith(testImports, "io.mockk")) {
			mocking.add("MockK");
		}

		log.debug("Detected testing frameworks {} and mocking frameworks {}", testing, mocking);
		return new FrameworkDetection(testing, mocking);
	}

	private boolean usesJUnit5(String build, Set<String> testImports) {
		return build.contains("junit-jupiter")
				|| build.contains("org.junit.jupiter")
				|| importStartsWith(testImports, "org.junit.jupiter");
	}

	private boolean usesJUnit4(String build, Set<String> testImports) {
		boolean inBuild = build.contains("junit:junit")
				|| build.contains("<artifactid>junit</artifactid>");
		boolean inImports = testImports.stream()
				.anyMatch(imp -> imp.startsWith("org.junit.")
						&& !imp.startsWith("org.junit.jupiter"));
		return inBuild || inImports;
	}

	private boolean usesTestNg(String build, Set<String> testImports) {
		return build.contains("testng") || importStartsWith(testImports, "org.testng");
	}

	private boolean importStartsWith(Set<String> testImports, String prefix) {
		return testImports.stream().anyMatch(imp -> imp.startsWith(prefix));
	}

	private String readBuildFiles(Path repositoryPath) {
		StringBuilder combined = new StringBuilder();
		for (String buildFile : BUILD_FILES) {
			Path path = repositoryPath.resolve(buildFile);
			if (!Files.isRegularFile(path)) {
				continue;
			}
			try {
				combined.append(Files.readString(path)).append('\n');
			} catch (IOException e) {
				log.debug("Could not read build file {}: {}", path, e.getMessage());
			}
		}
		return combined.toString();
	}
}
