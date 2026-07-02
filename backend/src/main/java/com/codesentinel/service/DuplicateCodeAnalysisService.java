package com.codesentinel.service;

import com.codesentinel.dto.DuplicateCodeAnalysisResponse;

public interface DuplicateCodeAnalysisService {

	DuplicateCodeAnalysisResponse analyze(String githubUrl);
}
