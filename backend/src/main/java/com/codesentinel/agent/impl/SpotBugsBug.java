package com.codesentinel.agent.impl;

import lombok.Getter;

/**
 * Raw SpotBugs / FindSecBugs bug instance as read from the XML report, kept
 * separate from our own {@code Finding} model so the underlying tool can be
 * swapped out later (mirrors {@code PmdViolation} / {@code CpdDuplication}).
 */
@Getter
public class SpotBugsBug {

	private final String type;
	private final String category;
	private final int priority;
	private final int rank;
	private final String title;
	private final String description;
	private final String filePath;
	private final Integer lineNumber;

	public SpotBugsBug(
			String type,
			String category,
			int priority,
			int rank,
			String title,
			String description,
			String filePath,
			Integer lineNumber) {
		this.type = type;
		this.category = category;
		this.priority = priority;
		this.rank = rank;
		this.title = title;
		this.description = description;
		this.filePath = filePath;
		this.lineNumber = lineNumber;
	}
}
