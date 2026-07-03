package com.codesentinel.agent.impl;

import java.util.List;
import lombok.Getter;

/**
 * The testing and mocking frameworks detected in a repository, as human-readable
 * names (e.g. {@code "JUnit 5"}, {@code "Mockito"}). Kept separate from the
 * {@code Finding} model.
 */
@Getter
public class FrameworkDetection {

	private final List<String> testingFrameworks;
	private final List<String> mockingFrameworks;

	public FrameworkDetection(List<String> testingFrameworks, List<String> mockingFrameworks) {
		this.testingFrameworks = testingFrameworks;
		this.mockingFrameworks = mockingFrameworks;
	}

	public boolean hasTestingFramework() {
		return !testingFrameworks.isEmpty();
	}

	public boolean hasMockingFramework() {
		return !mockingFrameworks.isEmpty();
	}
}
