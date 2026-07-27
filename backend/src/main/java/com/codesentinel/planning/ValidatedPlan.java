package com.codesentinel.planning;

import com.codesentinel.model.Recommendation;
import java.util.List;

/**
 * A validated, normalized planning result ready to persist. Produced by
 * {@link RecommendationValidator} from the raw model output; the recommendations
 * carry no {@code analysis} association yet (the service sets it before saving).
 */
public record ValidatedPlan(String overallAssessment, List<Recommendation> recommendations) {
}
