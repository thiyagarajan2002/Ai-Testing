package org.ai.testing.parser;

import org.ai.testing.env.EnvironmentDto;
import org.ai.testing.testrun.dto.TestRunDto;
import org.ai.testing.util.Strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Picks the right parser for a collection and layers an environment on top.
 *
 * <p>Format detection is by shape rather than by flag: a directory or a
 * {@code .bru} file is Bruno, anything else is treated as Postman JSON.</p>
 */
public class CollectionLoader {

    private final PostmanParser postmanParser = new PostmanParser();
    private final BrunoParser brunoParser = new BrunoParser();
    private final PostmanEnvironmentParser postmanEnvironmentParser =
            new PostmanEnvironmentParser();

    public TestRunDto load(Path collectionPath, Path environmentPath) {

        if (collectionPath == null) {
            throw new IllegalArgumentException("Collection path cannot be null");
        }
        if (!Files.exists(collectionPath)) {
            throw new CollectionParseException("Collection not found: " + collectionPath);
        }

        TestRunDto run = isBruno(collectionPath)
                ? brunoParser.parse(collectionPath)
                : postmanParser.parse(collectionPath);

        if (environmentPath != null) {
            if (!Files.exists(environmentPath)) {
                throw new CollectionParseException("Environment not found: " + environmentPath);
            }
            EnvironmentDto environment = loadEnvironment(environmentPath);
            run.setEnvironment(environment.getName());
            run.setEnvironmentVariables(environment.getValues());
        } else if (Strings.isBlank(run.getEnvironment())) {
            run.setEnvironment("default");
        }
        return run;
    }

    public EnvironmentDto loadEnvironment(Path environmentPath) {
        if (environmentPath == null) {
            throw new IllegalArgumentException("Environment path cannot be null");
        }
        return fileName(environmentPath).endsWith(".bru")
                ? brunoParser.parseEnvironment(environmentPath)
                : postmanEnvironmentParser.parse(environmentPath);
    }

    private boolean isBruno(Path path) {
        return Files.isDirectory(path) || fileName(path).endsWith(".bru");
    }

    private String fileName(Path path) {
        Path name = path.getFileName();
        return name == null ? "" : name.toString().toLowerCase(Locale.ROOT);
    }
}
