package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceLengthAnalyzerTest {

	@Test
	void countsOnlyNonEmptyLines(@TempDir Path repo) throws Exception {
		String source =
				"""
				public class Foo {

					public void bar() {
						int a = 0;

						a++;
					}
				}
				""";
		Files.writeString(repo.resolve("Foo.java"), source);

		List<LengthMeasurement> measurements = new SourceLengthAnalyzer().analyze(repo);

		LengthMeasurement bar = measurements.stream()
				.filter(m -> m.getKind() == LengthMeasurement.Kind.METHOD)
				.filter(m -> m.getName().equals("bar"))
				.findFirst()
				.orElseThrow();

		// bar spans: signature, `int a = 0;`, blank, `a++;`, `}` -> 4 non-empty lines.
		assertEquals(4, bar.getNonEmptyLines());

		assertTrue(
				measurements.stream()
						.anyMatch(m ->
								m.getKind() == LengthMeasurement.Kind.CLASS
										&& m.getName().equals("Foo")),
				"expected a class measurement for Foo");
	}
}
