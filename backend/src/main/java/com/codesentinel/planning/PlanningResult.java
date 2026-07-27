package com.codesentinel.planning;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deserialized shape of the OpenAI JSON response. Kept separate from the
 * persisted {@link com.codesentinel.model.Recommendation} model and the API DTOs
 * so the raw model output can be validated before anything is persisted.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanningResult {

	private String overallAssessment;
	private List<PlanningRecommendation> recommendations = new ArrayList<>();
}
