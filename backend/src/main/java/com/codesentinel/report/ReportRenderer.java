package com.codesentinel.report;

/**
 * Renders an assembled {@link ReportModel} into a concrete output format. New
 * formats (e.g. PDF) implement this interface without changing report assembly.
 */
public interface ReportRenderer {

	String render(ReportModel model);
}
