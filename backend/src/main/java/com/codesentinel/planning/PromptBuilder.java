package com.codesentinel.planning;

import com.codesentinel.config.PlanningProperties;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.model.Severity;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Turns repository metadata and the persisted findings into a concise system +
 * user prompt for the model. It sends aggregate signal (severity + per-category
 * counts) plus a bounded, severity-ranked sample of findings per category rather
 * than dumping every finding, keeping the prompt small and focused.
 */
@Component
public class PromptBuilder {

	/** Categories included in the prompt, in a stable order. */
	private static final List<AgentType> CATEGORIES = List.of(
			AgentType.SECURITY, AgentType.COMPLEXITY, AgentType.TESTING, AgentType.DUPLICATE_CODE);

	private static final Comparator<Finding> BY_SEVERITY_DESC =
			Comparator.comparingInt((Finding finding) -> severityRank(finding.getSeverity())).reversed();

	private final PlanningProperties properties;

	public PromptBuilder(PlanningProperties properties) {
		this.properties = properties;
	}

	/** A ready-to-send pair of prompts. */
	public record Prompt(String system, String user) {
	}

	public Prompt build(RepositoryEntity repository, List<Finding> findings) {
		return new Prompt(systemPrompt(), userPrompt(repository, findings));
	}

	private String systemPrompt() {
		return """
				You are a senior software engineer and technical lead reviewing an \
				automated static-analysis report for a code repository. Analysis agents \
				have already collected the evidence; your job is to INTERPRET it, not to \
				re-analyze source code.

				Produce a prioritized, actionable engineering plan that:
				- assesses the repository's overall health,
				- prioritizes the most important issues first (security and correctness \
				outrank maintainability and style),
				- merges related findings into a single recommendation where sensible,
				- explains your reasoning and estimates the engineering impact of each item,
				- is grounded ONLY in the findings provided (do not invent issues).

				Return AT MOST %d recommendations. Respond with a single JSON object ONLY \
				(no markdown, no commentary) in exactly this shape:
				{
				  "overallAssessment": "string",
				  "recommendations": [
				    {
				      "priority": 1,
				      "title": "string",
				      "description": "string",
				      "reason": "string",
				      "impact": "HIGH | MEDIUM | LOW",
				      "affectedCategories": ["SECURITY | COMPLEXITY | TESTING | DUPLICATE_CODE"]
				    }
				  ]
				}
				"priority" is a 1-based integer where 1 is most important. "impact" must be \
				one of HIGH, MEDIUM, or LOW. "affectedCategories" must only use the listed \
				category names. If there are no meaningful issues, return an empty \
				recommendations array with an assessment explaining why.\
				"""
				.formatted(properties.getMaxRecommendations());
	}

	private String userPrompt(RepositoryEntity repository, List<Finding> findings) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("Repository: ").append(safe(repository.getName())).append('\n');
		prompt.append("Language: ").append(safe(repository.getLanguage())).append('\n');
		prompt.append("Build tool: ").append(safe(repository.getBuildTool())).append('\n');
		prompt.append("Total findings: ").append(findings.size()).append('\n');
		prompt.append(severitySummary(findings)).append("\n\n");

		Map<AgentType, List<Finding>> byCategory = findings.stream()
				.collect(Collectors.groupingBy(Finding::getAgentType));

		prompt.append("Findings by category:\n");
		for (AgentType category : CATEGORIES) {
			List<Finding> categoryFindings = byCategory.getOrDefault(category, List.of());
			prompt.append("\n## ")
					.append(category.name())
					.append(" (")
					.append(categoryFindings.size())
					.append(" findings)\n");
			appendKeyFindings(prompt, categoryFindings);
		}

		prompt.append(
				"\nBased on the evidence above, assess the repository's health and produce the "
						+ "prioritized recommendations as specified.");
		return prompt.toString();
	}

	private void appendKeyFindings(StringBuilder prompt, List<Finding> categoryFindings) {
		if (categoryFindings.isEmpty()) {
			prompt.append("- none\n");
			return;
		}
		categoryFindings.stream()
				.sorted(BY_SEVERITY_DESC)
				.limit(properties.getMaxFindingsPerCategory())
				.forEach(finding -> prompt.append("- [")
						.append(finding.getSeverity())
						.append("] ")
						.append(safe(finding.getTitle()))
						.append(location(finding))
						.append('\n'));
	}

	private String severitySummary(List<Finding> findings) {
		return "Severity counts — CRITICAL: %d, HIGH: %d, MEDIUM: %d, LOW: %d".formatted(
				countBySeverity(findings, Severity.CRITICAL),
				countBySeverity(findings, Severity.HIGH),
				countBySeverity(findings, Severity.MEDIUM),
				countBySeverity(findings, Severity.LOW));
	}

	private long countBySeverity(List<Finding> findings, Severity severity) {
		return findings.stream().filter(finding -> finding.getSeverity() == severity).count();
	}

	private String location(Finding finding) {
		if (finding.getFilePath() == null) {
			return "";
		}
		String location = " (" + finding.getFilePath();
		if (finding.getLineNumber() != null) {
			location += ":" + finding.getLineNumber();
		}
		return location + ")";
	}

	private static int severityRank(Severity severity) {
		return switch (severity) {
			case CRITICAL -> 4;
			case HIGH -> 3;
			case MEDIUM -> 2;
			case LOW -> 1;
		};
	}

	private String safe(String value) {
		return value == null ? "unknown" : value;
	}
}
