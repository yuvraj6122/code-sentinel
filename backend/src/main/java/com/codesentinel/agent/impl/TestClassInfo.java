package com.codesentinel.agent.impl;

import java.util.List;
import lombok.Getter;

/**
 * A parsed test class, kept separate from the {@code Finding} model. Aggregates
 * the per-class facts the Testing Analysis Agent grades: its test methods,
 * whether it is disabled/integration, and its package/path for organization and
 * finding attribution.
 */
@Getter
public class TestClassInfo {

	private final String className;
	private final String packageName;
	private final String filePath;
	private final boolean integrationTest;
	private final boolean disabled;
	private final List<TestMethodInfo> testMethods;

	public TestClassInfo(
			String className,
			String packageName,
			String filePath,
			boolean integrationTest,
			boolean disabled,
			List<TestMethodInfo> testMethods) {
		this.className = className;
		this.packageName = packageName;
		this.filePath = filePath;
		this.integrationTest = integrationTest;
		this.disabled = disabled;
		this.testMethods = testMethods;
	}

	/** A class with no {@code @Test} methods contributes nothing to the suite. */
	public boolean isEmpty() {
		return testMethods.isEmpty();
	}
}
