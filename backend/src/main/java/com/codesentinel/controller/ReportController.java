package com.codesentinel.controller;

import com.codesentinel.report.ReportDocument;
import com.codesentinel.service.ReportGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the consolidated engineering report for a completed analysis as
 * Markdown. Read-only: it renders already-persisted data and never re-runs an
 * analysis or regenerates recommendations. Pass {@code ?download=true} to get
 * the report as a downloadable {@code .md} attachment.
 */
@Slf4j
@RestController
public class ReportController {

	private static final MediaType MARKDOWN = MediaType.parseMediaType("text/markdown; charset=UTF-8");

	private final ReportGeneratorService reportGeneratorService;

	public ReportController(ReportGeneratorService reportGeneratorService) {
		this.reportGeneratorService = reportGeneratorService;
	}

	@GetMapping("/api/analyses/{analysisId}/report")
	public ResponseEntity<String> report(
			@PathVariable Long analysisId,
			@RequestParam(name = "download", defaultValue = "false") boolean download) {
		log.info("Engineering report requested for analysis #{} (download={})", analysisId, download);
		ReportDocument document = reportGeneratorService.generateMarkdown(analysisId);

		ResponseEntity.BodyBuilder response = ResponseEntity.ok().contentType(MARKDOWN);
		if (download) {
			response.header(
					HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=\"" + document.fileName() + "\"");
		}
		return response.body(document.content());
	}
}
