package com.codesentinel.agent;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Summary observations about a repository's test suite, produced alongside the
 * {@link com.codesentinel.model.Finding}s by the Testing Analysis Agent. These
 * describe the testing strategy's maturity (frameworks in use, integration
 * coverage, disabled tests, and an overall 0-100 maturity score) rather than raw
 * test counts, which the Repository Scanner already reports.
 */
@Getter
@AllArgsConstructor
public class TestingMetrics {

	private List<String> testingFrameworks;
	private List<String> mockingFrameworks;
	private boolean hasTests;
	private boolean hasIntegrationTests;
	private int testClassCount;
	private int testMethodCount;
	private int disabledTestCount;
	private int maturityScore;
	private String maturitySummary;
}
