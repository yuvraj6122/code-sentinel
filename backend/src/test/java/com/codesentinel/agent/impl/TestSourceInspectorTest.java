package com.codesentinel.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestSourceInspectorTest {

	private final TestSourceInspector inspector = new TestSourceInspector();

	@Test
	void extractsTestMethodsWithAssertionAndDisabledFacts(@TempDir Path repo) throws Exception {
		Path testDir = repo.resolve("src/test/java/com/example");
		Files.createDirectories(testDir);
		Files.writeString(testDir.resolve("SampleTest.java"), """
				package com.example;

				import org.junit.jupiter.api.Test;
				import org.junit.jupiter.api.Disabled;
				import static org.junit.jupiter.api.Assertions.assertEquals;

				class SampleTest {

					@Test
					void shouldComputeSum() {
						assertEquals(2, 1 + 1);
					}

					@Test
					void test1() {
						int result = 1 + 1;
					}

					@Test
					@Disabled
					void ignoredForNow() {
						assertEquals(2, 1 + 1);
					}
				}
				""");

		TestSourceScan scan = inspector.inspect(repo);

		assertEquals(1, scan.getTestClasses().size());
		TestClassInfo sample = scan.getTestClasses().get(0);
		assertEquals(3, sample.getTestMethods().size());
		assertFalse(sample.isIntegrationTest());

		TestMethodInfo withAssertion = method(sample, "shouldComputeSum");
		assertTrue(withAssertion.isHasAssertions());
		assertFalse(withAssertion.isDisabled());

		TestMethodInfo withoutAssertion = method(sample, "test1");
		assertFalse(withoutAssertion.isHasAssertions());

		TestMethodInfo disabled = method(sample, "ignoredForNow");
		assertTrue(disabled.isDisabled());
	}

	@Test
	void flagsSpringBootTestAsIntegration(@TempDir Path repo) throws Exception {
		Path testDir = repo.resolve("src/test/java/com/example");
		Files.createDirectories(testDir);
		Files.writeString(testDir.resolve("AppIntegrationTest.java"), """
				package com.example;

				import org.junit.jupiter.api.Test;
				import org.springframework.boot.test.context.SpringBootTest;
				import static org.junit.jupiter.api.Assertions.assertTrue;

				@SpringBootTest
				class AppIntegrationTest {

					@Test
					void contextLoads() {
						assertTrue(true);
					}
				}
				""");

		TestSourceScan scan = inspector.inspect(repo);

		assertEquals(1, scan.getTestClasses().size());
		assertTrue(scan.getTestClasses().get(0).isIntegrationTest());
	}

	@Test
	void collectsMainAndTestPackages(@TempDir Path repo) throws Exception {
		Path mainDir = repo.resolve("src/main/java/com/example/service");
		Path testDir = repo.resolve("src/test/java/com/example/service");
		Files.createDirectories(mainDir);
		Files.createDirectories(testDir);
		Files.writeString(mainDir.resolve("Widget.java"), """
				package com.example.service;

				public class Widget {}
				""");
		Files.writeString(testDir.resolve("WidgetTest.java"), """
				package com.example.service;

				import org.junit.jupiter.api.Test;

				class WidgetTest {
					@Test
					void placeholder() {}
				}
				""");

		TestSourceScan scan = inspector.inspect(repo);

		assertTrue(scan.getMainPackages().contains("com.example.service"));
		assertTrue(scan.getTestPackages().contains("com.example.service"));
	}

	private TestMethodInfo method(TestClassInfo testClass, String name) {
		return testClass.getTestMethods().stream()
				.filter(method -> method.getName().equals(name))
				.findFirst()
				.orElseThrow();
	}
}
