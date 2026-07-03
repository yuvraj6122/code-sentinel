package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesentinel.agent.TestingAnalysisResult;
import com.codesentinel.config.TestingProperties;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestingAnalysisAgentImplTest {

	private final TestingAnalysisAgentImpl agent = new TestingAnalysisAgentImpl(
			new TestingProperties(), new TestSourceInspector(), new TestFrameworkDetector());

	@Test
	void reportsNoTestingFrameworkAndZeroMaturityForBareRepository(@TempDir Path repo)
			throws Exception {
		writeFile(repo, "src/main/java/com/example/App.java", """
				package com.example;

				public class App {}
				""");

		TestingAnalysisResult result = agent.analyze(repo);

		Finding framework = find(result.getFindings(), "No Testing Framework Detected");
		assertEquals(Severity.HIGH, framework.getSeverity());
		assertEquals(AgentType.TESTING, framework.getAgentType());
		assertEquals(0, result.getMetrics().getMaturityScore());
		assertEquals("No Tests", result.getMetrics().getMaturitySummary());
		assertFalse(result.getMetrics().isHasTests());
	}

	@Test
	void detectsQualityIssuesAcrossChecks(@TempDir Path repo) throws Exception {
		writeFile(repo, "build.gradle", """
				dependencies {
					testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
				}
				""");
		writeFile(repo, "src/main/java/com/example/Service.java", """
				package com.example;

				public class Service {}
				""");
		writeFile(repo, "src/test/java/com/example/GoodTest.java", """
				package com.example;

				import org.junit.jupiter.api.Test;
				import static org.junit.jupiter.api.Assertions.assertEquals;

				class GoodTest {
					@Test
					void shouldReturnValue() {
						assertEquals(1, 1);
					}
				}
				""");
		writeFile(repo, "src/test/java/com/example/BadTest.java", """
				package com.example;

				import org.junit.jupiter.api.Test;
				import org.junit.jupiter.api.Disabled;
				import static org.junit.jupiter.api.Assertions.assertEquals;

				class BadTest {
					@Test
					void test1() {
						int x = 1 + 1;
					}

					@Test
					@Disabled
					void ignoredCase() {
						assertEquals(1, 1);
					}
				}
				""");
		writeFile(repo, "src/test/java/com/example/EmptyTest.java", """
				package com.example;

				class EmptyTest {
				}
				""");

		List<Finding> findings = agent.analyze(repo).getFindings();

		assertTrue(has(findings, "No Testing Framework Detected").isEmpty());
		assertEquals(Severity.LOW, find(findings, "No Mocking Framework Detected").getSeverity());
		assertEquals(
				Severity.MEDIUM, find(findings, "No Integration Tests Detected").getSeverity());
		assertEquals(Severity.LOW, find(findings, "Disabled Tests Detected").getSeverity());
		assertEquals(Severity.LOW, find(findings, "Empty Test Class").getSeverity());
		assertEquals(Severity.MEDIUM, find(findings, "Test Without Assertions").getSeverity());
		assertEquals(Severity.LOW, find(findings, "Poor Test Naming Convention").getSeverity());
	}

	@Test
	void mapsManyDisabledTestsToHighSeverity(@TempDir Path repo) throws Exception {
		writeFile(repo, "build.gradle", """
				dependencies {
					testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
				}
				""");
		writeFile(repo, "src/test/java/com/example/DisabledTest.java", disabledClass(12));

		List<Finding> findings = agent.analyze(repo).getFindings();

		Finding disabled = find(findings, "Disabled Tests Detected");
		assertEquals(Severity.HIGH, disabled.getSeverity());
		assertTrue(disabled.getDescription().contains("12 disabled tests"));
	}

	@Test
	void awardsFullMaturityToAWellTestedRepository(@TempDir Path repo) throws Exception {
		writeFile(repo, "build.gradle", """
				dependencies {
					testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
					testImplementation 'org.mockito:mockito-core:5.7.0'
				}
				""");
		writeFile(repo, "src/main/java/com/example/Service.java", """
				package com.example;

				public class Service {}
				""");
		writeFile(repo, "src/test/java/com/example/ServiceTest.java", """
				package com.example;

				import org.junit.jupiter.api.Test;
				import org.springframework.boot.test.context.SpringBootTest;
				import static org.junit.jupiter.api.Assertions.assertEquals;

				@SpringBootTest
				class ServiceTest {
					@Test
					void shouldReturnComputedValue() {
						assertEquals(4, 2 + 2);
					}

					@Test
					void shouldRejectInvalidInput() {
						assertEquals(0, 0);
					}
				}
				""");

		TestingAnalysisResult result = agent.analyze(repo);

		assertTrue(result.getFindings().isEmpty(), "expected no findings for a healthy suite");
		assertEquals(100, result.getMetrics().getMaturityScore());
		assertEquals("Excellent", result.getMetrics().getMaturitySummary());
		assertTrue(result.getMetrics().isHasIntegrationTests());
		assertTrue(result.getMetrics().getTestingFrameworks().contains("JUnit 5"));
		assertTrue(result.getMetrics().getMockingFrameworks().contains("Mockito"));
	}

	private String disabledClass(int count) {
		StringBuilder body = new StringBuilder();
		body.append("package com.example;\n\n");
		body.append("import org.junit.jupiter.api.Test;\n");
		body.append("import org.junit.jupiter.api.Disabled;\n");
		body.append("import static org.junit.jupiter.api.Assertions.assertEquals;\n\n");
		body.append("class DisabledTest {\n");
		for (int i = 0; i < count; i++) {
			body.append("\t@Test\n\t@Disabled\n\tvoid shouldDoThing").append(i).append("() {\n");
			body.append("\t\tassertEquals(1, 1);\n\t}\n\n");
		}
		body.append("}\n");
		return body.toString();
	}

	private Finding find(List<Finding> findings, String title) {
		return has(findings, title).orElseThrow(
				() -> new AssertionError("expected a finding titled '" + title + "'"));
	}

	private Optional<Finding> has(List<Finding> findings, String title) {
		return findings.stream().filter(finding -> finding.getTitle().equals(title)).findFirst();
	}

	private void writeFile(Path repo, String relativePath, String content) throws Exception {
		Path target = repo.resolve(relativePath);
		Files.createDirectories(target.getParent());
		Files.writeString(target, content);
	}
}
