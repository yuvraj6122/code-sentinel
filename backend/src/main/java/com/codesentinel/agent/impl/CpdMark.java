package com.codesentinel.agent.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A single occurrence of a duplicated block within one file, as read from the
 * CPD XML report.
 */
@Getter
@AllArgsConstructor
public class CpdMark {

	private final String filePath;
	private final int startLine;
	private final int endLine;

	public String getFileName() {
		if (filePath == null || filePath.isBlank()) {
			return "unknown";
		}
		String normalized = filePath.replace('\\', '/');
		int slash = normalized.lastIndexOf('/');
		return slash >= 0 ? normalized.substring(slash + 1) : normalized;
	}
}
