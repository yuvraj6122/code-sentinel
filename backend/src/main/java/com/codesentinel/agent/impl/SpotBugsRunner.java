package com.codesentinel.agent.impl;

import com.codesentinel.exception.SecurityAnalysisException;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.config.UserPreferences;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Runs SpotBugs (with the FindSecBugs plugin, discovered on the classpath) over
 * a set of compiled class directories and writes an XML report. This mirrors how
 * {@code ComplexityAnalysisAgentImpl}/{@code DuplicateCodeAnalysisAgentImpl}
 * drive PMD embedded and emit an XML report for a dedicated parser to read.
 */
@Slf4j
@Component
public class SpotBugsRunner {

	public void run(List<Path> classDirectories, Path repositoryPath, Path reportFile,
			int minPriority) {
		Project project = new Project();
		project.setProjectName("codesentinel-security");
		for (Path classDir : classDirectories) {
			project.addFile(classDir.toAbsolutePath().toString());
		}
		addSourceDirectories(project, repositoryPath);

		FindBugs2 findBugs = new FindBugs2();
		try (OutputStream out = Files.newOutputStream(reportFile)) {
			PrintStream reportStream = new PrintStream(out, false, StandardCharsets.UTF_8);

			XMLBugReporter reporter = new XMLBugReporter(project);
			reporter.setPriorityThreshold(minPriority);
			reporter.setAddMessages(true);
			reporter.setOutputStream(reportStream);

			UserPreferences preferences = UserPreferences.createDefaultUserPreferences();
			preferences.enableAllDetectors(true);

			findBugs.setProject(project);
			findBugs.setBugReporter(reporter);
			findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
			findBugs.setUserPreferences(preferences);
			findBugs.setNoClassOk(true);

			log.info("Running SpotBugs + FindSecBugs over {} class directory(ies)",
					classDirectories.size());
			findBugs.execute();
			reportStream.flush();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SecurityAnalysisException("SpotBugs analysis was interrupted", e);
		} catch (IOException | RuntimeException e) {
			throw new SecurityAnalysisException(
					"SpotBugs analysis failed: " + e.getMessage(), e);
		}
	}

	/** Registering source roots lets SpotBugs resolve line numbers in findings. */
	private void addSourceDirectories(Project project, Path repositoryPath) {
		try (Stream<Path> paths = Files.walk(repositoryPath)) {
			paths.filter(Files::isDirectory)
					.filter(dir -> dir.toString().replace('\\', '/').endsWith("/src/main/java"))
					.forEach(dir -> project.addSourceDir(dir.toAbsolutePath().toString()));
		} catch (IOException e) {
			log.debug("Could not enumerate source directories: {}", e.getMessage());
		}
	}
}
