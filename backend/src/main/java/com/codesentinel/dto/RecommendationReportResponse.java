package com.codesentinel.dto;

import com.codesentinel.model.Recommendation;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API response for the Planning Agent: the repository-health assessment plus the
 * prioritized recommendations stored for an analysis.
 */
@Getter
@AllArgsConstructor
public class RecommendationReportResponse {

	private Long analysisId;
	private String overallAssessment;
	private LocalDateTime generatedAt;
	private int totalRecommendations;
	private List<RecommendationDto> recommendations;

	public static RecommendationReportResponse from(
			Long analysisId, String overallAssessment, List<Recommendation> recommendations) {
		LocalDateTime generatedAt = recommendations.stream()
				.map(Recommendation::getCreatedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);

		return new RecommendationReportResponse(
				analysisId,
				overallAssessment,
				generatedAt,
				recommendations.size(),
				recommendations.stream().map(RecommendationDto::from).toList());
	}
}
