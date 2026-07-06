package com.codesentinel.agent.impl;

import com.codesentinel.agent.SecurityAnalysisAgent;
import com.codesentinel.config.SecurityProperties;
import com.codesentinel.exception.SecurityAnalysisException;
import com.codesentinel.model.Finding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Analyzes a repository for security vulnerabilities and security-relevant code
 * quality issues using SpotBugs + FindSecBugs — no custom scanning logic is
 * added where these proven tools already detect an issue.
 *
 * <p>Mirrors the other analysis agents (build the tool report, parse it, convert
 * to the shared {@link Finding} model), with an extra best-effort build step
 * because SpotBugs analyzes bytecode: build → run SpotBugs → parse XML → filter
 * to security findings.
 */
@Slf4j
@Component
public class SecurityAnalysisAgentImpl implements SecurityAnalysisAgent {

	private final SecurityProperties properties;
	private final RepositoryBuilder repositoryBuilder;
	private final SpotBugsRunner spotBugsRunner;
	private final SpotBugsReportParser reportParser;
	private final SecurityFindingFactory findingFactory;

	public SecurityAnalysisAgentImpl(
			SecurityProperties properties,
			RepositoryBuilder repositoryBuilder,
			SpotBugsRunner spotBugsRunner,
			SpotBugsReportParser reportParser,
			SecurityFindingFactory findingFactory) {
		this.properties = properties;
		this.repositoryBuilder = repositoryBuilder;
		this.spotBugsRunner = spotBugsRunner;
		this.reportParser = reportParser;
		this.findingFactory = findingFactory;
	}

	@Override
	public List<Finding> analyze(Path repositoryPath) {
		log.info("Running security analysis on {}", repositoryPath);

		List<Path> classDirectories = repositoryBuilder.build(repositoryPath);
		if (classDirectories.isEmpty()) {
			log.warn("No compiled bytecode available for {}; skipping SpotBugs analysis",
					repositoryPath);
			return List.of();
		}

		Path reportFile = null;
		try {
			reportFile = Files.createTempFile("codesentinel-spotbugs-report-", ".xml");
			spotBugsRunner.run(
					classDirectories, repositoryPath, reportFile, properties.getMinPriority());

			List<Finding> findings = new ArrayList<>();
			for (SpotBugsBug bug : reportParser.parse(reportFile)) {
				findingFactory.toFinding(bug).ifPresent(findings::add);
			}

			log.info("Security analysis produced {} findings", findings.size());
			return findings;
		} catch (IOException e) {
			throw new SecurityAnalysisException(
					"Security analysis failed: " + e.getMessage(), e);
		} finally {
			deleteQuietly(reportFile);
		}
	}

	private void deleteQuietly(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// Temp files are best-effort cleanup.
		}
	}
}
