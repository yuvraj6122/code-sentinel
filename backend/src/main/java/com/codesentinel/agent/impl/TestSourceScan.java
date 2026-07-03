package com.codesentinel.agent.impl;

import java.util.List;
import java.util.Set;
import lombok.Getter;

/**
 * The result of inspecting a repository's sources for test analysis: the parsed
 * test classes, the imports seen across test files (used as a fallback signal
 * for framework detection), and the package layout of {@code src/main/java} and
 * {@code src/test/java} (used to judge test organization).
 */
@Getter
public class TestSourceScan {

	private final List<TestClassInfo> testClasses;
	private final Set<String> testImports;
	private final Set<String> mainPackages;
	private final Set<String> testPackages;

	public TestSourceScan(
			List<TestClassInfo> testClasses,
			Set<String> testImports,
			Set<String> mainPackages,
			Set<String> testPackages) {
		this.testClasses = testClasses;
		this.testImports = testImports;
		this.mainPackages = mainPackages;
		this.testPackages = testPackages;
	}

	public boolean hasTests() {
		return !testClasses.isEmpty();
	}
}
