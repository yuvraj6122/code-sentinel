package com.codesentinel.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Recommendation;
import com.codesentinel.model.RecommendationImpact;
import com.codesentinel.model.Severity;
import com.codesentinel.report.ReportModel.CategoryReport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownReportRendererTest {

	private final MarkdownReportRenderer renderer = new MarkdownReportRenderer();

	@Test
	void rendersAllNineSectionsInOrder() {
		String md = renderer.render(sampleModel());

		List<String> headings = List.of(
				"## 1. Report Header",
				"## 2. Executive Summary",
				"## 3. Repository Overview",
				"## 4. Complexity Analysis",
				"## 5. Security Analysis",
				"## 6. Testing Analysis",
				"## 7. Duplicate Code Analysis",
				"## 8. AI Recommendations",
				"## 9. Conclusion");

		int previous = -1;
		for (String heading : headings) {
			int index = md.indexOf(heading);
			assertThat(index).as("section '%s' present", heading).isGreaterThanOrEqualTo(0);
			assertThat(index).as("section '%s' in order", heading).isGreaterThan(previous);
			previous = index;
		}
	}

	@Test
	void includesTitleHeaderMetadataAndMarkdownTables() {
		String md = renderer.render(sampleModel());

		assertThat(md).startsWith("# CodeSentinel Engineering Report");
		assertThat(md).contains("| Repository | demo-repo |");
		assertThat(md).contains("| Repository URL | https://github.com/acme/demo-repo |");
		assertThat(md).contains("| Analysis ID | 42 |");
		assertThat(md).contains("| Status | COMPLETED |");
		// Severity table markdown structure.
		assertThat(md).contains("| Severity | Count |");
		assertThat(md).contains("| --- | --- |");
	}

	@Test
	void summarizesSeverityAndHighlightsCriticalFiles() {
		String md = renderer.render(sampleModel());

		// Overall severity counts (1 high security + 1 medium complexity).
		assertThat(md).contains("**Total findings:** 2");
		// Complexity section highlights the file location.
		assertThat(md).contains("`src/main/java/Big.java:120`");
		// Security section highlights the high-severity file.
		assertThat(md).contains("**Most critical files:**");
		assertThat(md).contains("`src/main/java/Login.java:10`");
	}

	@Test
	void includesRecommendationsWithReasoningAndImpact() {
		String md = renderer.render(sampleModel());

		assertThat(md).contains("### 1. Fix SQL injection");
		assertThat(md).contains("**Impact:** HIGH");
		assertThat(md).contains("**Affected categories:** SECURITY");
		assertThat(md).contains("**Reasoning:** Untrusted input reaches a query.");
		// Conclusion pulls the recommendation title into next steps.
		assertThat(md).contains("- Fix SQL injection");
	}

	@Test
	void handlesEmptyFindingsAndNoRecommendations() {
		ReportModel model = new ReportModel(
				"empty-repo",
				"https://github.com/acme/empty-repo",
				"Java",
				"Maven",
				7L,
				"COMPLETED",
				LocalDateTime.of(2026, 1, 1, 9, 0),
				null,
				"Excellent — no issues detected",
				new SeverityBreakdown(0, 0, 0, 0),
				List.of(
						new CategoryReport(AgentType.COMPLEXITY, new SeverityBreakdown(0, 0, 0, 0), List.of()),
						new CategoryReport(AgentType.SECURITY, new SeverityBreakdown(0, 0, 0, 0), List.of()),
						new CategoryReport(AgentType.TESTING, new SeverityBreakdown(0, 0, 0, 0), List.of()),
						new CategoryReport(
								AgentType.DUPLICATE_CODE, new SeverityBreakdown(0, 0, 0, 0), List.of())),
				List.of());

		String md = renderer.render(model);

		assertThat(md).contains("**Total findings:** 0");
		assertThat(md).contains("_No complexity issues were detected above the threshold._");
		assertThat(md).contains("_No security issues were detected._");
		assertThat(md).contains(
				"No AI recommendations have been generated for this analysis.");
		assertThat(md).contains("No AI assessment has been generated for this analysis yet.");
		// Every category is a strength when nothing was found.
		assertThat(md).contains("**Primary risks:** No high-severity or critical issues were found.");
	}

	private ReportModel sampleModel() {
		Finding complexity = finding(
				AgentType.COMPLEXITY, Severity.MEDIUM, "High cyclomatic complexity",
				"src/main/java/Big.java", 120);
		Finding security = finding(
				AgentType.SECURITY, Severity.HIGH, "SQL injection", "src/main/java/Login.java", 10);

		Recommendation recommendation = new Recommendation();
		recommendation.setPriority(1);
		recommendation.setTitle("Fix SQL injection");
		recommendation.setDescription("Use parameterized queries.");
		recommendation.setReason("Untrusted input reaches a query.");
		recommendation.setImpact(RecommendationImpact.HIGH);
		recommendation.setAffectedCategories(List.of(AgentType.SECURITY));
		recommendation.setCreatedAt(LocalDateTime.of(2026, 1, 2, 12, 0));

		return new ReportModel(
				"demo-repo",
				"https://github.com/acme/demo-repo",
				"Java",
				"Gradle",
				42L,
				"COMPLETED",
				LocalDateTime.of(2026, 1, 2, 11, 30),
				"The repository is healthy overall but has one high-risk security issue.",
				"At risk — high-severity issues present",
				new SeverityBreakdown(0, 1, 1, 0),
				List.of(
						new CategoryReport(
								AgentType.COMPLEXITY, new SeverityBreakdown(0, 0, 1, 0), List.of(complexity)),
						new CategoryReport(
								AgentType.SECURITY, new SeverityBreakdown(0, 1, 0, 0), List.of(security)),
						new CategoryReport(AgentType.TESTING, new SeverityBreakdown(0, 0, 0, 0), List.of()),
						new CategoryReport(
								AgentType.DUPLICATE_CODE, new SeverityBreakdown(0, 0, 0, 0), List.of())),
				List.of(recommendation));
	}

	private Finding finding(
			AgentType type, Severity severity, String title, String filePath, Integer line) {
		Finding finding = new Finding();
		finding.setAgentType(type);
		finding.setSeverity(severity);
		finding.setTitle(title);
		finding.setFilePath(filePath);
		finding.setLineNumber(line);
		return finding;
	}
}
