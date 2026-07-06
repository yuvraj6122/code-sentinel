package com.codesentinel.dto;

import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API response for the Security Analysis Agent. Shares the finding-summary shape
 * used by the Complexity, Duplicate Code, and Testing responses (total +
 * severity counts + findings).
 */
@Getter
@AllArgsConstructor
public class SecurityAnalysisResponse {

	private Long analysisId;
	private int totalFindings;
	private long highSeverity;
	private long mediumSeverity;
	private long lowSeverity;
	private List<FindingDto> findings;

	public static SecurityAnalysisResponse from(Long analysisId, List<Finding> findings) {
		return new SecurityAnalysisResponse(
				analysisId,
				findings.size(),
				countBySeverity(findings, Severity.HIGH),
				countBySeverity(findings, Severity.MEDIUM),
				countBySeverity(findings, Severity.LOW),
				findings.stream().map(FindingDto::from).toList());
	}

	private static long countBySeverity(List<Finding> findings, Severity severity) {
		return findings.stream().filter(finding -> finding.getSeverity() == severity).count();
	}
}
