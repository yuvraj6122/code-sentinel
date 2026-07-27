package com.codesentinel.planning;

import com.codesentinel.exception.PlanningAgentException;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Recommendation;
import com.codesentinel.model.RecommendationImpact;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Validates and normalizes the raw model output before anything is persisted.
 *
 * <p>The contract is strict: a structurally invalid response (missing assessment,
 * a recommendation missing a required field, or an unparseable impact) is rejected
 * wholesale with a {@link PlanningAgentException} so no partial/invalid data is
 * stored. Recoverable noise is normalized instead — unknown categories are dropped
 * and a non-positive priority falls back to positional order. An empty
 * recommendation list is a valid outcome (assessment with no actions).
 */
@Component
public class RecommendationValidator {

	public ValidatedPlan validate(PlanningResult result) {
		if (result == null) {
			throw new PlanningAgentException("OpenAI returned an empty response");
		}

		String assessment = trimToNull(result.getOverallAssessment());
		if (assessment == null) {
			throw new PlanningAgentException(
					"OpenAI response is missing the required 'overallAssessment' field");
		}

		List<Recommendation> recommendations = new ArrayList<>();
		List<PlanningRecommendation> raw =
				result.getRecommendations() == null ? List.of() : result.getRecommendations();
		int position = 1;
		for (PlanningRecommendation candidate : raw) {
			recommendations.add(toRecommendation(candidate, position));
			position++;
		}

		return new ValidatedPlan(assessment, recommendations);
	}

	private Recommendation toRecommendation(PlanningRecommendation candidate, int position) {
		if (candidate == null) {
			throw new PlanningAgentException("OpenAI response contains a null recommendation");
		}

		String title = requireField(candidate.getTitle(), "title");
		String description = requireField(candidate.getDescription(), "description");
		String reason = requireField(candidate.getReason(), "reason");

		Recommendation recommendation = new Recommendation();
		recommendation.setPriority(candidate.getPriority() > 0 ? candidate.getPriority() : position);
		recommendation.setTitle(title);
		recommendation.setDescription(description);
		recommendation.setReason(reason);
		recommendation.setImpact(parseImpact(candidate.getImpact()));
		recommendation.setAffectedCategories(parseCategories(candidate.getAffectedCategories()));
		recommendation.setCreatedAt(LocalDateTime.now());
		return recommendation;
	}

	private String requireField(String value, String field) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new PlanningAgentException(
					"OpenAI recommendation is missing the required '" + field + "' field");
		}
		return trimmed;
	}

	private RecommendationImpact parseImpact(String impact) {
		String value = trimToNull(impact);
		if (value == null) {
			throw new PlanningAgentException("OpenAI recommendation is missing the required 'impact' field");
		}
		try {
			return RecommendationImpact.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new PlanningAgentException("OpenAI recommendation has an invalid impact: " + impact);
		}
	}

	/** Maps category names to {@link AgentType}, silently dropping unknown values. */
	private List<AgentType> parseCategories(List<String> categories) {
		List<AgentType> parsed = new ArrayList<>();
		if (categories == null) {
			return parsed;
		}
		for (String category : categories) {
			String value = trimToNull(category);
			if (value == null) {
				continue;
			}
			try {
				parsed.add(AgentType.valueOf(value.toUpperCase()));
			} catch (IllegalArgumentException ignored) {
				// Ignore categories the model invented that don't map to a known agent.
			}
		}
		return parsed;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
