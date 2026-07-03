package com.codesentinel.agent;

import java.nio.file.Path;

public interface TestingAnalysisAgent {

	TestingAnalysisResult analyze(Path repositoryPath);
}
