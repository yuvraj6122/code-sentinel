package com.codesentinel.report;

/**
 * A rendered report: its suggested download file name and its content. Keeping
 * the file name with the content lets the controller offer a download without
 * re-deriving it.
 */
public record ReportDocument(String fileName, String content) {
}
