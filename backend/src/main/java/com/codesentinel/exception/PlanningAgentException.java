package com.codesentinel.exception;

/**
 * Raised when recommendation generation fails — OpenAI errors, timeouts,
 * exhausted retries, missing configuration, or a malformed/invalid model
 * response. Never thrown from the analysis pipeline, so it cannot crash an
 * analysis.
 */
public class PlanningAgentException extends RuntimeException {

	public PlanningAgentException(String message) {
		super(message);
	}

	public PlanningAgentException(String message, Throwable cause) {
		super(message, cause);
	}
}
