package com.codesentinel.agent.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A non-empty-line count for a single method or class, produced by parsing the
 * source with JavaParser. Kept separate from our {@code Finding} model.
 */
@Getter
@AllArgsConstructor
public class LengthMeasurement {

	public enum Kind {
		METHOD,
		CLASS
	}

	private final Kind kind;
	private final String filePath;
	private final String name;
	private final int nonEmptyLines;
}
