package com.codesentinel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codesentinel.config.TestingProperties.Thresholds;
import com.codesentinel.model.Severity;
import org.junit.jupiter.api.Test;

class TestingPropertiesTest {

	private final Thresholds disabledTests = new TestingProperties().getDisabledTests();

	@Test
	void ignoresZeroDisabledTests() {
		assertNull(disabledTests.classify(0));
	}

	@Test
	void mapsOneToFiveDisabledTestsToLow() {
		assertEquals(Severity.LOW, disabledTests.classify(1));
		assertEquals(Severity.LOW, disabledTests.classify(5));
	}

	@Test
	void mapsSixToTenDisabledTestsToMedium() {
		assertEquals(Severity.MEDIUM, disabledTests.classify(6));
		assertEquals(Severity.MEDIUM, disabledTests.classify(10));
	}

	@Test
	void mapsElevenPlusDisabledTestsToHigh() {
		assertEquals(Severity.HIGH, disabledTests.classify(11));
		assertEquals(Severity.HIGH, disabledTests.classify(100));
	}
}
