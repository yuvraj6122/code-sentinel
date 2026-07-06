package com.codesentinel.agent.impl;

import com.codesentinel.exception.SecurityAnalysisException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses a SpotBugs XML report (the same format FindSecBugs findings are written
 * into, since FindSecBugs is a SpotBugs plugin) into a flat list of
 * {@link SpotBugsBug} records. Mirrors {@link PmdReportParser} — a DOM parse into
 * a raw holder, decoupled from the {@code Finding} model.
 */
@Slf4j
@Component
public class SpotBugsReportParser {

	public List<SpotBugsBug> parse(Path reportFile) {
		List<SpotBugsBug> bugs = new ArrayList<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setExpandEntityReferences(false);

			Document document = factory.newDocumentBuilder().parse(reportFile.toFile());
			document.getDocumentElement().normalize();

			NodeList bugNodes = document.getElementsByTagName("BugInstance");
			for (int i = 0; i < bugNodes.getLength(); i++) {
				Node node = bugNodes.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				bugs.add(toBug((Element) node));
			}
		} catch (Exception e) {
			throw new SecurityAnalysisException(
					"Failed to parse SpotBugs report: " + e.getMessage(), e);
		}

		log.debug("Parsed {} bug instances from {}", bugs.size(), reportFile);
		return bugs;
	}

	private SpotBugsBug toBug(Element bug) {
		String type = bug.getAttribute("type");
		String category = bug.getAttribute("category");
		int priority = parseInt(bug.getAttribute("priority"), 3);
		int rank = parseInt(bug.getAttribute("rank"), 0);

		String shortMessage = firstChildText(bug, "ShortMessage");
		String longMessage = firstChildText(bug, "LongMessage");
		String title = shortMessage != null ? shortMessage : humanize(type);
		String description = longMessage != null
				? longMessage
				: (shortMessage != null ? shortMessage : humanize(type));

		Element sourceLine = primarySourceLine(bug);
		String filePath = sourceLine == null ? null : sourcePath(sourceLine);
		Integer lineNumber = sourceLine == null ? null : startLine(sourceLine);

		return new SpotBugsBug(
				type, category, priority, rank, title, description, filePath, lineNumber);
	}

	/**
	 * Picks the most relevant source line for a bug: the one flagged
	 * {@code primary="true"} if present, otherwise the first that resolves to a
	 * source path.
	 */
	private Element primarySourceLine(Element bug) {
		NodeList sourceLines = bug.getElementsByTagName("SourceLine");
		Element firstWithPath = null;
		for (int i = 0; i < sourceLines.getLength(); i++) {
			Element sourceLine = (Element) sourceLines.item(i);
			if (Boolean.parseBoolean(sourceLine.getAttribute("primary"))) {
				return sourceLine;
			}
			if (firstWithPath == null && !sourcePath(sourceLine).isBlank()) {
				firstWithPath = sourceLine;
			}
		}
		return firstWithPath;
	}

	private String sourcePath(Element sourceLine) {
		String sourcePath = sourceLine.getAttribute("sourcepath");
		return sourcePath.isBlank() ? sourceLine.getAttribute("sourcefile") : sourcePath;
	}

	private Integer startLine(Element sourceLine) {
		int start = parseInt(sourceLine.getAttribute("start"), -1);
		return start > 0 ? start : null;
	}

	private String firstChildText(Element element, String tag) {
		NodeList nodes = element.getElementsByTagName(tag);
		if (nodes.getLength() == 0) {
			return null;
		}
		String text = nodes.item(0).getTextContent();
		return text == null || text.isBlank() ? null : text.trim();
	}

	/** Turns a bug type like {@code PREDICTABLE_RANDOM} into {@code Predictable Random}. */
	private String humanize(String type) {
		if (type == null || type.isBlank()) {
			return "Security Issue";
		}
		String[] words = type.toLowerCase().split("_");
		StringBuilder builder = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			builder.append(Character.toUpperCase(word.charAt(0)))
					.append(word.substring(1))
					.append(' ');
		}
		return builder.toString().trim();
	}

	private int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
