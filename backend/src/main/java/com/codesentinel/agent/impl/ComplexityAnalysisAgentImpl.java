package com.codesentinel.agent.impl;

import com.codesentinel.agent.ComplexityAnalysisAgent;
import com.codesentinel.config.ComplexityProperties;
import com.codesentinel.exception.ComplexityAnalysisException;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Severity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ComplexityAnalysisAgentImpl implements ComplexityAnalysisAgent {

	private static final String CYCLOMATIC_RULE = "CyclomaticComplexity";

	private final ComplexityProperties properties;
	private final PmdReportParser reportParser;
	private final SourceLengthAnalyzer lengthAnalyzer;

	public ComplexityAnalysisAgentImpl(
			ComplexityProperties properties,
			PmdReportParser reportParser,
			SourceLengthAnalyzer lengthAnalyzer) {
		this.properties = properties;
		this.reportParser = reportParser;
		this.lengthAnalyzer = lengthAnalyzer;
	}

	@Override
	public List<Finding> analyze(Path repositoryPath) {
		log.info("Running complexity analysis on {}", repositoryPath);

		List<Finding> findings = new ArrayList<>();
		findings.addAll(analyzeCyclomaticComplexity(repositoryPath));
		findings.addAll(analyzeLength(repositoryPath));

		log.info("Complexity analysis produced {} findings", findings.size());
		return findings;
	}

	private List<Finding> analyzeCyclomaticComplexity(Path repositoryPath) {
		Path rulesetFile = null;
		Path reportFile = null;
		try {
			rulesetFile = writeRuleset();
			reportFile = Files.createTempFile("codesentinel-pmd-report-", ".xml");
			runPmd(repositoryPath, rulesetFile, reportFile);

			List<Finding> findings = new ArrayList<>();
			for (PmdViolation violation : reportParser.parse(reportFile)) {
				if (CYCLOMATIC_RULE.equals(violation.getRule()) && violation.isMethodLevel()) {
					toComplexityFinding(violation).ifPresent(findings::add);
				}
			}
			return findings;
		} catch (IOException e) {
			throw new ComplexityAnalysisException(
					"Cyclomatic complexity analysis failed: " + e.getMessage(), e);
		} finally {
			deleteQuietly(rulesetFile);
			deleteQuietly(reportFile);
		}
	}

	private Optional<Finding> toComplexityFinding(PmdViolation violation) {
		int value = violation.getMetricValue() == null ? 0 : violation.getMetricValue();
		Severity severity = properties.getCyclomatic().classify(value);
		if (severity == null) {
			return Optional.empty();
		}
		Finding finding = newFinding(severity, violation.getFilePath());
		finding.setTitle(titleCase(severity) + " Cyclomatic Complexity");
		finding.setDescription(violation.getDescription());
		return Optional.of(finding);
	}

	private List<Finding> analyzeLength(Path repositoryPath) {
		List<Finding> findings = new ArrayList<>();
		for (LengthMeasurement measurement : lengthAnalyzer.analyze(repositoryPath)) {
			boolean isMethod = measurement.getKind() == LengthMeasurement.Kind.METHOD;
			ComplexityProperties.Thresholds thresholds =
					isMethod ? properties.getMethodLength() : properties.getClassLength();

			Severity severity = thresholds.classify(measurement.getNonEmptyLines());
			if (severity == null) {
				continue;
			}

			Finding finding = newFinding(severity, measurement.getFilePath());
			finding.setTitle(isMethod ? "Method Too Long" : "Large Class");
			finding.setDescription("%s '%s' has %d non-empty lines."
					.formatted(
							isMethod ? "Method" : "Class",
							measurement.getName(),
							measurement.getNonEmptyLines()));
			findings.add(finding);
		}
		return findings;
	}

	private Finding newFinding(Severity severity, String filePath) {
		Finding finding = new Finding();
		finding.setAgentType(AgentType.COMPLEXITY);
		finding.setSeverity(severity);
		finding.setFilePath(filePath);
		return finding;
	}

	private void runPmd(Path repositoryPath, Path rulesetFile, Path reportFile) {
		PMDConfiguration config = new PMDConfiguration();
		config.addInputPath(repositoryPath);
		config.addRelativizeRoot(repositoryPath);
		config.addRuleSet(rulesetFile.toString());
		config.setReportFormat("xml");
		config.setReportFile(reportFile);
		config.setIgnoreIncrementalAnalysis(true);

		try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
			pmd.performAnalysis();
		}
	}

	private String titleCase(Severity severity) {
		String name = severity.name().toLowerCase();
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private Path writeRuleset() throws IOException {
		String ruleset = """
				<?xml version="1.0"?>
				<ruleset name="CodeSentinel Complexity"
						xmlns="http://pmd.sourceforge.net/ruleset/2.0.0">
					<description>CodeSentinel complexity rules</description>
					<rule ref="category/java/design.xml/CyclomaticComplexity">
						<properties>
							<property name="methodReportLevel" value="%d"/>
							<property name="classReportLevel" value="%d"/>
						</properties>
					</rule>
				</ruleset>
				"""
				.formatted(
						properties.getCyclomatic().getLowThreshold(),
						properties.getCyclomaticClassReportLevel());

		Path rulesetFile = Files.createTempFile("codesentinel-ruleset-", ".xml");
		Files.writeString(rulesetFile, ruleset, StandardCharsets.UTF_8);
		return rulesetFile;
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
