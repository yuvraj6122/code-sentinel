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
@ConfigurationProperties(prefix = "codesentinel.complexity")
public class ComplexityProperties {

	private Thresholds cyclomatic = new Thresholds(11, 16, 21);
	private int cyclomaticClassReportLevel = 9999;
	private Thresholds methodLength = new Thresholds(50, 100, 150);
	private Thresholds classLength = new Thresholds(300, 500, 800);

	/**
	 * Graded thresholds for a metric. A measured value is classified against the
	 * bands; anything below {@code lowThreshold} is considered OK and not reported.
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
		 *     low threshold (i.e. OK and should not produce a finding).
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
