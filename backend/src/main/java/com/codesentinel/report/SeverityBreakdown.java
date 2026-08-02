package com.codesentinel.report;

import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.util.List;

/**
 * Severity tally for a set of findings. Format-agnostic so any renderer (Markdown
 * today, PDF later) can present the same numbers.
 */
public record SeverityBreakdown(long critical, long high, long medium, long low) {

	public static SeverityBreakdown of(List<Finding> findings) {
		return new SeverityBreakdown(
				count(findings, Severity.CRITICAL),
				count(findings, Severity.HIGH),
				count(findings, Severity.MEDIUM),
				count(findings, Severity.LOW));
	}

	public long total() {
		return critical + high + medium + low;
	}

	private static long count(List<Finding> findings, Severity severity) {
		return findings.stream().filter(finding -> finding.getSeverity() == severity).count();
	}
}
