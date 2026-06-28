package com.codesentinel.agent.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Raw PMD violation as read from the XML report, kept separate from our own
 * {@code Finding} model so that PMD can be swapped out later.
 */
@Getter
@AllArgsConstructor
public class PmdViolation {

	private final String rule;
	private final int priority;
	private final String filePath;
	private final int beginLine;
	private final String className;
	private final String methodName;
	private final String description;
	private final Integer metricValue;

	public boolean isMethodLevel() {
		return methodName != null && !methodName.isBlank();
	}
}
