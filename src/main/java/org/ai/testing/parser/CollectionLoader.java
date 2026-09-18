package org.ai.testing.parser;

import org.ai.testing.env.EnvironmentDto;
import org.ai.testing.testrun.dto.TestRunDto;

import java.nio.file.Files;
import java.nio.file.Path;

public class CollectionLoader {

    private final PostmanParser postmanParser = new PostmanParser();
    private final BrunoParser brunoParser = new BrunoParser();
    private final PostmanEnvironmentParser postmanEnvironmentParser = new PostmanEnvironmentParser();

    public TestRunDto load(Path collectionPath, Path environmentPath) {
        if (collectionPath == null) {
            throw new IllegalArgumentException("Collection path cannot be null");
        }

        TestRunDto run;
        String fileName = collectionPath.getFileName().toString().toLowerCase();
        if (Files.isDirectory(collectionPath) || fileName.endsWith(".bru")) {
            run = brunoParser.parse(collectionPath);
        } else {
            run = postmanParser.parse(collectionPath);
        }

        if (environmentPath != null) {
            EnvironmentDto environment = loadEnvironment(environmentPath);
            run.setEnvironment(environment.getName());
            run.setEnvironmentVariables(environment.getValues());
        } else if (run.getEnvironment() == null) {
            run.setEnvironment("default");
        }
        return run;
    }

    public EnvironmentDto loadEnvironment(Path environmentPath) {
        String fileName = environmentPath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".bru")) {
            return brunoParser.parseEnvironment(environmentPath);
        }
        return postmanEnvironmentParser.parse(environmentPath);
    }
}
