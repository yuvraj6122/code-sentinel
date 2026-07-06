package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesentinel.config.SecurityProperties;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SecurityFindingFactoryTest {

	private final SecurityFindingFactory factory =
			new SecurityFindingFactory(new SecurityProperties(), new SecuritySeverityMapper());

	@Test
	void convertsSecurityBugIntoFindingWithLineNumber() {
		SpotBugsBug bug = new SpotBugsBug(
				"HARD_CODE_PASSWORD", "SECURITY", 1, 7,
				"Hard coded password", "Hard coded password found", "com/example/Db.java", 8);

		Optional<Finding> result = factory.toFinding(bug);

		assertTrue(result.isPresent());
		Finding finding = result.get();
		assertEquals(AgentType.SECURITY, finding.getAgentType());
		assertEquals(Severity.HIGH, finding.getSeverity());
		assertEquals("Hard coded password", finding.getTitle());
		assertEquals("com/example/Db.java", finding.getFilePath());
		assertEquals(8, finding.getLineNumber());
	}

	@Test
	void keepsNonSecurityCategoryWhenTypePrefixMatches() {
		SpotBugsBug nullDeref = new SpotBugsBug(
				"NP_NULL_ON_SOME_PATH", "CORRECTNESS", 2, 12,
				"Possible null dereference", "May dereference null", "com/example/Svc.java", 20);

		Optional<Finding> result = factory.toFinding(nullDeref);

		assertTrue(result.isPresent());
		assertEquals(Severity.MEDIUM, result.get().getSeverity());
	}

	@Test
	void dropsNonSecurityBugsThatDoNotMatchAnyPrefix() {
		SpotBugsBug styleBug = new SpotBugsBug(
				"UUF_UNUSED_FIELD", "STYLE", 2, 18,
				"Unused field", "Field is never read", "com/example/Svc.java", 3);

		assertTrue(factory.toFinding(styleBug).isEmpty());
	}

	@Test
	void dropsBugsBelowThePriorityThreshold() {
		SpotBugsBug experimental = new SpotBugsBug(
				"WEAK_MESSAGE_DIGEST_MD5", "SECURITY", 4, 15,
				"Weak digest", "MD5 used", "com/example/Hashing.java", 42);

		assertTrue(factory.toFinding(experimental).isEmpty());
	}
}
