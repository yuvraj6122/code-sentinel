package com.codesentinel.controller;

import com.codesentinel.dto.CloneRepositoryResponse;
import com.codesentinel.exception.ComplexityAnalysisException;
import com.codesentinel.exception.DuplicationAnalysisException;
import com.codesentinel.exception.InvalidRepositoryUrlException;
import com.codesentinel.exception.RepositoryCloneException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidRepositoryUrlException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public CloneRepositoryResponse handleInvalidUrl(InvalidRepositoryUrlException ex) {
		log.warn("Bad request — {}", ex.getMessage());
		return CloneRepositoryResponse.failure(ex.getMessage());
	}

	@ExceptionHandler(RepositoryCloneException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public CloneRepositoryResponse handleCloneFailure(RepositoryCloneException ex) {
		log.error("Clone failed — {}", ex.getMessage());
		return CloneRepositoryResponse.failure(ex.getMessage());
	}

	@ExceptionHandler(ComplexityAnalysisException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public CloneRepositoryResponse handleComplexityFailure(ComplexityAnalysisException ex) {
		log.error("Complexity analysis failed — {}", ex.getMessage());
		return CloneRepositoryResponse.failure(ex.getMessage());
	}

	@ExceptionHandler(DuplicationAnalysisException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public CloneRepositoryResponse handleDuplicationFailure(DuplicationAnalysisException ex) {
		log.error("Duplicate code analysis failed — {}", ex.getMessage());
		return CloneRepositoryResponse.failure(ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public CloneRepositoryResponse handleValidationFailure(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Invalid request");
		log.warn("Validation failed — {}", message);
		return CloneRepositoryResponse.failure(message);
	}
}
