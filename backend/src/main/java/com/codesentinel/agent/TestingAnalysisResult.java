package com.codesentinel.agent;

import com.codesentinel.model.Finding;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The output of the Testing Analysis Agent: the persisted-style {@link Finding}s
 * (reusing the shared model) together with the higher-level {@link TestingMetrics}
 * summary. The findings flow through the existing persistence/reporting pipeline;
 * the metrics enrich the API/dashboard with the testing-maturity picture.
 */
@Getter
@AllArgsConstructor
public class TestingAnalysisResult {

	private List<Finding> findings;
	private TestingMetrics metrics;
}
