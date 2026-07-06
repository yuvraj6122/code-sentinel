package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesentinel.config.SecurityProperties;
import com.codesentinel.model.Finding;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecurityAnalysisAgentImplTest {

	private SecurityAnalysisAgentImpl newAgent(SecurityProperties properties) {
		SecuritySeverityMapper mapper = new SecuritySeverityMapper();
		return new SecurityAnalysisAgentImpl(
				properties,
				new RepositoryBuilder(properties),
				new SpotBugsRunner(),
				new SpotBugsReportParser(),
				new SecurityFindingFactory(properties, mapper));
	}

	@Test
	void returnsNoFindingsWhenThereIsNoBytecodeToAnalyze(@TempDir Path repo) throws Exception {
		// A source-only checkout with no build tool: nothing compiles, so SpotBugs
		// is never invoked and the agent degrades gracefully to an empty result.
		Files.createDirectories(repo.resolve("src/main/java/com/example"));
		Files.writeString(repo.resolve("src/main/java/com/example/App.java"), """
				package com.example;

				public class App {}
				""");

		SecurityProperties properties = new SecurityProperties();
		properties.setBuildEnabled(false);

		List<Finding> findings = newAgent(properties).analyze(repo);

		assertTrue(findings.isEmpty());
	}
}
