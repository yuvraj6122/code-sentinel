package com.codesentinel.dto;

import com.codesentinel.service.AgentExecutionResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Per-agent execution status inside a unified analysis run. Lets clients see
 * which agents completed, how many findings each contributed, and why any agent
 * failed — without failing the overall analysis.
 */
@Getter
@AllArgsConstructor
public class AgentExecutionDto {

	private String agent;
	private String status;
	private int findings;
	private String message;

	public static AgentExecutionDto from(AgentExecutionResult result) {
		return new AgentExecutionDto(
				result.getAgentType().name(),
				result.isSuccess() ? "COMPLETED" : "FAILED",
				result.getFindings().size(),
				result.getErrorMessage());
	}
}
