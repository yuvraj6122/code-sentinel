package com.codesentinel.report;

import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Recommendation;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Structured, format-agnostic representation of an engineering report, assembled
 * from an analysis's persisted data (repository, findings, recommendations). All
 * report-building logic (grouping, tallies, health) lives here so renderers only
 * format — this is what lets additional formats (e.g. PDF) be added later without
 * touching the assembly logic.
 */
public record ReportModel(
		String repositoryName,
		String repositoryUrl,
		String language,
		String buildTool,
		Long analysisId,
		String status,
		LocalDateTime analysisDate,
		String overallAssessment,
		String healthLabel,
		SeverityBreakdown severity,
		List<CategoryReport> categories,
		List<Recommendation> recommendations) {

	/** A single analysis category's findings and their severity tally. */
	public record CategoryReport(AgentType category, SeverityBreakdown severity, List<Finding> findings) {

		public int total() {
			return findings.size();
		}
	}

	/** Returns the category report for a given agent type (empty if absent). */
	public CategoryReport category(AgentType type) {
		return categories.stream()
				.filter(category -> category.category() == type)
				.findFirst()
				.orElse(new CategoryReport(type, new SeverityBreakdown(0, 0, 0, 0), List.of()));
	}

	public int totalFindings() {
		return (int) severity.total();
	}

	public boolean hasRecommendations() {
		return recommendations != null && !recommendations.isEmpty();
	}
}
