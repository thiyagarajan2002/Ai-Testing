package org.ai.testing.TestData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private String content;

    public Config(String filePath) throws IOException {
        this.content = Files.readString(Path.of(filePath));
    }

    public String getContent() {
        return content;
    }
}
