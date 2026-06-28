package com.codesentinel.dto;

import com.codesentinel.model.Finding;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FindingDto {

	private String agentType;
	private String severity;
	private String title;
	private String description;
	private String filePath;

	public static FindingDto from(Finding finding) {
		return new FindingDto(
				finding.getAgentType().name(),
				finding.getSeverity().name(),
				finding.getTitle(),
				finding.getDescription(),
				finding.getFilePath());
	}
}
