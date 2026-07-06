package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpotBugsReportParserTest {

	private final SpotBugsReportParser parser = new SpotBugsReportParser();

	@Test
	void parsesBugInstancesWithMessagesAndPrimarySourceLine(@TempDir Path tempDir)
			throws Exception {
		String xml =
				"""
				<?xml version="1.0" encoding="UTF-8"?>
				<BugCollection version="4.8.6" sequence="0">
					<BugInstance type="PREDICTABLE_RANDOM" priority="2" rank="14" category="SECURITY">
						<ShortMessage>Predictable pseudorandom number generator</ShortMessage>
						<LongMessage>Use of java.util.Random is predictable in Example.java</LongMessage>
						<Class classname="com.example.Example" primary="true">
							<SourceLine classname="com.example.Example" start="5" end="20"
									sourcefile="Example.java" sourcepath="com/example/Example.java"/>
						</Class>
						<SourceLine classname="com.example.Example" start="12" end="12"
								sourcefile="Example.java" sourcepath="com/example/Example.java"
								primary="true"/>
					</BugInstance>
					<BugInstance type="HARD_CODE_PASSWORD" priority="1" rank="7" category="SECURITY">
						<ShortMessage>Hard coded password</ShortMessage>
						<LongMessage>Hard coded password found in Db.java</LongMessage>
						<SourceLine classname="com.example.Db" start="8" end="8"
								sourcefile="Db.java" sourcepath="com/example/Db.java" primary="true"/>
					</BugInstance>
				</BugCollection>
				""";
		Path report = tempDir.resolve("spotbugs.xml");
		Files.writeString(report, xml);

		List<SpotBugsBug> bugs = parser.parse(report);

		assertEquals(2, bugs.size());

		SpotBugsBug random = bugs.get(0);
		assertEquals("PREDICTABLE_RANDOM", random.getType());
		assertEquals("SECURITY", random.getCategory());
		assertEquals(2, random.getPriority());
		assertEquals("Predictable pseudorandom number generator", random.getTitle());
		assertEquals("Use of java.util.Random is predictable in Example.java",
				random.getDescription());
		assertEquals("com/example/Example.java", random.getFilePath());
		assertEquals(12, random.getLineNumber());

		SpotBugsBug password = bugs.get(1);
		assertEquals("HARD_CODE_PASSWORD", password.getType());
		assertEquals(1, password.getPriority());
		assertEquals("com/example/Db.java", password.getFilePath());
		assertEquals(8, password.getLineNumber());
	}

	@Test
	void fallsBackToHumanizedTypeWhenMessagesAbsent(@TempDir Path tempDir) throws Exception {
		String xml =
				"""
				<?xml version="1.0" encoding="UTF-8"?>
				<BugCollection version="4.8.6">
					<BugInstance type="SQL_INJECTION_JDBC" priority="1" rank="5" category="SECURITY">
					</BugInstance>
				</BugCollection>
				""";
		Path report = tempDir.resolve("spotbugs.xml");
		Files.writeString(report, xml);

		List<SpotBugsBug> bugs = parser.parse(report);

		assertEquals(1, bugs.size());
		assertEquals("Sql Injection Jdbc", bugs.get(0).getTitle());
		assertNull(bugs.get(0).getFilePath());
		assertNull(bugs.get(0).getLineNumber());
	}
}
