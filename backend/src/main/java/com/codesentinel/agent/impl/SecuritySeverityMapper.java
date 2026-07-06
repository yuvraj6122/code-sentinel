package com.codesentinel.agent.impl;

import com.codesentinel.model.Severity;
import org.springframework.stereotype.Component;

/**
 * Centralized translation of SpotBugs / FindSecBugs bug priorities into
 * CodeSentinel {@link Severity} values, so severity conversion lives in exactly
 * one place rather than being scattered across the parsing/finding code.
 *
 * <p>SpotBugs encodes priority as a number where lower is more severe:
 * {@code 1 = High}, {@code 2 = Medium}, {@code 3 = Low} (4/5 are
 * experimental/ignore). The mapping is therefore:
 *
 * <pre>
 *   SpotBugs High (1)   → HIGH
 *   SpotBugs Medium (2) → MEDIUM
 *   SpotBugs Low (3+)   → LOW
 * </pre>
 */
@Component
public class SecuritySeverityMapper {

	public static final int PRIORITY_HIGH = 1;
	public static final int PRIORITY_MEDIUM = 2;

	public Severity fromPriority(int priority) {
		if (priority <= PRIORITY_HIGH) {
			return Severity.HIGH;
		}
		if (priority == PRIORITY_MEDIUM) {
			return Severity.MEDIUM;
		}
		return Severity.LOW;
	}
}
