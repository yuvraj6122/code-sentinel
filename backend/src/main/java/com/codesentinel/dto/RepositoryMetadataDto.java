package com.codesentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryMetadataDto {

	private String repositoryName;
	private String language;
	private String buildTool;
	private Integer javaFileCount;
	private Integer testFileCount;
}
