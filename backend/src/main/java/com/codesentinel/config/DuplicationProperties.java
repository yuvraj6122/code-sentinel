package com.codesentinel.config;

import com.codesentinel.model.Severity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "codesentinel.duplication")
public class DuplicationProperties {

	/**
	 * Minimum duplicate size, in tokens, before CPD reports a match. This is CPD's
	 * detection knob and is intentionally lower than the line thresholds so that
	 * shorter blocks are surfaced and then graded by {@link #lines}.
	 */
	private int minimumTokens = 50;

	private Thresholds lines = new Thresholds(20, 50, 100);

	/**
	 * Graded line thresholds for a duplicated block. A duplication is classified
	 * against the bands; anything below {@code lowThreshold} is considered
	 * insignificant and not reported.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Thresholds {

		private int lowThreshold;
		private int mediumThreshold;
		private int highThreshold;

		/**
		 * @return the matching severity, or {@code null} when the value is below the
		 *     low threshold (i.e. insignificant and should not produce a finding).
		 */
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
}
