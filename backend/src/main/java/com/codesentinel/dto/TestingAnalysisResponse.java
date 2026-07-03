package com.codesentinel.dto;

import com.codesentinel.agent.TestingMetrics;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API response for the Testing Analysis Agent. Shares the finding-summary shape
 * used by the Complexity and Duplicate Code responses (total + severity counts +
 * findings) and adds the testing-maturity summary (frameworks, integration
 * status, disabled count, and the 0-100 maturity score).
 */
@Getter
@AllArgsConstructor
public class TestingAnalysisResponse {

	private Long analysisId;
	private int totalFindings;
	private long highSeverity;
	private long mediumSeverity;
	private long lowSeverity;
	private List<String> testingFrameworks;
	private List<String> mockingFrameworks;
	private boolean hasTests;
	private boolean hasIntegrationTests;
	private int testClassCount;
	private int testMethodCount;
	private int disabledTestCount;
	private int maturityScore;
	private String maturitySummary;
	private List<FindingDto> findings;

	public static TestingAnalysisResponse from(
			Long analysisId, List<Finding> findings, TestingMetrics metrics) {
		return new TestingAnalysisResponse(
				analysisId,
				findings.size(),
				countBySeverity(findings, Severity.HIGH),
				countBySeverity(findings, Severity.MEDIUM),
				countBySeverity(findings, Severity.LOW),
				metrics.getTestingFrameworks(),
				metrics.getMockingFrameworks(),
				metrics.isHasTests(),
				metrics.isHasIntegrationTests(),
				metrics.getTestClassCount(),
				metrics.getTestMethodCount(),
				metrics.getDisabledTestCount(),
				metrics.getMaturityScore(),
				metrics.getMaturitySummary(),
				findings.stream().map(FindingDto::from).toList());
	}

	private static long countBySeverity(List<Finding> findings, Severity severity) {
		return findings.stream().filter(finding -> finding.getSeverity() == severity).count();
	}
}
