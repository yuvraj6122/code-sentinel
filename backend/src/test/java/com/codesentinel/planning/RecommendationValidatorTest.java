package com.codesentinel.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesentinel.exception.PlanningAgentException;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Recommendation;
import com.codesentinel.model.RecommendationImpact;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationValidatorTest {

	private final RecommendationValidator validator = new RecommendationValidator();

	@Test
	void normalizesValidResult() {
		PlanningResult result = result("  Repo is healthy.  ",
				recommendation(0, "Fix SQLi", "Parameterize queries", "Injection sink found",
						"high", List.of("SECURITY", "NOT_A_CATEGORY")));

		ValidatedPlan plan = validator.validate(result);

		assertEquals("Repo is healthy.", plan.overallAssessment());
		assertEquals(1, plan.recommendations().size());
		Recommendation recommendation = plan.recommendations().get(0);
		// Non-positive priority falls back to positional order.
		assertEquals(1, recommendation.getPriority());
		// Impact is normalized case-insensitively.
		assertEquals(RecommendationImpact.HIGH, recommendation.getImpact());
		// Unknown categories are dropped, known ones kept.
		assertEquals(List.of(AgentType.SECURITY), recommendation.getAffectedCategories());
	}

	@Test
	void allowsEmptyRecommendations() {
		ValidatedPlan plan = validator.validate(result("Nothing actionable.", (PlanningRecommendation[]) null));

		assertEquals("Nothing actionable.", plan.overallAssessment());
		assertTrue(plan.recommendations().isEmpty());
	}

	@Test
	void rejectsNullResult() {
		assertThrows(PlanningAgentException.class, () -> validator.validate(null));
	}

	@Test
	void rejectsBlankAssessment() {
		assertThrows(PlanningAgentException.class, () -> validator.validate(result("   ")));
	}

	@Test
	void rejectsInvalidImpact() {
		PlanningResult result = result("assessment",
				recommendation(1, "Title", "Description", "Reason", "CATASTROPHIC", List.of()));

		assertThrows(PlanningAgentException.class, () -> validator.validate(result));
	}

	@Test
	void rejectsMissingTitle() {
		PlanningResult result = result("assessment",
				recommendation(1, "  ", "Description", "Reason", "LOW", List.of()));

		assertThrows(PlanningAgentException.class, () -> validator.validate(result));
	}

	private PlanningResult result(String assessment, PlanningRecommendation... recommendations) {
		PlanningResult result = new PlanningResult();
		result.setOverallAssessment(assessment);
		if (recommendations != null) {
			result.setRecommendations(new ArrayList<>(List.of(recommendations)));
		}
		return result;
	}

	private PlanningRecommendation recommendation(
			int priority,
			String title,
			String description,
			String reason,
			String impact,
			List<String> categories) {
		PlanningRecommendation recommendation = new PlanningRecommendation();
		recommendation.setPriority(priority);
		recommendation.setTitle(title);
		recommendation.setDescription(description);
		recommendation.setReason(reason);
		recommendation.setImpact(impact);
		recommendation.setAffectedCategories(new ArrayList<>(categories));
		return recommendation;
	}
}
