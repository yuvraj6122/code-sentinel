package com.codesentinel.controller;

import com.codesentinel.dto.CloneRepositoryResponse;
import com.codesentinel.exception.InvalidRepositoryUrlException;
import com.codesentinel.exception.RepositoryCloneException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidRepositoryUrlException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public CloneRepositoryResponse handleInvalidUrl(InvalidRepositoryUrlException ex) {
		return CloneRepositoryResponse.failure(ex.getMessage());
	}

	@ExceptionHandler(RepositoryCloneException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public CloneRepositoryResponse handleCloneFailure(RepositoryCloneException ex) {
		return CloneRepositoryResponse.failure(ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public CloneRepositoryResponse handleValidationFailure(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Invalid request");
		return CloneRepositoryResponse.failure(message);
	}
}
