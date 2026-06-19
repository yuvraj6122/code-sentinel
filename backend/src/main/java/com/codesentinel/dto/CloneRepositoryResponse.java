package com.codesentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CloneRepositoryResponse {

	private String status;
	private String localPath;
	private String message;

	public static CloneRepositoryResponse success(String localPath) {
		return new CloneRepositoryResponse("SUCCESS", localPath, null);
	}

	public static CloneRepositoryResponse failure(String message) {
		return new CloneRepositoryResponse("FAILED", null, message);
	}
}