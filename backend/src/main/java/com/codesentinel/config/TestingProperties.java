package com.codesentinel.config;

import com.codesentinel.model.Severity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunable knobs for the Testing Analysis Agent. Nothing about the testing
 * quality heuristics is hard-coded in the agent; the disabled-test severity
 * bands and the maturity-score penalties all live here.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "codesentinel.testing")
public class TestingProperties {

	/**
	 * Disabled/ignored test count → severity: 1-5 LOW, 6-10 MEDIUM, 11+ HIGH.
	 * A count of 0 classifies to {@code null} (no finding).
	 */
	private Thresholds disabledTests = new Thresholds(1, 6, 11);

	/** Maturity score deductions, all expressed on a 0-100 scale. */
	private Maturity maturity = new Maturity();

	/**
	 * Graded count thresholds. A measured count is classified against the bands;
	 * anything below {@code lowThreshold} classifies to {@code null} (no finding).
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Thresholds {

		private int lowThreshold;
		private int mediumThreshold;
		private int highThreshold;

		public Severity classify(int value) {
			if (value >= highThreshold) {
				return Severity.HIGH;
			}
			if (value >= mediumThreshold) {
				return Severity.MEDIUM;
			}
			if (value >= lowThreshold) {
				return Severity.LOW;
			}
			return null;
		}
	}

	/**
	 * Point deductions applied to a perfect score of 100. Per-item penalties are
	 * capped so a single category cannot dominate the whole score.
	 */
	@Getter
	@Setter
	public static class Maturity {

		private int noFrameworkPenalty = 40;
		private int noMockingPenalty = 8;
		private int noIntegrationPenalty = 15;
		private int disabledTestPenaltyPerTest = 3;
		private int disabledTestPenaltyCap = 20;
		private int noAssertionPenaltyPerTest = 4;
		private int noAssertionPenaltyCap = 20;
		private int emptyClassPenaltyPerClass = 3;
		private int emptyClassPenaltyCap = 12;
		private int poorNamingPenaltyPerMethod = 1;
		private int poorNamingPenaltyCap = 10;
		private int poorOrganizationPenalty = 8;
	}
}
