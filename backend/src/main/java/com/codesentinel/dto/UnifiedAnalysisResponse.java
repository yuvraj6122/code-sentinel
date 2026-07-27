package com.codesentinel.dto;

import com.codesentinel.agent.TestingMetrics;
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
 * <p>It also carries the repository {@link RepositoryMetadataDto metadata} and the
 * Testing Analysis Agent's {@link TestingMetrics} so the dashboard can render
 * every metric from this single response — no per-agent endpoints required. The
 * flat {@code findings} list lets the client derive each category's findings and
 * severity counts by filtering on {@code agentType}.
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
	private RepositoryMetadataDto metadata;
	private String status;
	private int totalFindings;
	private long criticalSeverity;
	private long highSeverity;
	private long mediumSeverity;
	private long lowSeverity;
	private Map<String, Long> findingsByCategory;
	private List<AgentExecutionDto> agentExecutions;
	private List<FindingDto> findings;
	private TestingMetrics testingMetrics;

	/**
	 * Backward-compatible summary without repository metadata or testing metrics.
	 * Retained for callers that only need the aggregated finding summary.
	 */
	public static UnifiedAnalysisResponse from(
			Long analysisId, RepositoryEntity repository, List<AgentExecutionResult> executions) {
		return from(analysisId, repository, null, executions, null);
	}

	public static UnifiedAnalysisResponse from(
			Long analysisId,
			RepositoryEntity repository,
			RepositoryMetadataDto metadata,
			List<AgentExecutionResult> executions,
			TestingMetrics testingMetrics) {
		List<Finding> findings = executions.stream()
				.flatMap(execution -> execution.getFindings().stream())
				.toList();

		return new UnifiedAnalysisResponse(
				analysisId,
				repository.getGithubUrl(),
				repository.getName(),
				resolveMetadata(repository, metadata),
				resolveStatus(executions),
				findings.size(),
				countBySeverity(findings, Severity.CRITICAL),
				countBySeverity(findings, Severity.HIGH),
				countBySeverity(findings, Severity.MEDIUM),
				countBySeverity(findings, Severity.LOW),
				groupByCategory(findings),
				executions.stream().map(AgentExecutionDto::from).toList(),
				findings.stream().map(FindingDto::from).toList(),
				testingMetrics);
	}

	/** Uses the scanned metadata when available, otherwise falls back to the entity. */
	private static RepositoryMetadataDto resolveMetadata(
			RepositoryEntity repository, RepositoryMetadataDto metadata) {
		if (metadata != null) {
			return metadata;
		}
		return new RepositoryMetadataDto(
				repository.getName(), repository.getLanguage(), repository.getBuildTool(), 0, 0);
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
