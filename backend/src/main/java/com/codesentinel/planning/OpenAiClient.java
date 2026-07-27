package com.codesentinel.planning;

/**
 * Boundary to the OpenAI chat-completions API. Isolating it behind an interface
 * keeps prompt construction and validation out of the HTTP layer and lets tests
 * mock the model without making live calls.
 */
public interface OpenAiClient {

	/**
	 * Sends the system + user prompts and returns the assistant message content
	 * (expected to be a JSON string). Handles auth, timeouts, and retries.
	 *
	 * @throws com.codesentinel.exception.PlanningAgentException on configuration
	 *     errors, exhausted retries, or a non-recoverable API error.
	 */
	String complete(String systemPrompt, String userPrompt);
}
