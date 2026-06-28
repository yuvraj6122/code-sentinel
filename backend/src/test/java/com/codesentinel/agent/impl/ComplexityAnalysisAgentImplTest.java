package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesentinel.config.ComplexityProperties;
import com.codesentinel.config.ComplexityProperties.Thresholds;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ComplexityAnalysisAgentImplTest {

	@Test
	void detectsComplexityLongMethodAndLargeClass(@TempDir Path repo) throws Exception {
		Files.writeString(repo.resolve("Sample.java"), sampleSource());

		ComplexityAnalysisAgentImpl agent = new ComplexityAnalysisAgentImpl(
				lowThresholds(), new PmdReportParser(), new SourceLengthAnalyzer());

		List<Finding> findings = agent.analyze(repo);

		assertTrue(findings.size() > 0, "expected at least one finding");
		assertTrue(
				findings.stream().allMatch(f -> f.getAgentType() == AgentType.COMPLEXITY),
				"every finding should be a COMPLEXITY finding");
		assertTrue(
				findings.stream().allMatch(f -> "Sample.java".equals(f.getFilePath())),
				"file path should be relative to the repository root");

		assertTrue(
				findings.stream()
						.anyMatch(f ->
								f.getTitle().endsWith("Cyclomatic Complexity")
										&& f.getSeverity() == Severity.HIGH),
				"expected a HIGH cyclomatic complexity finding");
		assertTrue(
				findings.stream().anyMatch(f -> "Method Too Long".equals(f.getTitle())),
				"expected a method-too-long finding");
		assertTrue(
				findings.stream().anyMatch(f -> "Large Class".equals(f.getTitle())),
				"expected a large-class finding");
	}

	private ComplexityProperties lowThresholds() {
		ComplexityProperties props = new ComplexityProperties();
		props.setCyclomatic(new Thresholds(3, 4, 5));
		props.setCyclomaticClassReportLevel(9999);
		props.setMethodLength(new Thresholds(5, 10, 20));
		props.setClassLength(new Thresholds(10, 20, 30));
		return props;
	}

	private String sampleSource() {
		return """
				public class Sample {

					public int complex(int a) {
						int result = 0;
						if (a > 1) { result++; }
						if (a > 2) { result++; }
						if (a > 3) { result++; }
						if (a > 4) { result++; }
						if (a > 5) { result++; }
						if (a > 6) { result++; }
						return result;
					}

					public void longMethod() {
						int a = 0;
						a++;
						a++;
						a++;
						a++;
						a++;
						a++;
						a++;
					}
				}
				""";
	}
}
