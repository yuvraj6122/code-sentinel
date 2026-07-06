package com.codesentinel.config;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunable knobs for the Security Analysis Agent. SpotBugs/FindSecBugs do the
 * detection; this only controls how the repository is built, which SpotBugs
 * priorities/categories are surfaced as security findings, and the analysis
 * budget. Nothing here is hard-coded in the agent.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "codesentinel.security")
public class SecurityProperties {

	/** Attempt to compile the repository (Maven/Gradle) so SpotBugs has bytecode. */
	private boolean buildEnabled = true;

	/** Hard cap on the best-effort build; the build is abandoned past this. */
	private int buildTimeoutSeconds = 240;

	/**
	 * SpotBugs priority threshold (1=high, 2=medium, 3=low). Bugs with a numeric
	 * priority greater than this are ignored. 3 keeps HIGH/MEDIUM/LOW.
	 */
	private int minPriority = 3;

	/**
	 * SpotBugs bug categories always treated as security findings — covers all
	 * FindSecBugs detections plus SpotBugs' own malicious-code checks.
	 */
	private Set<String> includedCategories = Set.of("SECURITY", "MALICIOUS_CODE");

	/**
	 * Additional non-SECURITY bug types surfaced because they carry security
	 * weight: null dereferences, unclosed streams, and other resource leaks.
	 */
	private List<String> includedTypePrefixes =
			List.of("NP_", "OS_OPEN_STREAM", "OBL_", "ODR_", "RCN_");
}
