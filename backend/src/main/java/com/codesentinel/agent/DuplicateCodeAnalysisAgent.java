package com.codesentinel.agent;

import com.codesentinel.model.Finding;
import java.nio.file.Path;
import java.util.List;

public interface DuplicateCodeAnalysisAgent {

	List<Finding> analyze(Path repositoryPath);
}
