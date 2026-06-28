package com.codesentinel.service;

import com.codesentinel.dto.ComplexityAnalysisResponse;

public interface ComplexityAnalysisService {

	ComplexityAnalysisResponse analyze(String githubUrl);
}
