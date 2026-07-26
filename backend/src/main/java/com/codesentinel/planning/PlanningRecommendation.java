package com.codesentinel.planning;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single recommendation as returned by OpenAI, prior to validation. */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanningRecommendation {

	private int priority;
	private String title;
	private String description;
	private String reason;
	private String impact;
	private List<String> affectedCategories = new ArrayList<>();
}
