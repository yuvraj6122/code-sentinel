package com.codesentinel.service;

import com.codesentinel.dto.TestingAnalysisResponse;

public interface TestingAnalysisService {

	TestingAnalysisResponse analyze(String githubUrl);
}
