package com.codesentinel.agent.impl;

import com.codesentinel.agent.TestingAnalysisAgent;
import com.codesentinel.agent.TestingAnalysisResult;
import com.codesentinel.agent.TestingMetrics;
import com.codesentinel.config.TestingProperties;
import com.codesentinel.exception.TestingAnalysisException;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Evaluates the quality and maturity of a repository's test suite — "how mature
 * and effective is the testing strategy?" — rather than raw test counts (which
 * the Repository Scanner already reports).
 *
 * <p>Mirrors {@link ComplexityAnalysisAgentImpl} and
 * {@link DuplicateCodeAnalysisAgentImpl}: inspect the cloned repository, run a
 * set of graded checks, and translate the results into the shared
 * {@link Finding} model. The checks: framework detection, mocking framework
 * detection, integration-test detection, disabled tests, empty test classes,
 * tests without assertions, naming quality, and test organization. A 0-100
 * testing-maturity score is derived from the same checks.
 */
@Slf4j
@Component
public class TestingAnalysisAgentImpl implements TestingAnalysisAgent {

	private static final Set<String> POOR_TEST_NAMES = Set.of(
			"test", "abc", "check", "sample", "foo", "bar", "baz", "tmp", "temp",
			"todo", "xyz", "asdf", "dummy", "example", "testcase", "mytest");
	private static final double MIN_PACKAGE_OVERLAP = 0.5;

	private final TestingProperties properties;
	private final TestSourceInspector sourceInspector;
	private final TestFrameworkDetector frameworkDetector;

	public TestingAnalysisAgentImpl(
			TestingProperties properties,
			TestSourceInspector sourceInspector,
			TestFrameworkDetector frameworkDetector) {
		this.properties = properties;
		this.sourceInspector = sourceInspector;
		this.frameworkDetector = frameworkDetector;
	}

	@Override
	public TestingAnalysisResult analyze(Path repositoryPath) {
		log.info("Running testing analysis on {}", repositoryPath);

		try {
			TestSourceScan scan = sourceInspector.inspect(repositoryPath);
			FrameworkDetection frameworks =
					frameworkDetector.detect(repositoryPath, scan.getTestImports());

			Tally tally = new Tally();
			List<Finding> findings = new ArrayList<>();

			checkTestingFramework(frameworks, findings);
			int testMethodCount = countTestMethods(scan);
			boolean testsExist = testMethodCount > 0;

			checkMockingFramework(frameworks, testsExist, findings);
			boolean hasIntegration = checkIntegrationTests(scan, testsExist, findings);
			int disabledCount = checkDisabledTests(scan, findings);
			checkEmptyTestClasses(scan, findings, tally);
			checkTestsWithoutAssertions(scan, findings, tally);
			checkTestNaming(scan, findings, tally);
			boolean poorOrganization = checkTestOrganization(scan, findings);

			int maturityScore = computeMaturityScore(
					frameworks, testsExist, hasIntegration, disabledCount, poorOrganization, tally);

			TestingMetrics metrics = new TestingMetrics(
					frameworks.getTestingFrameworks(),
					frameworks.getMockingFrameworks(),
					testsExist,
					hasIntegration,
					scan.getTestClasses().size(),
					testMethodCount,
					disabledCount,
					maturityScore,
					summarize(maturityScore, testsExist));

			log.info(
					"Testing analysis produced {} findings; maturity score {} ({})",
					findings.size(),
					maturityScore,
					metrics.getMaturitySummary());
			return new TestingAnalysisResult(findings, metrics);
		} catch (RuntimeException e) {
			throw new TestingAnalysisException(
					"Testing analysis failed: " + e.getMessage(), e);
		}
	}

	private void checkTestingFramework(FrameworkDetection frameworks, List<Finding> findings) {
		if (frameworks.hasTestingFramework()) {
			return;
		}
		findings.add(newFinding(
				Severity.HIGH,
				"No Testing Framework Detected",
				"No recognized Java testing framework was found in the repository.",
				null));
	}

	private void checkMockingFramework(
			FrameworkDetection frameworks, boolean testsExist, List<Finding> findings) {
		if (!testsExist || frameworks.hasMockingFramework()) {
			return;
		}
		findings.add(newFinding(
				Severity.LOW,
				"No Mocking Framework Detected",
				"Repository tests may be tightly coupled to implementations.",
				null));
	}

	private boolean checkIntegrationTests(
			TestSourceScan scan, boolean testsExist, List<Finding> findings) {
		boolean hasIntegration = scan.getTestClasses().stream()
				.anyMatch(TestClassInfo::isIntegrationTest);
		if (testsExist && !hasIntegration) {
			findings.add(newFinding(
					Severity.MEDIUM,
					"No Integration Tests Detected",
					"Only unit tests were found; no integration testing patterns "
							+ "(e.g. @SpringBootTest, @DataJpaTest, Testcontainers) were detected.",
					null));
		}
		return hasIntegration;
	}

	private int checkDisabledTests(TestSourceScan scan, List<Finding> findings) {
		int disabled = 0;
		for (TestClassInfo testClass : scan.getTestClasses()) {
			if (testClass.isDisabled()) {
				disabled += Math.max(1, testClass.getTestMethods().size());
				continue;
			}
			disabled += (int) testClass.getTestMethods().stream()
					.filter(TestMethodInfo::isDisabled)
					.count();
		}

		Severity severity = properties.getDisabledTests().classify(disabled);
		if (severity != null) {
			findings.add(newFinding(
					severity,
					"Disabled Tests Detected",
					"%d disabled test%s were found.".formatted(disabled, disabled == 1 ? "" : "s"),
					null));
		}
		return disabled;
	}

