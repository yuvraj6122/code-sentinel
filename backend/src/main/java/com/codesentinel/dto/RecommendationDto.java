package com.codesentinel.dto;

import com.codesentinel.model.AgentType;
import com.codesentinel.model.Recommendation;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API view of a single persisted {@link Recommendation}. Enums are exposed as
 * their names, mirroring the finding-DTO convention.
 */
@Getter
@AllArgsConstructor
public class RecommendationDto {

	private int priority;
	private String title;
	private String description;
	private String reason;
	private String impact;
	private List<String> affectedCategories;

	public static RecommendationDto from(Recommendation recommendation) {
		return new RecommendationDto(
				recommendation.getPriority(),
				recommendation.getTitle(),
				recommendation.getDescription(),
				recommendation.getReason(),
				recommendation.getImpact().name(),
				recommendation.getAffectedCategories().stream().map(AgentType::name).toList());
	}
}
