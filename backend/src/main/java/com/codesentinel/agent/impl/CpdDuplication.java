package com.codesentinel.agent.impl;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Raw CPD duplication as read from the XML report, kept separate from our own
 * {@code Finding} model so that PMD CPD can be swapped out later.
 */
@Getter
@AllArgsConstructor
public class CpdDuplication {

	private final int lines;
	private final int tokens;
	private final List<CpdMark> marks;
}
