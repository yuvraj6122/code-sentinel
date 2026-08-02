package com.codesentinel.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codesentinel.exception.AnalysisNotFoundException;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Analysis;
import com.codesentinel.model.AnalysisStatus;
import com.codesentinel.model.Finding;
import com.codesentinel.model.Recommendation;
import com.codesentinel.model.RepositoryEntity;
import com.codesentinel.model.Severity;
import com.codesentinel.report.ReportDocument;
import com.codesentinel.report.ReportModel;
import com.codesentinel.report.ReportRenderer;
import com.codesentinel.repository.AnalysisRepository;
import com.codesentinel.repository.RecommendationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportGeneratorServiceImplTest {

	@Mock private AnalysisRepository analysisRepository;
	@Mock private RecommendationRepository recommendationRepository;
	@Mock private ReportRenderer reportRenderer;

	@InjectMocks private ReportGeneratorServiceImpl service;

	@Test
	void assemblesModelFromPersistedDataAndDelegatesToRenderer() {
		Analysis analysis = analysis(42L);
		when(analysisRepository.findById(42L)).thenReturn(Optional.of(analysis));
		when(recommendationRepository.findByAnalysisIdOrderByPriorityAsc(42L))
				.thenReturn(List.of(recommendation()));
		when(reportRenderer.render(any())).thenReturn("# rendered");

		ReportDocument document = service.generateMarkdown(42L);

		ArgumentCaptor<ReportModel> captor = ArgumentCaptor.forClass(ReportModel.class);
		org.mockito.Mockito.verify(reportRenderer).render(captor.capture());
		ReportModel model = captor.getValue();

		assertThat(model.analysisId()).isEqualTo(42L);
		assertThat(model.repositoryName()).isEqualTo("demo-repo");
		assertThat(model.repositoryUrl()).isEqualTo("https://github.com/acme/demo-repo");
		assertThat(model.status()).isEqualTo("COMPLETED");
		assertThat(model.totalFindings()).isEqualTo(2);
		assertThat(model.severity().high()).isEqualTo(1);
		assertThat(model.severity().medium()).isEqualTo(1);
		assertThat(model.healthLabel()).isEqualTo("At risk — high-severity issues present");
		// Findings are split into the correct category buckets.
		assertThat(model.category(AgentType.SECURITY).total()).isEqualTo(1);
		assertThat(model.category(AgentType.COMPLEXITY).total()).isEqualTo(1);
		assertThat(model.category(AgentType.TESTING).total()).isZero();
		assertThat(model.recommendations()).hasSize(1);

		assertThat(document.content()).isEqualTo("# rendered");
		assertThat(document.fileName()).isEqualTo("demo-repo-analysis-42-report.md");
	}

	@Test
	void throwsWhenAnalysisDoesNotExist() {
		when(analysisRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generateMarkdown(99L))
				.isInstanceOf(AnalysisNotFoundException.class)
				.hasMessageContaining("99");

		verifyNoInteractions(recommendationRepository, reportRenderer);
	}

	private Analysis analysis(Long id) {
		RepositoryEntity repository = new RepositoryEntity();
		repository.setName("demo-repo");
		repository.setGithubUrl("https://github.com/acme/demo-repo");
		repository.setLanguage("Java");
		repository.setBuildTool("Gradle");

		Analysis analysis = new Analysis();
		analysis.setId(id);
		analysis.setRepository(repository);
		analysis.setStatus(AnalysisStatus.COMPLETED);
		analysis.setStartedAt(LocalDateTime.of(2026, 1, 2, 11, 0));
		analysis.setCompletedAt(LocalDateTime.of(2026, 1, 2, 11, 30));
		analysis.setOverallAssessment("Healthy overall.");

		List<Finding> findings = new ArrayList<>();
		findings.add(finding(AgentType.SECURITY, Severity.HIGH, "SQL injection"));
		findings.add(finding(AgentType.COMPLEXITY, Severity.MEDIUM, "High complexity"));
		analysis.setFindings(findings);
		return analysis;
	}

	private Finding finding(AgentType type, Severity severity, String title) {
		Finding finding = new Finding();
		finding.setAgentType(type);
		finding.setSeverity(severity);
		finding.setTitle(title);
		return finding;
	}

	private Recommendation recommendation() {
		Recommendation recommendation = new Recommendation();
		recommendation.setPriority(1);
		recommendation.setTitle("Fix it");
		recommendation.setDescription("desc");
		recommendation.setReason("reason");
		recommendation.setImpact(com.codesentinel.model.RecommendationImpact.HIGH);
		recommendation.setCreatedAt(LocalDateTime.now());
		return recommendation;
	}
}
