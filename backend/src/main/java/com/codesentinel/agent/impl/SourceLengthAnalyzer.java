package com.codesentinel.agent.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Measures method and class length as the number of non-empty source lines,
 * using JavaParser. PMD 7 has no line-based length rule, so this fills that gap.
 */
@Slf4j
@Component
public class SourceLengthAnalyzer {

	public List<LengthMeasurement> analyze(Path repositoryPath) {
		List<LengthMeasurement> measurements = new ArrayList<>();
		JavaParser parser = new JavaParser(
				new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_21));

		try (Stream<Path> paths = Files.walk(repositoryPath)) {
			paths.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(".java"))
					.filter(path -> !isUnderGit(path, repositoryPath))
					.forEach(path -> measureFile(parser, repositoryPath, path, measurements));
		} catch (IOException e) {
			log.warn("Could not walk {} for length analysis: {}", repositoryPath, e.getMessage());
		}

		log.debug("Collected {} length measurements", measurements.size());
		return measurements;
	}

	private void measureFile(
			JavaParser parser,
			Path repositoryPath,
			Path file,
			List<LengthMeasurement> measurements) {
		try {
			List<String> lines = Files.readAllLines(file);
			ParseResult<CompilationUnit> result = parser.parse(file);
			if (!result.isSuccessful() || result.getResult().isEmpty()) {
				log.debug("Skipping unparseable file {}", file);
				return;
			}

			String relativePath = relativePath(repositoryPath, file);
			CompilationUnit unit = result.getResult().get();

			for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
				addMeasurement(measurements, LengthMeasurement.Kind.CLASS, relativePath, type, lines);
			}
			for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
				addMeasurement(
						measurements, LengthMeasurement.Kind.METHOD, relativePath, method, lines);
			}
			for (ConstructorDeclaration ctor : unit.findAll(ConstructorDeclaration.class)) {
				addMeasurement(
						measurements, LengthMeasurement.Kind.METHOD, relativePath, ctor, lines);
			}
		} catch (IOException e) {
			log.debug("Could not read {}: {}", file, e.getMessage());
		}
	}

	private <T extends Node & NodeWithSimpleName<?>> void addMeasurement(
			List<LengthMeasurement> measurements,
			LengthMeasurement.Kind kind,
			String relativePath,
			T node,
			List<String> lines) {
		node.getRange().ifPresent(range -> {
			int count = countNonEmptyLines(lines, range.begin.line, range.end.line);
			measurements.add(
					new LengthMeasurement(kind, relativePath, node.getNameAsString(), count));
		});
	}

	private int countNonEmptyLines(List<String> lines, int beginLine, int endLine) {
		int count = 0;
		for (int line = beginLine; line <= endLine && line <= lines.size(); line++) {
			if (!lines.get(line - 1).strip().isEmpty()) {
				count++;
			}
		}
		return count;
	}

	private String relativePath(Path repositoryPath, Path file) {
		return repositoryPath.relativize(file).toString().replace('\\', '/');
	}

	private boolean isUnderGit(Path path, Path repositoryRoot) {
		Path relative = repositoryRoot.relativize(path);
		return relative.getNameCount() > 0 && relative.getName(0).toString().equals(".git");
	}
}
