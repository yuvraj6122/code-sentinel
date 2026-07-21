package com.codesentinel.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.model.Severity;
import com.codesentinel.service.AgentExecutionResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UnifiedAnalysisResponseTest {

	@Test
	void aggregatesSeveritiesAndCategoriesAndMarksPartialWhenAnAgentFails() {
		List<AgentExecutionResult> executions = List.of(
				AgentExecutionResult.completed(
						AgentType.COMPLEXITY,
						List.of(finding(AgentType.COMPLEXITY, Severity.HIGH),
								finding(AgentType.COMPLEXITY, Severity.LOW))),
				AgentExecutionResult.completed(
						AgentType.DUPLICATE_CODE,
						List.of(finding(AgentType.DUPLICATE_CODE, Severity.MEDIUM))),
				AgentExecutionResult.completed(AgentType.TESTING, List.of()),
				AgentExecutionResult.failed(AgentType.SECURITY, "SpotBugs crashed"));

		UnifiedAnalysisResponse response =
				UnifiedAnalysisResponse.from(11L, repository(), executions);

		assertEquals(11L, response.getAnalysisId());
		assertEquals("https://github.com/owner/repo", response.getRepository());
		assertEquals("repo", response.getRepositoryName());
		assertEquals("PARTIAL", response.getStatus());

		assertEquals(3, response.getTotalFindings());
		assertEquals(0, response.getCriticalSeverity());
		assertEquals(1, response.getHighSeverity());
		assertEquals(1, response.getMediumSeverity());
		assertEquals(1, response.getLowSeverity());

		Map<String, Long> byCategory = response.getFindingsByCategory();
		assertEquals(2L, byCategory.get("COMPLEXITY"));
		assertEquals(1L, byCategory.get("DUPLICATE_CODE"));
		assertEquals(0L, byCategory.get("TESTING"));
		assertEquals(0L, byCategory.get("SECURITY"));

		assertEquals(4, response.getAgentExecutions().size());
		AgentExecutionDto security = response.getAgentExecutions().get(3);
		assertEquals("SECURITY", security.getAgent());
		assertEquals("FAILED", security.getStatus());
		assertEquals(0, security.getFindings());
		assertEquals("SpotBugs crashed", security.getMessage());

		AgentExecutionDto complexity = response.getAgentExecutions().get(0);
		assertEquals("COMPLETED", complexity.getStatus());
		assertEquals(2, complexity.getFindings());
		assertNull(complexity.getMessage());

		assertEquals(3, response.getFindings().size());
	}

	@Test
	void marksCompletedWhenEveryAgentSucceedsAndCountsCritical() {
		List<AgentExecutionResult> executions = List.of(
				AgentExecutionResult.completed(
						AgentType.SECURITY, List.of(finding(AgentType.SECURITY, Severity.CRITICAL))),
				AgentExecutionResult.completed(AgentType.TESTING, List.of()));

		UnifiedAnalysisResponse response =
				UnifiedAnalysisResponse.from(1L, repository(), executions);

		assertEquals("COMPLETED", response.getStatus());
		assertEquals(1, response.getTotalFindings());
		assertEquals(1, response.getCriticalSeverity());
	}

	@Test
	void marksFailedWhenEveryAgentFails() {
		List<AgentExecutionResult> executions = List.of(
				AgentExecutionResult.failed(AgentType.COMPLEXITY, "boom"),
				AgentExecutionResult.failed(AgentType.SECURITY, "boom"));

		UnifiedAnalysisResponse response =
				UnifiedAnalysisResponse.from(2L, repository(), executions);

		assertEquals("FAILED", response.getStatus());
		assertEquals(0, response.getTotalFindings());
	}

	private RepositoryEntity repository() {
		RepositoryEntity repository = new RepositoryEntity();
		repository.setGithubUrl("https://github.com/owner/repo");
		repository.setName("repo");
		return repository;
	}

	private Finding finding(AgentType agentType, Severity severity) {
		Finding finding = new Finding();
		finding.setAgentType(agentType);
		finding.setSeverity(severity);
		finding.setTitle(agentType.name() + " issue");
		finding.setDescription("description");
		return finding;
	}
}
