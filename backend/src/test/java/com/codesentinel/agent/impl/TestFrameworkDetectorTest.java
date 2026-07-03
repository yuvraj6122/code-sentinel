package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestFrameworkDetectorTest {

	private final TestFrameworkDetector detector = new TestFrameworkDetector();

	@Test
	void detectsJUnit5AndMockitoFromGradle(@TempDir Path repo) throws Exception {
		Files.writeString(repo.resolve("build.gradle"), """
				dependencies {
					testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
					testImplementation 'org.mockito:mockito-core:5.7.0'
				}
				""");

		FrameworkDetection detection = detector.detect(repo, Set.of());

		assertTrue(detection.getTestingFrameworks().contains("JUnit 5"));
		assertTrue(detection.getMockingFrameworks().contains("Mockito"));
		assertFalse(detection.getTestingFrameworks().contains("JUnit 4"));
	}

	@Test
	void detectsJUnit4FromGradle(@TempDir Path repo) throws Exception {
		Files.writeString(repo.resolve("build.gradle"), """
				dependencies {
					testImplementation 'junit:junit:4.13.2'
				}
				""");

		FrameworkDetection detection = detector.detect(repo, Set.of());

		assertTrue(detection.getTestingFrameworks().contains("JUnit 4"));
	}

	@Test
	void detectsJUnit4FromMaven(@TempDir Path repo) throws Exception {
		Files.writeString(repo.resolve("pom.xml"), """
				<project>
					<dependencies>
						<dependency>
							<groupId>junit</groupId>
							<artifactId>junit</artifactId>
							<version>4.13.2</version>
						</dependency>
					</dependencies>
				</project>
				""");

		FrameworkDetection detection = detector.detect(repo, Set.of());

		assertTrue(detection.getTestingFrameworks().contains("JUnit 4"));
	}

	@Test
	void detectsTestNgAndEasyMock(@TempDir Path repo) throws Exception {
		Files.writeString(repo.resolve("build.gradle"), """
				dependencies {
					testImplementation 'org.testng:testng:7.8.0'
					testImplementation 'org.easymock:easymock:5.2.0'
				}
				""");

		FrameworkDetection detection = detector.detect(repo, Set.of());

		assertTrue(detection.getTestingFrameworks().contains("TestNG"));
		assertTrue(detection.getMockingFrameworks().contains("EasyMock"));
	}

	@Test
	void fallsBackToTestImportsWhenBuildFileIsSilent(@TempDir Path repo) throws Exception {
		FrameworkDetection detection =
				detector.detect(repo, Set.of("org.junit.jupiter.api.Test", "io.mockk.MockKKt"));

		assertTrue(detection.getTestingFrameworks().contains("JUnit 5"));
		assertTrue(detection.getMockingFrameworks().contains("MockK"));
	}

	@Test
	void detectsNoFrameworksInBareRepository(@TempDir Path repo) {
		FrameworkDetection detection = detector.detect(repo, Set.of());

		assertFalse(detection.hasTestingFramework());
		assertFalse(detection.hasMockingFramework());
	}
}