	private void checkEmptyTestClasses(
			TestSourceScan scan, List<Finding> findings, Tally tally) {
		for (TestClassInfo testClass : scan.getTestClasses()) {
			if (!testClass.isEmpty() || !isTestNamedClass(testClass.getClassName())) {
				continue;
			}
			tally.emptyClasses++;
			findings.add(newFinding(
					Severity.LOW,
					"Empty Test Class",
					"Test class '%s' contains no test methods.".formatted(testClass.getClassName()),
					testClass.getFilePath()));
		}
	}

	private void checkTestsWithoutAssertions(
			TestSourceScan scan, List<Finding> findings, Tally tally) {
		for (TestClassInfo testClass : scan.getTestClasses()) {
			for (TestMethodInfo method : testClass.getTestMethods()) {
				if (method.isDisabled() || method.isHasAssertions()) {
					continue;
				}
				tally.noAssertion++;
				findings.add(newFinding(
						Severity.MEDIUM,
						"Test Without Assertions",
						"Test method '%s' executes code but performs no verification."
								.formatted(method.getName()),
						testClass.getFilePath()));
			}
		}
	}

	private void checkTestNaming(TestSourceScan scan, List<Finding> findings, Tally tally) {
		for (TestClassInfo testClass : scan.getTestClasses()) {
			for (TestMethodInfo method : testClass.getTestMethods()) {
				if (method.isDisabled() || !isPoorName(method.getName())) {
					continue;
				}
				tally.poorNaming++;
				findings.add(newFinding(
						Severity.LOW,
						"Poor Test Naming Convention",
						("Test method '%s' has a non-descriptive name; prefer intent-revealing "
								+ "names such as 'shouldRejectInvalidInput'.")
								.formatted(method.getName()),
						testClass.getFilePath()));
			}
		}
	}

	private boolean checkTestOrganization(TestSourceScan scan, List<Finding> findings) {
		Set<String> mainPackages = scan.getMainPackages();
		Set<String> testPackages = scan.getTestPackages();
		if (testPackages.isEmpty() || mainPackages.isEmpty()) {
			return false;
		}

		long mirrored = testPackages.stream().filter(mainPackages::contains).count();
		double overlap = (double) mirrored / testPackages.size();
		if (overlap >= MIN_PACKAGE_OVERLAP) {
			return false;
		}

		findings.add(newFinding(
				Severity.LOW,
				"Poor Test Organization",
				"Test packages do not mirror the source package structure "
						+ "(%d of %d test packages match a source package)."
						.formatted(mirrored, testPackages.size()),
				null));
		return true;
	}

	private int computeMaturityScore(
			FrameworkDetection frameworks,
			boolean testsExist,
			boolean hasIntegration,
			int disabledCount,
			boolean poorOrganization,
			Tally tally) {
		if (!testsExist) {
			return 0;
		}

		TestingProperties.Maturity weights = properties.getMaturity();
		int score = 100;
		if (!frameworks.hasTestingFramework()) {
			score -= weights.getNoFrameworkPenalty();
		}
		if (!frameworks.hasMockingFramework()) {
			score -= weights.getNoMockingPenalty();
		}
		if (!hasIntegration) {
			score -= weights.getNoIntegrationPenalty();
		}
		score -= cap(disabledCount * weights.getDisabledTestPenaltyPerTest(),
				weights.getDisabledTestPenaltyCap());
		score -= cap(tally.noAssertion * weights.getNoAssertionPenaltyPerTest(),
				weights.getNoAssertionPenaltyCap());
		score -= cap(tally.emptyClasses * weights.getEmptyClassPenaltyPerClass(),
				weights.getEmptyClassPenaltyCap());
		score -= cap(tally.poorNaming * weights.getPoorNamingPenaltyPerMethod(),
				weights.getPoorNamingPenaltyCap());
		if (poorOrganization) {
			score -= weights.getPoorOrganizationPenalty();
		}
		return Math.max(0, Math.min(100, score));
	}

	private String summarize(int score, boolean testsExist) {
		if (!testsExist) {
			return "No Tests";
		}
		if (score >= 80) {
			return "Excellent";
		}
		if (score >= 60) {
			return "Good";
		}
		if (score >= 40) {
			return "Moderate";
		}
		if (score >= 20) {
			return "Weak";
		}
		return "Poor";
	}

	private int countTestMethods(TestSourceScan scan) {
		return scan.getTestClasses().stream()
				.mapToInt(testClass -> testClass.getTestMethods().size())
				.sum();
	}

	private boolean isTestNamedClass(String className) {
		return className.endsWith("Test")
				|| className.endsWith("Tests")
				|| className.endsWith("IT")
				|| className.endsWith("ITCase")
				|| className.contains("IntegrationTest");
	}

	private boolean isPoorName(String name) {
		String lower = name.toLowerCase();
		return POOR_TEST_NAMES.contains(lower)
				|| lower.matches("test\\d+")
				|| name.length() <= 3;
	}

	private int cap(int value, int max) {
		return Math.min(value, max);
	}

	private Finding newFinding(
			Severity severity, String title, String description, String filePath) {
		Finding finding = new Finding();
		finding.setAgentType(AgentType.TESTING);
		finding.setSeverity(severity);
		finding.setTitle(title);
		finding.setDescription(description);
		finding.setFilePath(filePath);
		return finding;
	}

	/** Running counts used only to derive the maturity score. */
	private static final class Tally {
		private int noAssertion;
		private int emptyClasses;
		private int poorNaming;
	}
}
