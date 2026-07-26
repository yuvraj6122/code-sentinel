package com.codesentinel.exception;

/** Raised when an operation references an analysis id that does not exist. */
public class AnalysisNotFoundException extends RuntimeException {

	public AnalysisNotFoundException(String message) {
		super(message);
	}
}
