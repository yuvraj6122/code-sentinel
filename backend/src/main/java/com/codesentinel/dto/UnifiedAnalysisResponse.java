package com.codesentinel.dto;

import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.model.Severity;
import com.codesentinel.service.AgentExecutionResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Repository-level result of a unified analysis run. Aggregates the findings
 * produced by every agent under a single {@code analysisId}, adds severity and
 * per-category summaries, and reports each agent's execution status so callers
 * can tell whether the run was complete or partial.
 *
 * <p>Reuses the finding-summary shape (total + severity counts + {@link FindingDto}
 * list) established by the per-agent responses.
 */
@Getter
@AllArgsConstructor
public class UnifiedAnalysisResponse {

	/** Analysis categories reported, in a stable display order. */
	private static final List<AgentType> CATEGORIES =
			List.of(AgentType.COMPLEXITY, AgentType.DUPLICATE_CODE, AgentType.TESTING,
					AgentType.SECURITY);

	private Long analysisId;
	private String repository;
	private String repositoryName;
	private String status;
	private int totalFindings;
	private long criticalSeverity;
	private long highSeverity;
	private long mediumSeverity;
	private long lowSeverity;
	private Map<String, Long> findingsByCategory;
	private List<AgentExecutionDto> agentExecutions;
	private List<FindingDto> findings;

	public static UnifiedAnalysisResponse from(
			Long analysisId, RepositoryEntity repository, List<AgentExecutionResult> executions) {
		List<Finding> findings = executions.stream()
				.flatMap(execution -> execution.getFindings().stream())
				.toList();

		return new UnifiedAnalysisResponse(
				analysisId,
				repository.getGithubUrl(),
				repository.getName(),
				resolveStatus(executions),
				findings.size(),
				countBySeverity(findings, Severity.CRITICAL),
				countBySeverity(findings, Severity.HIGH),
				countBySeverity(findings, Severity.MEDIUM),
				countBySeverity(findings, Severity.LOW),
				groupByCategory(findings),
				executions.stream().map(AgentExecutionDto::from).toList(),
				findings.stream().map(FindingDto::from).toList());
	}

	private static String resolveStatus(List<AgentExecutionResult> executions) {
		long failures = executions.stream().filter(execution -> !execution.isSuccess()).count();
		if (failures == 0) {
			return "COMPLETED";
		}
		return failures == executions.size() ? "FAILED" : "PARTIAL";
	}

	private static Map<String, Long> groupByCategory(List<Finding> findings) {
		Map<String, Long> byCategory = new LinkedHashMap<>();
		for (AgentType category : CATEGORIES) {
			byCategory.put(
					category.name(),
					findings.stream().filter(finding -> finding.getAgentType() == category).count());
		}
		return byCategory;
	}

	private static long countBySeverity(List<Finding> findings, Severity severity) {
		return findings.stream().filter(finding -> finding.getSeverity() == severity).count();
	}
}
