package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codesentinel.model.Severity;
import org.junit.jupiter.api.Test;

class SecuritySeverityMapperTest {

	private final SecuritySeverityMapper mapper = new SecuritySeverityMapper();

	@Test
	void mapsSpotBugsHighPriorityToHigh() {
		assertEquals(Severity.HIGH, mapper.fromPriority(1));
	}

	@Test
	void mapsSpotBugsMediumPriorityToMedium() {
		assertEquals(Severity.MEDIUM, mapper.fromPriority(2));
	}

	@Test
	void mapsSpotBugsLowPriorityToLow() {
		assertEquals(Severity.LOW, mapper.fromPriority(3));
	}

	@Test
	void mapsExperimentalAndIgnorePrioritiesToLow() {
		assertEquals(Severity.LOW, mapper.fromPriority(4));
		assertEquals(Severity.LOW, mapper.fromPriority(5));
	}
}
