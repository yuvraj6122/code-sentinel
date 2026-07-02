package com.codesentinel.agent.impl;

import com.codesentinel.agent.DuplicateCodeAnalysisAgent;
import com.codesentinel.config.DuplicationProperties;
import com.codesentinel.exception.DuplicationAnalysisException;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pmd.cpd.CPDConfiguration;
import net.sourceforge.pmd.cpd.CpdAnalysis;
import net.sourceforge.pmd.cpd.XMLRenderer;
import net.sourceforge.pmd.lang.Language;
import org.springframework.stereotype.Component;

/**
 * Detects copy/pasted code using PMD CPD (Copy/Paste Detector), then grades each
 * duplicated block by line count into {@link Finding}s. Mirrors the structure of
 * {@link ComplexityAnalysisAgentImpl}: run the PMD tool, emit an XML report,
 * parse it, and translate into the project's internal model.
 */
@Slf4j
@Component
public class DuplicateCodeAnalysisAgentImpl implements DuplicateCodeAnalysisAgent {

	private static final String JAVA_LANGUAGE_ID = "java";

	private final DuplicationProperties properties;
	private final CpdReportParser reportParser;

	public DuplicateCodeAnalysisAgentImpl(
			DuplicationProperties properties, CpdReportParser reportParser) {
		this.properties = properties;
		this.reportParser = reportParser;
	}

	@Override
	public List<Finding> analyze(Path repositoryPath) {
		log.info("Running duplicate code analysis on {}", repositoryPath);

		Path reportFile = null;
		try {
			reportFile = Files.createTempFile("codesentinel-cpd-report-", ".xml");
			runCpd(repositoryPath, reportFile);

			List<Finding> findings = new ArrayList<>();
			for (CpdDuplication duplication : reportParser.parse(reportFile)) {
				toFinding(duplication, repositoryPath).ifPresent(findings::add);
			}

			log.info("Duplicate code analysis produced {} findings", findings.size());
			return findings;
		} catch (IOException e) {
			throw new DuplicationAnalysisException(
					"Duplicate code analysis failed: " + e.getMessage(), e);
		} finally {
			deleteQuietly(reportFile);
		}
	}

	private Optional<Finding> toFinding(CpdDuplication duplication, Path repositoryPath) {
		Severity severity = properties.getLines().classify(duplication.getLines());
		if (severity == null) {
			return Optional.empty();
		}

		Finding finding = new Finding();
		finding.setAgentType(AgentType.DUPLICATE_CODE);
		finding.setSeverity(severity);
		finding.setTitle("Duplicate Code Detected");
		finding.setFilePath(primaryPath(duplication, repositoryPath));
		finding.setDescription(describe(duplication));
		return Optional.of(finding);
	}

	private String describe(CpdDuplication duplication) {
		Set<String> names = new LinkedHashSet<>();
		for (CpdMark mark : duplication.getMarks()) {
			names.add(mark.getFileName());
		}

		String location = names.size() <= 1
				? "within " + (names.isEmpty() ? "a single file" : names.iterator().next())
				: "between " + joinNames(names);

		return "%d duplicated lines detected %s.".formatted(duplication.getLines(), location);
	}

	private String joinNames(Set<String> names) {
		List<String> list = new ArrayList<>(names);
		if (list.size() == 2) {
			return list.get(0) + " and " + list.get(1);
		}
		String head = String.join(", ", list.subList(0, list.size() - 1));
		return head + " and " + list.get(list.size() - 1);
	}

	private String primaryPath(CpdDuplication duplication, Path repositoryPath) {
		return duplication.getMarks().isEmpty()
				? null
				: relativize(duplication.getMarks().get(0).getFilePath(), repositoryPath);
	}

	/**
	 * CPD reports absolute paths from the local clone; convert them to
	 * repository-relative paths so findings don't leak the temporary clone
	 * location (e.g. {@code /tmp/codesentinel/analysis-2/...}).
	 */
	private String relativize(String filePath, Path repositoryPath) {
		if (filePath == null || filePath.isBlank()) {
			return filePath;
		}

		Path file = Path.of(filePath);
		if (!file.isAbsolute()) {
			return filePath;
		}

		Path root = repositoryPath.toAbsolutePath().normalize();
		Path normalizedFile = file.normalize();
		if (normalizedFile.startsWith(root)) {
			return root.relativize(normalizedFile).toString();
		}

		// Fall back to real paths to handle symlinked roots (e.g. /tmp -> /private/tmp).
		try {
			Path realRoot = repositoryPath.toRealPath();
			Path realFile = file.toRealPath();
			if (realFile.startsWith(realRoot)) {
				return realRoot.relativize(realFile).toString();
			}
		} catch (IOException ignored) {
			// Keep the original path if it cannot be resolved.
		}

		return filePath;
	}

	private void runCpd(Path repositoryPath, Path reportFile) {
		CPDConfiguration config = new CPDConfiguration();
		config.setMinimumTileSize(properties.getMinimumTokens());
		config.setSourceEncoding(StandardCharsets.UTF_8);
		config.addInputPath(repositoryPath);
		config.addRelativizeRoot(repositoryPath);

		Language java = config.getLanguageRegistry().getLanguageById(JAVA_LANGUAGE_ID);
		if (java != null) {
			config.setOnlyRecognizeLanguage(java);
		}

		try (CpdAnalysis cpd = CpdAnalysis.create(config)) {
			cpd.performAnalysis(report -> {
				try (Writer writer = Files.newBufferedWriter(reportFile, StandardCharsets.UTF_8)) {
					new XMLRenderer("UTF-8").render(report, writer);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (IOException | UncheckedIOException e) {
			throw new DuplicationAnalysisException(
					"CPD analysis failed: " + e.getMessage(), e);
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
