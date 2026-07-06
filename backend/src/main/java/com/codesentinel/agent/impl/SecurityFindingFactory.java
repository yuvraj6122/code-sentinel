package com.codesentinel.agent.impl;

import com.codesentinel.config.SecurityProperties;
import com.codesentinel.model.AgentType;
import com.codesentinel.model.Finding;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Converts raw {@link SpotBugsBug}s into the shared {@link Finding} model,
 * keeping only security-relevant bugs. A bug qualifies when its category is one
 * of the configured security categories (covering all FindSecBugs detections)
 * or its type matches a configured prefix (null dereferences, resource leaks,
 * …). Severity comes from the centralized {@link SecuritySeverityMapper}.
 */
@Slf4j
@Component
public class SecurityFindingFactory {

	private final SecurityProperties properties;
	private final SecuritySeverityMapper severityMapper;

	public SecurityFindingFactory(
			SecurityProperties properties, SecuritySeverityMapper severityMapper) {
		this.properties = properties;
		this.severityMapper = severityMapper;
	}

	public Optional<Finding> toFinding(SpotBugsBug bug) {
		if (bug.getPriority() > properties.getMinPriority() || !isSecurityRelevant(bug)) {
			return Optional.empty();
		}

		Finding finding = new Finding();
		finding.setAgentType(AgentType.SECURITY);
		finding.setSeverity(severityMapper.fromPriority(bug.getPriority()));
		finding.setTitle(bug.getTitle());
		finding.setDescription(bug.getDescription());
		finding.setFilePath(bug.getFilePath());
		finding.setLineNumber(bug.getLineNumber());
		return Optional.of(finding);
	}

	private boolean isSecurityRelevant(SpotBugsBug bug) {
		String category = bug.getCategory();
		if (category != null && properties.getIncludedCategories().contains(category)) {
			return true;
		}
		String type = bug.getType();
		if (type == null) {
			return false;
		}
		return properties.getIncludedTypePrefixes().stream().anyMatch(type::startsWith);
	}
}
