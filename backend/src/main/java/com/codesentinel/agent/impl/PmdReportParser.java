package com.codesentinel.agent.impl;

import com.codesentinel.exception.ComplexityAnalysisException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses a PMD XML report into a flat list of {@link PmdViolation} records.
 */
@Slf4j
@Component
public class PmdReportParser {

	private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)");

	public List<PmdViolation> parse(Path reportFile) {
		List<PmdViolation> violations = new ArrayList<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setExpandEntityReferences(false);

			Document document = factory.newDocumentBuilder().parse(reportFile.toFile());
			document.getDocumentElement().normalize();

			NodeList fileNodes = document.getElementsByTagName("file");
			for (int i = 0; i < fileNodes.getLength(); i++) {
				Element fileElement = (Element) fileNodes.item(i);
				String filePath = fileElement.getAttribute("name");
				collectViolations(fileElement, filePath, violations);
			}
		} catch (Exception e) {
			throw new ComplexityAnalysisException("Failed to parse PMD report: " + e.getMessage(), e);
		}

		log.debug("Parsed {} violations from {}", violations.size(), reportFile);
		return violations;
	}

	private void collectViolations(
			Element fileElement, String filePath, List<PmdViolation> violations) {
		NodeList children = fileElement.getElementsByTagName("violation");
		for (int j = 0; j < children.getLength(); j++) {
			Node node = children.item(j);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element violation = (Element) node;
			String message = violation.getTextContent().trim();

			violations.add(new PmdViolation(
					violation.getAttribute("rule"),
					parseInt(violation.getAttribute("priority"), 5),
					filePath,
					parseInt(violation.getAttribute("beginline"), 0),
					emptyToNull(violation.getAttribute("class")),
					emptyToNull(violation.getAttribute("method")),
					message,
					extractMetricValue(message)));
		}
	}

	private Integer extractMetricValue(String message) {
		Matcher matcher = TRAILING_NUMBER.matcher(message);
		Integer last = null;
		while (matcher.find()) {
			last = Integer.parseInt(matcher.group(1));
		}
		return last;
	}

	private int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private String emptyToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
