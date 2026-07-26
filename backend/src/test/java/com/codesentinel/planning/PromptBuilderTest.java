package com.codesentinel.planning;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesentinel.config.PlanningProperties;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

	@Test
	void systemPromptAsksForJsonOnlyAndBoundsRecommendations() {
		PlanningProperties properties = new PlanningProperties();
		properties.setMaxRecommendations(5);
		PromptBuilder builder = new PromptBuilder(properties);

		PromptBuilder.Prompt prompt = builder.build(repository(), List.of());

		assertTrue(prompt.system().contains("JSON"));
		assertTrue(prompt.system().contains("overallAssessment"));
		assertTrue(prompt.system().contains("affectedCategories"));
		// The configured recommendation cap is injected into the instructions.
		assertTrue(prompt.system().contains("AT MOST 5 recommendations"));
	}

	@Test
	void userPromptIncludesMetadataSeverityCountsAndCategoryBreakdown() {
		PromptBuilder builder = new PromptBuilder(new PlanningProperties());

		PromptBuilder.Prompt prompt = builder.build(
				repository(),
				List.of(
						finding(AgentType.SECURITY, Severity.CRITICAL, "SQL injection", "Dao.java", 42),
						finding(AgentType.TESTING, Severity.LOW, "No assertions", null, null)));

		String user = prompt.user();
		assertTrue(user.contains("Repository: demo"));
		assertTrue(user.contains("Language: JAVA"));
		assertTrue(user.contains("Total findings: 2"));
		assertTrue(user.contains("CRITICAL: 1"));
		assertTrue(user.contains("## SECURITY"));
		assertTrue(user.contains("SQL injection"));
		assertTrue(user.contains("Dao.java:42"));
		// Categories with no findings are still listed as empty.
		assertTrue(user.contains("## DUPLICATE_CODE (0 findings)"));
	}

	@Test
	void userPromptCapsFindingsPerCategoryAndKeepsHighestSeverity() {
		PlanningProperties properties = new PlanningProperties();
		properties.setMaxFindingsPerCategory(2);
		PromptBuilder builder = new PromptBuilder(properties);

		PromptBuilder.Prompt prompt = builder.build(
				repository(),
				List.of(
						finding(AgentType.SECURITY, Severity.CRITICAL, "critical-bug", null, null),
						finding(AgentType.SECURITY, Severity.HIGH, "high-bug", null, null),
						finding(AgentType.SECURITY, Severity.LOW, "low-bug", null, null)));

		String user = prompt.user();
		// Only the two highest-severity findings are included; the low one is dropped.
		assertTrue(user.contains("critical-bug"));
		assertTrue(user.contains("high-bug"));
		assertFalse(user.contains("low-bug"));
	}

	private RepositoryEntity repository() {
		RepositoryEntity repository = new RepositoryEntity();
		repository.setName("demo");
		repository.setLanguage("JAVA");
		repository.setBuildTool("GRADLE");
		return repository;
	}

	private Finding finding(
			AgentType agentType, Severity severity, String title, String filePath, Integer line) {
		Finding finding = new Finding();
		finding.setAgentType(agentType);
		finding.setSeverity(severity);
		finding.setTitle(title);
		finding.setDescription("description");
		finding.setFilePath(filePath);
		finding.setLineNumber(line);
		return finding;
	}
}
