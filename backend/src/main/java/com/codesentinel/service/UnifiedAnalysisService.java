package com.codesentinel.service;

import com.codesentinel.dto.UnifiedAnalysisResponse;

/**
 * Coordinates the complete repository analysis workflow: clone, scan, run every
 * analysis agent, aggregate and persist findings, and summarize the result under
 * a single analysis record.
 */
public interface UnifiedAnalysisService {

	UnifiedAnalysisResponse analyze(String githubUrl);
}
