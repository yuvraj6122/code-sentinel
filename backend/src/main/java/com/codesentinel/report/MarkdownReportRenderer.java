package com.codesentinel.report;

import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Recommendation;
import com.codesentinel.model.Severity;
import com.codesentinel.report.ReportModel.CategoryReport;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Renders a {@link ReportModel} as GitHub-flavoured Markdown. Purely
 * presentational — it formats what the model already computed and adds no
 * business logic, so a future PDF renderer can reuse the same model.
 */
@Component
public class MarkdownReportRenderer implements ReportRenderer {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	/** Cap on findings listed per category so the report stays readable. */
	private static final int MAX_FINDINGS_PER_CATEGORY = 15;

	/** Cap on "most critical files" / "most duplicated files" highlights. */
	private static final int MAX_HIGHLIGHTS = 5;

	private static final Comparator<Finding> BY_SEVERITY_DESC =
			Comparator.comparingInt((Finding finding) -> rank(finding.getSeverity())).reversed();

	@Override
	public String render(ReportModel model) {
		StringBuilder md = new StringBuilder();
		header(md, model);
		executiveSummary(md, model);
		repositoryOverview(md, model);
		complexity(md, model);
		security(md, model);
		testing(md, model);
		duplicate(md, model);
		recommendations(md, model);
		conclusion(md, model);
		return md.toString();
	}

	private void header(StringBuilder md, ReportModel model) {
		md.append("# CodeSentinel Engineering Report\n\n");
		md.append("## 1. Report Header\n\n");
		md.append("| Field | Value |\n| --- | --- |\n");
		md.append("| Repository | ").append(model.repositoryName()).append(" |\n");
		md.append("| Repository URL | ").append(model.repositoryUrl()).append(" |\n");
		md.append("| Analysis ID | ").append(model.analysisId()).append(" |\n");
		md.append("| Status | ").append(model.status()).append(" |\n");
		md.append("| Analysis date | ").append(formatDate(model)).append(" |\n\n");
	}

	private void executiveSummary(StringBuilder md, ReportModel model) {
		md.append("## 2. Executive Summary\n\n");
		md.append("**Repository health:** ").append(model.healthLabel()).append("\n\n");
		md.append("**Overall assessment:** ")
				.append(model.overallAssessment() != null && !model.overallAssessment().isBlank()
						? model.overallAssessment()
						: "No AI assessment has been generated for this analysis yet.")
				.append("\n\n");
		md.append("**Total findings:** ").append(model.totalFindings()).append("\n\n");
		md.append(severityTable(model.severity()));
	}

	private void repositoryOverview(StringBuilder md, ReportModel model) {
		md.append("## 3. Repository Overview\n\n");
		md.append("| Property | Value |\n| --- | --- |\n");
		md.append("| Name | ").append(model.repositoryName()).append(" |\n");
		md.append("| Language | ").append(orUnknown(model.language())).append(" |\n");
		md.append("| Build tool | ").append(orUnknown(model.buildTool())).append(" |\n");
		md.append("| Total findings | ").append(model.totalFindings()).append(" |\n");
		md.append("| Files with findings | ").append(filesWithFindings(model)).append(" |\n\n");
		md.append(
				"> Repository statistics are derived from the stored analysis. Source/test file "
						+ "counts and the testing maturity score are computed during analysis and are "
						+ "reflected in the findings below.\n\n");
	}

	private void complexity(StringBuilder md, ReportModel model) {
		md.append("## 4. Complexity Analysis\n\n");
		CategoryReport report = model.category(AgentType.COMPLEXITY);
		md.append("Cyclomatic complexity and method/class length findings (PMD).\n\n");
		md.append(severityTable(report.severity()));
		mostCriticalFiles(md, report.findings());
		findingsList(md, report.findings(), "No complexity issues were detected above the threshold.");
	}

	private void security(StringBuilder md, ReportModel model) {
		md.append("## 5. Security Analysis\n\n");
		CategoryReport report = model.category(AgentType.SECURITY);
		md.append("Vulnerabilities and security smells (SpotBugs + FindSecBugs).\n\n");
		md.append(severityTable(report.severity()));
		mostCriticalFiles(md, report.findings());
		findingsList(md, report.findings(), "No security issues were detected.");
	}

	private void testing(StringBuilder md, ReportModel model) {
		md.append("## 6. Testing Analysis\n\n");
		CategoryReport report = model.category(AgentType.TESTING);
		md.append(
				"Test-suite quality signals — framework detection, disabled/ignored tests, missing "
						+ "integration tests, assertion quality, and test naming/organization.\n\n");
		md.append(severityTable(report.severity()));
		findingsList(md, report.findings(), "No testing quality issues were detected.");
	}

	private void duplicate(StringBuilder md, ReportModel model) {
		md.append("## 7. Duplicate Code Analysis\n\n");
		CategoryReport report = model.category(AgentType.DUPLICATE_CODE);
		md.append("Copy/paste duplication blocks (PMD CPD).\n\n");
		md.append(severityTable(report.severity()));
		mostDuplicatedFiles(md, report.findings());
		findingsList(md, report.findings(), "No duplicate code was detected above the threshold.");
	}

