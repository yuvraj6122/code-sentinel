package com.codesentinel.model;

/**
 * Estimated engineering impact of acting on a recommendation. Distinct from
 * {@link Severity}: severity grades an individual finding's risk, whereas impact
 * expresses how much value the Planning Agent expects a recommendation to deliver.
 */
public enum RecommendationImpact {
	LOW,
	MEDIUM,
	HIGH
}
