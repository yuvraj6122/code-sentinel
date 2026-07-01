package com.codesentinel.agent.impl;

import com.codesentinel.exception.DuplicationAnalysisException;
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
 * Parses a PMD CPD XML report into a flat list of {@link CpdDuplication} records.
 */
@Slf4j
@Component
public class CpdReportParser {

	public List<CpdDuplication> parse(Path reportFile) {
		List<CpdDuplication> duplications = new ArrayList<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setExpandEntityReferences(false);

			Document document = factory.newDocumentBuilder().parse(reportFile.toFile());
			document.getDocumentElement().normalize();

			NodeList duplicationNodes = document.getElementsByTagName("duplication");
			for (int i = 0; i < duplicationNodes.getLength(); i++) {
				Node node = duplicationNodes.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element duplication = (Element) node;
				duplications.add(new CpdDuplication(
						parseInt(duplication.getAttribute("lines"), 0),
						parseInt(duplication.getAttribute("tokens"), 0),
						collectMarks(duplication)));
			}
		} catch (Exception e) {
			throw new DuplicationAnalysisException(
					"Failed to parse CPD report: " + e.getMessage(), e);
		}

		log.debug("Parsed {} duplications from {}", duplications.size(), reportFile);
		return duplications;
	}

	private List<CpdMark> collectMarks(Element duplication) {
		List<CpdMark> marks = new ArrayList<>();
		NodeList fileNodes = duplication.getElementsByTagName("file");
		for (int j = 0; j < fileNodes.getLength(); j++) {
			Node node = fileNodes.item(j);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element file = (Element) node;
			marks.add(new CpdMark(
					file.getAttribute("path"),
					parseInt(file.getAttribute("line"), 0),
					parseInt(file.getAttribute("endline"), 0)));
		}
		return marks;
	}

	private int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
