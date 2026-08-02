package com.codesentinel.service;

import com.codesentinel.report.ReportDocument;

/**
 * Builds a consolidated engineering report for a completed analysis from data
 * that already exists — repository metadata, findings, and AI recommendations.
 * It never re-runs analysis or regenerates recommendations.
 */
public interface ReportGeneratorService {

	/** Assembles and renders the Markdown report for an existing analysis. */
	ReportDocument generateMarkdown(Long analysisId);
}
