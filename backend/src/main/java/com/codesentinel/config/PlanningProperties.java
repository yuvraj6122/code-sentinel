package com.codesentinel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunable knobs for the AI Planning Agent's OpenAI integration. The API key is
 * read from configuration (env-backed) and never hard-coded. Everything else —
 * model, endpoint, timeout, retries, and prompt budgeting — is adjustable here.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "codesentinel.planning")
public class PlanningProperties {

	/** OpenAI API key. Blank disables generation with a clear configuration error. */
	private String apiKey = "";

	/** OpenAI chat-completions model. */
	private String model = "gpt-4o-mini";

	/** OpenAI API base URL (override for proxies/compatible gateways). */
	private String baseUrl = "https://api.openai.com/v1";

	/** Per-request timeout (connect + read) in seconds. */
	private int timeoutSeconds = 60;

	/** Additional retry attempts on transient failures (timeouts, 429, 5xx). */
	private int maxRetries = 2;

	/** Backoff between retries in milliseconds (grows linearly per attempt). */
	private long retryBackoffMillis = 1000;

	/** Sampling temperature; low keeps recommendations focused and stable. */
	private double temperature = 0.2;

	/**
	 * Upper bound on findings included per category in the prompt. Keeps the
	 * prompt concise and bounds token cost; aggregate counts are always included.
	 */
	private int maxFindingsPerCategory = 15;

	/** Upper bound on the number of recommendations requested from the model. */
	private int maxRecommendations = 7;
}
