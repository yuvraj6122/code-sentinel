package com.codesentinel.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An AI-generated engineering recommendation produced by the Planning Agent for a
 * single {@link Analysis}. Unlike a {@link Finding} (an objective fact reported by
 * an analysis tool), a recommendation is interpretive: it prioritizes and explains
 * what to do about the collected findings.
 */
@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
public class Recommendation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "analysis_id", nullable = false)
	private Analysis analysis;

	/** 1-based priority; 1 is the most important action to take first. */
	@Column(nullable = false)
	private int priority;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RecommendationImpact impact;

	/** Analysis categories this recommendation addresses (e.g. SECURITY, TESTING). */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "recommendation_categories",
			joinColumns = @JoinColumn(name = "recommendation_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false)
	private List<AgentType> affectedCategories = new ArrayList<>();

	@Column(nullable = false)
	private LocalDateTime createdAt;
}
