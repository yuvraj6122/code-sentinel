package com.codesentinel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloneRepositoryRequest {

	@NotBlank
	private String githubUrl;
}