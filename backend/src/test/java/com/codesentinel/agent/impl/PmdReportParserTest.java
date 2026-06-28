package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PmdReportParserTest {

	private final PmdReportParser parser = new PmdReportParser();

	@Test
	void parsesMethodAndClassLevelViolations(@TempDir Path tempDir) throws Exception {
		String xml =
				"""
				<?xml version="1.0" encoding="UTF-8"?>
				<pmd xmlns="http://pmd.sourceforge.net/report/2.0.0" version="7.25.0">
					<file name="OrderService.java">
						<violation beginline="10" endline="40" rule="CyclomaticComplexity"
								ruleset="Design" class="OrderService" method="process" priority="3">
							The method 'process()' has a cyclomatic complexity of 18.
						</violation>
						<violation beginline="1" endline="600" rule="NcssCount"
								ruleset="Design" class="OrderService" priority="3">
							The class 'OrderService' has a NCSS line count of 600.
						</violation>
					</file>
				</pmd>
				""";
		Path report = tempDir.resolve("pmd-report.xml");
		Files.writeString(report, xml);

		List<PmdViolation> violations = parser.parse(report);

		assertEquals(2, violations.size());

		PmdViolation cyclomatic = violations.get(0);
		assertEquals("CyclomaticComplexity", cyclomatic.getRule());
		assertEquals("OrderService.java", cyclomatic.getFilePath());
		assertEquals("process", cyclomatic.getMethodName());
		assertTrue(cyclomatic.isMethodLevel());
		assertEquals(18, cyclomatic.getMetricValue());

		PmdViolation ncss = violations.get(1);
		assertEquals("NcssCount", ncss.getRule());
		assertNull(ncss.getMethodName());
		assertFalse(ncss.isMethodLevel());
		assertEquals(600, ncss.getMetricValue());
	}
}
