package com.codesentinel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codesentinel.config.DuplicationProperties.Thresholds;
import com.codesentinel.model.Severity;
import org.junit.jupiter.api.Test;

class DuplicationPropertiesTest {

	private final Thresholds thresholds = new DuplicationProperties().getLines();

	@Test
	void ignoresBlocksBelowTwentyLines() {
		assertNull(thresholds.classify(0));
		assertNull(thresholds.classify(19));
	}

	@Test
	void mapsTwentyToFortyNineLinesToLow() {
		assertEquals(Severity.LOW, thresholds.classify(20));
		assertEquals(Severity.LOW, thresholds.classify(49));
	}

	@Test
	void mapsFiftyToNinetyNineLinesToMedium() {
		assertEquals(Severity.MEDIUM, thresholds.classify(50));
		assertEquals(Severity.MEDIUM, thresholds.classify(99));
	}

	@Test
	void mapsHundredPlusLinesToHigh() {
		assertEquals(Severity.HIGH, thresholds.classify(100));
		assertEquals(Severity.HIGH, thresholds.classify(500));
	}
}
