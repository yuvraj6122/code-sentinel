package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesentinel.config.DuplicationProperties;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DuplicateCodeAnalysisAgentImplTest {

	@Test
	void detectsDuplicatedBlockAcrossFiles(@TempDir Path repo) throws Exception {
		String duplicatedBody = duplicatedMethod();
		Files.writeString(repo.resolve("UserService.java"), classWith("UserService", duplicatedBody));
		Files.writeString(repo.resolve("AdminService.java"), classWith("AdminService", duplicatedBody));

		DuplicateCodeAnalysisAgentImpl agent =
				new DuplicateCodeAnalysisAgentImpl(new DuplicationProperties(), new CpdReportParser());

		List<Finding> findings = agent.analyze(repo);

		assertFalse(findings.isEmpty(), "expected at least one duplicate finding");
		assertTrue(
				findings.stream().allMatch(f -> f.getAgentType() == AgentType.DUPLICATE_CODE),
				"every finding should be a DUPLICATE_CODE finding");

		Finding finding = findings.get(0);
		assertEquals("Duplicate Code Detected", finding.getTitle());
		assertNotNull(finding.getSeverity(), "a reported duplication must have a severity");
		assertNotNull(finding.getFilePath(), "file path should be populated");
		assertFalse(
				finding.getFilePath().startsWith("/"),
				"file path should be repository-relative, not an absolute clone path: "
						+ finding.getFilePath());
		assertTrue(
				List.of("UserService.java", "AdminService.java").contains(finding.getFilePath()),
				"file path should be relative to the repository root: " + finding.getFilePath());
		assertTrue(
				finding.getDescription().contains("duplicated lines"),
				"description should mention duplicated lines: " + finding.getDescription());
		assertTrue(
				finding.getDescription().contains("UserService.java")
						|| finding.getDescription().contains("AdminService.java"),
				"description should reference the involved files: " + finding.getDescription());
	}

	@Test
	void producesNoFindingsWhenThereIsNoDuplication(@TempDir Path repo) throws Exception {
		Files.writeString(
				repo.resolve("Solo.java"),
				"public class Solo {\n\tpublic int add(int a, int b) {\n\t\treturn a + b;\n\t}\n}\n");

		DuplicateCodeAnalysisAgentImpl agent =
				new DuplicateCodeAnalysisAgentImpl(new DuplicationProperties(), new CpdReportParser());

		assertTrue(agent.analyze(repo).isEmpty(), "no duplication expected for a single small file");
	}

	private String classWith(String className, String body) {
		return "public class %s {\n%s\n}\n".formatted(className, body);
	}

	private String duplicatedMethod() {
		StringBuilder body = new StringBuilder("\tpublic int compute(int seed) {\n");
		body.append("\t\tint total = seed;\n");
		for (int i = 0; i < 60; i++) {
			body.append("\t\ttotal = total + %d; total = total * 2; total = total - 1;\n".formatted(i));
		}
		body.append("\t\treturn total;\n");
		body.append("\t}\n");
		return body.toString();
	}
}
