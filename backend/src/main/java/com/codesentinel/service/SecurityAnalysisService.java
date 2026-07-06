package com.codesentinel.service;

import com.codesentinel.dto.SecurityAnalysisResponse;

public interface SecurityAnalysisService {

	SecurityAnalysisResponse analyze(String githubUrl);
}
