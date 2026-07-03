package com.codesentinel.agent.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Walks a repository's Java sources with JavaParser and extracts everything the
 * Testing Analysis Agent needs to judge test-suite quality: the test classes and
 * their {@code @Test} methods (with disabled/assertion facts), the imports seen
 * in test files, and the package layout of main vs test sources.
 *
 * <p>Mirrors {@link SourceLengthAnalyzer}: parse-once, translate into a
 * PMD/JavaParser-agnostic model, and keep it separate from the {@code Finding}
 * model so the parsing backend can be swapped later.
 */
@Slf4j
@Component
public class TestSourceInspector {

	private static final Set<String> TEST_METHOD_ANNOTATIONS = Set.of(
			"Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate");
	private static final Set<String> DISABLED_ANNOTATIONS = Set.of("Disabled", "Ignore");
	private static final Set<String> INTEGRATION_ANNOTATIONS = Set.of(
			"SpringBootTest",
			"WebMvcTest",
			"DataJpaTest",
			"JdbcTest",
			"WebFluxTest",
			"RestClientTest",
			"JsonTest",
			"Testcontainers");
	private static final Set<String> ASSERTION_SCOPES = Set.of(
			"Assertions", "Assert", "MatcherAssert", "Assertj", "Mockito");

	public TestSourceScan inspect(Path repositoryPath) {
		JavaParser parser = new JavaParser(
				new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_21));

		List<TestClassInfo> testClasses = new ArrayList<>();
		Set<String> testImports = new LinkedHashSet<>();
		Set<String> mainPackages = new LinkedHashSet<>();
		Set<String> testPackages = new LinkedHashSet<>();

		try (Stream<Path> paths = Files.walk(repositoryPath)) {
			paths.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(".java"))
					.filter(path -> !isUnderGit(path, repositoryPath))
					.forEach(path -> inspectFile(
							parser,
							repositoryPath,
							path,
							testClasses,
							testImports,
							mainPackages,
							testPackages));
		} catch (IOException e) {
			log.warn("Could not walk {} for testing analysis: {}", repositoryPath, e.getMessage());
		}

		log.debug(
				"Testing scan found {} test classes, {} main packages, {} test packages",
				testClasses.size(),
				mainPackages.size(),
				testPackages.size());
		return new TestSourceScan(testClasses, testImports, mainPackages, testPackages);
	}

	private void inspectFile(
			JavaParser parser,
			Path repositoryPath,
			Path file,
			List<TestClassInfo> testClasses,
			Set<String> testImports,
			Set<String> mainPackages,
			Set<String> testPackages) {
		String relativePath = relativePath(repositoryPath, file);
		boolean isTestSource = isTestSource(relativePath);

		ParseResult<CompilationUnit> result;
		try {
			result = parser.parse(file);
		} catch (IOException e) {
			log.debug("Could not read {}: {}", file, e.getMessage());
			return;
		}
		if (!result.isSuccessful() || result.getResult().isEmpty()) {
			log.debug("Skipping unparseable file {}", file);
			return;
		}

		CompilationUnit unit = result.getResult().get();
		String packageName = unit.getPackageDeclaration()
				.map(pkg -> pkg.getNameAsString())
				.orElse("");

		if (isMainSource(relativePath)) {
			mainPackages.add(packageName);
			return;
		}
		if (!isTestSource) {
			return;
		}

		testPackages.add(packageName);
		unit.getImports().forEach(imp -> testImports.add(imp.getNameAsString()));
		boolean fileUsesTestcontainers =
				unit.getImports().stream().anyMatch(imp ->
						imp.getNameAsString().startsWith("org.testcontainers"));

		for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
			if (!type.isTopLevelType() || type.isInterface()) {
				continue;
			}
			testClasses.add(toTestClassInfo(type, packageName, relativePath, fileUsesTestcontainers));
		}
	}

	private TestClassInfo toTestClassInfo(
			ClassOrInterfaceDeclaration type,
			String packageName,
			String relativePath,
			boolean fileUsesTestcontainers) {
		String className = type.getNameAsString();
		boolean disabled = hasAnyAnnotation(type.getAnnotations(), DISABLED_ANNOTATIONS);
		boolean integration = fileUsesTestcontainers
				|| isIntegrationByName(className)
				|| type.findAll(AnnotationExpr.class).stream()
						.anyMatch(annotation ->
								INTEGRATION_ANNOTATIONS.contains(annotation.getNameAsString()));

		List<TestMethodInfo> testMethods = new ArrayList<>();
		for (MethodDeclaration method : type.findAll(MethodDeclaration.class)) {
			if (!isTestMethod(method)) {
				continue;
			}
			boolean methodDisabled =
					hasAnyAnnotation(method.getAnnotations(), DISABLED_ANNOTATIONS);
			TestMethodInfo info = new TestMethodInfo(method.getNameAsString(), methodDisabled);
			if (methodHasAssertions(method)) {
				info.markHasAssertions();
			}
			testMethods.add(info);
		}

		return new TestClassInfo(
				className, packageName, relativePath, integration, disabled, testMethods);
	}

	private boolean isTestMethod(MethodDeclaration method) {
		return method.getAnnotations().stream()
				.anyMatch(annotation ->
						TEST_METHOD_ANNOTATIONS.contains(annotation.getNameAsString()));
	}

	/**
	 * A method "verifies" if it invokes a recognized assertion or verification:
	 * any {@code assert*}/{@code fail} call, a call qualified by a known assertion
	 * type (e.g. {@code Assertions.*}, {@code Assert.*}), or a Mockito
	 * {@code verify(...)}.
	 */
	private boolean methodHasAssertions(MethodDeclaration method) {
		return method.findAll(MethodCallExpr.class).stream().anyMatch(this::isAssertionCall);
	}

	private boolean isAssertionCall(MethodCallExpr call) {
		String name = call.getNameAsString();
		if (name.startsWith("assert") || name.equals("fail") || name.startsWith("verify")) {
			return true;
		}
		return call.getScope()
				.map(scope -> ASSERTION_SCOPES.contains(scope.toString()))
				.orElse(false);
	}

	private boolean isIntegrationByName(String className) {
		return className.endsWith("IT")
				|| className.endsWith("ITCase")
				|| className.contains("IntegrationTest");
	}

	private boolean hasAnyAnnotation(
			List<? extends AnnotationExpr> annotations, Set<String> names) {
		return annotations.stream()
				.anyMatch(annotation -> names.contains(annotation.getNameAsString()));
	}

	private boolean isMainSource(String relativePath) {
		return relativePath.contains("src/main/");
	}

	private boolean isTestSource(String relativePath) {
		if (relativePath.contains("src/test/")) {
			return true;
		}
		String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
		return fileName.endsWith("Test.java")
				|| fileName.endsWith("Tests.java")
				|| fileName.endsWith("IT.java")
				|| fileName.endsWith("ITCase.java");
	}

	private String relativePath(Path repositoryPath, Path file) {
		return repositoryPath.relativize(file).toString().replace('\\', '/');
	}

	private boolean isUnderGit(Path path, Path repositoryRoot) {
		Path relative = repositoryRoot.relativize(path);
		return relative.getNameCount() > 0 && relative.getName(0).toString().equals(".git");
	}
}
