package com.codesentinel.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityAnalysisResponseTest {

	@Test
	void summarizesFindingsBySeverityAndPreservesLineNumbers() {
		SecurityAnalysisResponse response =
				SecurityAnalysisResponse.from(7L, List.of(
						finding(Severity.HIGH, "Hard coded password", "com/example/Db.java", 8),
						finding(Severity.HIGH, "SQL injection", "com/example/Dao.java", 40),
						finding(Severity.MEDIUM, "Predictable random", "com/example/Rng.java", 5),
						finding(Severity.LOW, "Possible null dereference", "com/example/Svc.java",
								20)));

		assertEquals(7L, response.getAnalysisId());
		assertEquals(4, response.getTotalFindings());
		assertEquals(2, response.getHighSeverity());
		assertEquals(1, response.getMediumSeverity());
		assertEquals(1, response.getLowSeverity());

		FindingDto first = response.getFindings().get(0);
		assertEquals(AgentType.SECURITY.name(), first.getAgentType());
		assertEquals("com/example/Db.java", first.getFilePath());
		assertEquals(8, first.getLineNumber());
	}

	private Finding finding(Severity severity, String title, String filePath, int line) {
		Finding finding = new Finding();
		finding.setAgentType(AgentType.SECURITY);
		finding.setSeverity(severity);
		finding.setTitle(title);
		finding.setDescription(title + " detected");
		finding.setFilePath(filePath);
		finding.setLineNumber(line);
		return finding;
	}
}
