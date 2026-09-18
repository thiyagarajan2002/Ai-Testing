package org.ai.testing.parser;

import java.util.LinkedHashMap;
import java.util.Map;

final class BruBlockReader {

    private BruBlockReader() {
    }

    static Map<String, String> read(String source) {
        Map<String, String> blocks = new LinkedHashMap<>();
        if (source == null || source.isBlank()) {
            return blocks;
        }
        int index = 0;
        char[] chars = source.toCharArray();
        while (index < chars.length) {
            while (index < chars.length && Character.isWhitespace(chars[index])) {
                index++;
            }
            if (index >= chars.length) {
                break;
            }
            int nameStart = index;
            while (index < chars.length
                    && chars[index] != '{'
                    && chars[index] != '\n'
                    && chars[index] != '\r') {
                index++;
            }
            String name = source.substring(nameStart, index).trim();
            while (index < chars.length && chars[index] != '{') {
                if (chars[index] == '\n') {
                    name = "";
                    break;
                }
                index++;
            }
            if (index >= chars.length || chars[index] != '{') {
                index++;
                continue;
            }
            int bodyStart = index + 1;
            int depth = 1;
            index++;
            while (index < chars.length && depth > 0) {
                char current = chars[index];
                if (current == '{') {
                    depth++;
                } else if (current == '}') {
                    depth--;
                }
                index++;
            }
            if (!name.isBlank()) {
                int bodyEnd = Math.max(bodyStart, index - 1);
                blocks.put(name, source.substring(bodyStart, bodyEnd));
            }
        }
        return blocks;
    }
}
