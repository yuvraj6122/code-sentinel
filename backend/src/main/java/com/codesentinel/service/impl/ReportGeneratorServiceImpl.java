package com.codesentinel.service.impl;

import com.codesentinel.exception.AnalysisNotFoundException;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Analysis;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Recommendation;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.report.ReportDocument;
import com.codesentinel.report.ReportModel;
import com.codesentinel.report.ReportModel.CategoryReport;
import com.codesentinel.report.ReportRenderer;
import com.codesentinel.report.SeverityBreakdown;
import com.codesentinel.repository.AnalysisRepository;
import com.codesentinel.repository.RecommendationRepository;
import com.codesentinel.service.ReportGeneratorService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles a {@link ReportModel} from an analysis's persisted data and renders
 * it. Read-only: it loads the analysis, its findings, and its recommendations,
 * and triggers neither repository analysis nor recommendation generation.
 */
@Slf4j
@Service
public class ReportGeneratorServiceImpl implements ReportGeneratorService {

	/** Category sections in report order (sections 4–7). */
	private static final List<AgentType> CATEGORIES =
			List.of(AgentType.COMPLEXITY, AgentType.SECURITY, AgentType.TESTING, AgentType.DUPLICATE_CODE);

	private final AnalysisRepository analysisRepository;
	private final RecommendationRepository recommendationRepository;
	private final ReportRenderer reportRenderer;

	public ReportGeneratorServiceImpl(
			AnalysisRepository analysisRepository,
			RecommendationRepository recommendationRepository,
			ReportRenderer reportRenderer) {
		this.analysisRepository = analysisRepository;
		this.recommendationRepository = recommendationRepository;
		this.reportRenderer = reportRenderer;
	}

	@Override
	@Transactional(readOnly = true)
	public ReportDocument generateMarkdown(Long analysisId) {
		log.info("Generating engineering report for analysis #{}", analysisId);

		Analysis analysis = analysisRepository
				.findById(analysisId)
				.orElseThrow(
						() -> new AnalysisNotFoundException("No analysis found with id " + analysisId));

		List<Finding> findings = analysis.getFindings();
		List<Recommendation> recommendations =
				recommendationRepository.findByAnalysisIdOrderByPriorityAsc(analysisId);

		ReportModel model = assemble(analysis, findings, recommendations);
		String content = reportRenderer.render(model);

		log.info(
				"Engineering report for analysis #{} assembled — {} findings, {} recommendations",
				analysisId,
				findings.size(),
				recommendations.size());
		return new ReportDocument(fileName(analysis, model), content);
	}

	private ReportModel assemble(
			Analysis analysis, List<Finding> findings, List<Recommendation> recommendations) {
		RepositoryEntity repository = analysis.getRepository();
		SeverityBreakdown overall = SeverityBreakdown.of(findings);

		List<CategoryReport> categories = CATEGORIES.stream()
				.map(category -> {
					List<Finding> categoryFindings = findings.stream()
							.filter(finding -> finding.getAgentType() == category)
							.toList();
					return new CategoryReport(
							category, SeverityBreakdown.of(categoryFindings), categoryFindings);
				})
				.toList();

		return new ReportModel(
				repository.getName(),
				repository.getGithubUrl(),
				repository.getLanguage(),
				repository.getBuildTool(),
				analysis.getId(),
				analysis.getStatus() != null ? analysis.getStatus().name() : "UNKNOWN",
				analysisDate(analysis),
				analysis.getOverallAssessment(),
				healthLabel(overall),
				overall,
				categories,
				recommendations);
	}

	private LocalDateTime analysisDate(Analysis analysis) {
		return analysis.getCompletedAt() != null ? analysis.getCompletedAt() : analysis.getStartedAt();
	}

	/** Simple, deterministic health grade from the overall severity mix. */
	private String healthLabel(SeverityBreakdown severity) {
		if (severity.critical() > 0) {
			return "Critical — immediate attention required";
		}
		if (severity.high() > 0) {
			return "At risk — high-severity issues present";
		}
		if (severity.medium() > 0) {
			return "Fair — some issues to address";
		}
		if (severity.low() > 0) {
			return "Good — only minor issues";
		}
		return "Excellent — no issues detected";
	}

	private String fileName(Analysis analysis, ReportModel model) {
		String base = model.repositoryName() == null || model.repositoryName().isBlank()
				? "repository"
				: model.repositoryName();
		String safe = base.replaceAll("[^a-zA-Z0-9-_]", "-");
		return "%s-analysis-%d-report.md".formatted(safe, analysis.getId());
	}
}
