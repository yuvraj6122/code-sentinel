package com.codesentinel.repository;

import com.codesentinel.model.Recommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

	/** Stored recommendations for an analysis, highest priority (1) first. */
	List<Recommendation> findByAnalysisIdOrderByPriorityAsc(Long analysisId);

	boolean existsByAnalysisId(Long analysisId);
}
