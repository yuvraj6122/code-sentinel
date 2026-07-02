package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CpdReportParserTest {

	private final CpdReportParser parser = new CpdReportParser();

	@Test
	void parsesDuplicationBlocksWithFileMarks(@TempDir Path tempDir) throws Exception {
		String xml =
				"""
				<?xml version="1.0" encoding="UTF-8"?>
				<pmd-cpd>
					<duplication lines="85" tokens="420">
						<file column="2" endcolumn="3" endline="110" line="25" path="src/UserService.java"/>
						<file column="2" endcolumn="3" endline="145" line="60" path="src/AdminService.java"/>
						<codefragment><![CDATA[ duplicated code here ]]></codefragment>
					</duplication>
					<duplication lines="30" tokens="150">
						<file column="1" endcolumn="1" endline="49" line="20" path="src/Report.java"/>
						<file column="1" endcolumn="1" endline="79" line="50" path="src/Report.java"/>
					</duplication>
				</pmd-cpd>
				""";
		Path report = tempDir.resolve("cpd-report.xml");
		Files.writeString(report, xml);

		List<CpdDuplication> duplications = parser.parse(report);

		assertEquals(2, duplications.size());

		CpdDuplication first = duplications.get(0);
		assertEquals(85, first.getLines());
		assertEquals(420, first.getTokens());
		assertEquals(2, first.getMarks().size());
		assertEquals("src/UserService.java", first.getMarks().get(0).getFilePath());
		assertEquals("UserService.java", first.getMarks().get(0).getFileName());
		assertEquals(25, first.getMarks().get(0).getStartLine());
		assertEquals(110, first.getMarks().get(0).getEndLine());
		assertEquals("AdminService.java", first.getMarks().get(1).getFileName());

		CpdDuplication second = duplications.get(1);
		assertEquals(30, second.getLines());
		assertTrue(
				second.getMarks().stream().allMatch(mark -> "Report.java".equals(mark.getFileName())),
				"both marks should reference the same file");
	}

	@Test
	void returnsEmptyListWhenNoDuplications(@TempDir Path tempDir) throws Exception {
		String xml =
				"""
				<?xml version="1.0" encoding="UTF-8"?>
				<pmd-cpd>
				</pmd-cpd>
				""";
		Path report = tempDir.resolve("empty-cpd-report.xml");
		Files.writeString(report, xml);

		assertTrue(parser.parse(report).isEmpty());
	}
}