	private void recommendations(StringBuilder md, ReportModel model) {
		md.append("## 8. AI Recommendations\n\n");
		if (!model.hasRecommendations()) {
			md.append(
					"_No AI recommendations have been generated for this analysis. Generate them via the "
							+ "Planning Agent to see prioritized next actions._\n\n");
			return;
		}
		md.append("**Overall assessment:** ")
				.append(model.overallAssessment() != null ? model.overallAssessment() : "N/A")
				.append("\n\n");
		for (Recommendation recommendation : model.recommendations()) {
			md.append("### ")
					.append(recommendation.getPriority())
					.append(". ")
					.append(recommendation.getTitle())
					.append("\n\n");
			md.append("- **Impact:** ").append(recommendation.getImpact()).append("\n");
			md.append("- **Affected categories:** ")
					.append(recommendation.getAffectedCategories().isEmpty()
							? "—"
							: recommendation.getAffectedCategories().stream()
									.map(AgentType::name)
									.reduce((a, b) -> a + ", " + b)
									.orElse("—"))
					.append("\n");
			md.append("- **Description:** ").append(recommendation.getDescription()).append("\n");
			md.append("- **Reasoning:** ").append(recommendation.getReason()).append("\n\n");
		}
	}

	private void conclusion(StringBuilder md, ReportModel model) {
		md.append("## 9. Conclusion\n\n");

		List<String> strengths = model.categories().stream()
				.filter(category -> category.total() == 0)
				.map(category -> label(category.category()))
				.toList();
		List<String> risks = model.categories().stream()
				.filter(category -> category.severity().critical() > 0 || category.severity().high() > 0)
				.map(category -> "%s (%d critical, %d high)".formatted(
						label(category.category()),
						category.severity().critical(),
						category.severity().high()))
				.toList();

		md.append("**Strengths:** ")
				.append(strengths.isEmpty() ? "—" : String.join("; ", strengths)
						+ " showed no findings.")
				.append("\n\n");
		md.append("**Primary risks:** ")
				.append(risks.isEmpty() ? "No high-severity or critical issues were found." : String.join("; ", risks))
				.append("\n\n");

		md.append("**Recommended next steps:**\n\n");
		if (model.hasRecommendations()) {
			model.recommendations().stream()
					.limit(3)
					.forEach(recommendation -> md.append("- ")
							.append(recommendation.getTitle())
							.append('\n'));
			md.append('\n');
		} else {
			md.append("- Generate AI recommendations to obtain a prioritized action plan.\n");
			md.append("- Address critical and high-severity findings first.\n\n");
		}
	}

	private String severityTable(SeverityBreakdown severity) {
		return """
				| Severity | Count |
				| --- | --- |
				| Critical | %d |
				| High | %d |
				| Medium | %d |
				| Low | %d |

				""".formatted(severity.critical(), severity.high(), severity.medium(), severity.low());
	}

	private void mostCriticalFiles(StringBuilder md, List<Finding> findings) {
		List<Finding> highlights = findings.stream()
				.filter(finding -> finding.getFilePath() != null)
				.sorted(BY_SEVERITY_DESC)
				.limit(MAX_HIGHLIGHTS)
				.toList();
		if (highlights.isEmpty()) {
			return;
		}
		md.append("**Most critical files:**\n\n");
		highlights.forEach(finding -> md.append("- `")
				.append(location(finding))
				.append("` — **")
				.append(finding.getSeverity())
				.append("** ")
				.append(finding.getTitle())
				.append('\n'));
		md.append('\n');
	}

	private void mostDuplicatedFiles(StringBuilder md, List<Finding> findings) {
		Map<String, Long> byFile = new LinkedHashMap<>();
		findings.stream()
				.filter(finding -> finding.getFilePath() != null)
				.forEach(finding -> byFile.merge(finding.getFilePath(), 1L, Long::sum));
		if (byFile.isEmpty()) {
			return;
		}
		md.append("**Most duplicated files:**\n\n");
		byFile.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
				.limit(MAX_HIGHLIGHTS)
				.forEach(entry -> md.append("- `")
						.append(entry.getKey())
						.append("` — ")
						.append(entry.getValue())
						.append(entry.getValue() == 1 ? " block\n" : " blocks\n"));
		md.append('\n');
	}

	private void findingsList(StringBuilder md, List<Finding> findings, String emptyMessage) {
		if (findings.isEmpty()) {
			md.append("_").append(emptyMessage).append("_\n\n");
			return;
		}
		md.append("**Findings").append(findings.size() > MAX_FINDINGS_PER_CATEGORY
				? " (top " + MAX_FINDINGS_PER_CATEGORY + " of " + findings.size() + " by severity):**\n\n"
				: ":**\n\n");
		findings.stream()
				.sorted(BY_SEVERITY_DESC)
				.limit(MAX_FINDINGS_PER_CATEGORY)
				.forEach(finding -> {
					md.append("- **").append(finding.getSeverity()).append("** — ")
							.append(finding.getTitle());
					if (finding.getFilePath() != null) {
						md.append(" (`").append(location(finding)).append("`)");
					}
					md.append('\n');
				});
		md.append('\n');
	}

	private String location(Finding finding) {
		if (finding.getLineNumber() != null) {
			return finding.getFilePath() + ":" + finding.getLineNumber();
		}
		return finding.getFilePath();
	}

	private String formatDate(ReportModel model) {
		return model.analysisDate() != null ? model.analysisDate().format(DATE) : "N/A";
	}

	private long filesWithFindings(ReportModel model) {
		return model.categories().stream()
				.flatMap(category -> category.findings().stream())
				.map(Finding::getFilePath)
				.filter(path -> path != null)
				.distinct()
				.count();
	}

	private String orUnknown(String value) {
		return value == null || value.isBlank() ? "Unknown" : value;
	}

	private String label(AgentType type) {
		return switch (type) {
			case COMPLEXITY -> "Complexity";
			case SECURITY -> "Security";
			case TESTING -> "Testing";
			case DUPLICATE_CODE -> "Duplicate code";
			case REPOSITORY_SCANNER -> "Repository scan";
			case PLANNING -> "Planning";
		};
	}

	private static int rank(Severity severity) {
		return switch (severity) {
			case CRITICAL -> 4;
			case HIGH -> 3;
			case MEDIUM -> 2;
			case LOW -> 1;
		};
	}
}
