package com.codesentinel.agent.impl;

import lombok.Getter;

/**
 * A single {@code @Test} method discovered in a test class, kept separate from
 * the {@code Finding} model. Captures only what the testing heuristics need.
 */
@Getter
public class TestMethodInfo {

	private final String name;
	private final boolean disabled;
	private boolean hasAssertions;

	public TestMethodInfo(String name, boolean disabled) {
		this.name = name;
		this.disabled = disabled;
	}

	void markHasAssertions() {
		this.hasAssertions = true;
	}
}
