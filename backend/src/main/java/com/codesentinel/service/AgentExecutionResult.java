package com.codesentinel.service;

import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import java.util.List;
import lombok.Getter;

/**
 * Outcome of running a single analysis agent inside the unified orchestrator.
 *
 * <p>Keeps the orchestrator resilient: a successful run carries the agent's
 * {@link Finding}s, while a failed run carries the error message instead of
 * propagating the exception, so the remaining agents can still run and their
 * findings can still be persisted.
 */
@Getter
public class AgentExecutionResult {

	private final AgentType agentType;
	private final boolean success;
	private final List<Finding> findings;
	private final String errorMessage;

	private AgentExecutionResult(
			AgentType agentType, boolean success, List<Finding> findings, String errorMessage) {
		this.agentType = agentType;
		this.success = success;
		this.findings = findings;
		this.errorMessage = errorMessage;
	}

	public static AgentExecutionResult completed(AgentType agentType, List<Finding> findings) {
		return new AgentExecutionResult(agentType, true, List.copyOf(findings), null);
	}

	public static AgentExecutionResult failed(AgentType agentType, String errorMessage) {
		return new AgentExecutionResult(agentType, false, List.of(), errorMessage);
	}
}
