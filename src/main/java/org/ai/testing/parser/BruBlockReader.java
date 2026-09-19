package org.ai.testing.parser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Splits a {@code .bru} document into its named brace-delimited blocks.
 *
 * <p>A Bruno file is a sequence of {@code name { ... }} sections. Nested braces
 * inside a JSON body are tracked by depth so the block boundary is found
 * correctly.</p>
 */
final class BruBlockReader {

    private BruBlockReader() {
    }

    static Map<String, String> read(String source) {
        Map<String, String> blocks = new LinkedHashMap<>();
        if (source == null || source.isBlank()) {
            return blocks;
        }

        int index = 0;
        int length = source.length();

        while (index < length) {
            while (index < length && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
            if (index >= length) {
                break;
            }

            int nameStart = index;
            while (index < length && source.charAt(index) != '{'
                    && source.charAt(index) != '\n' && source.charAt(index) != '\r') {
                index++;
            }

            String name = source.substring(nameStart, index).trim();

            if (index >= length || source.charAt(index) != '{') {
                // A line that never reached an opening brace is not a block.
                index++;
                continue;
            }

            int bodyStart = index + 1;
            int depth = 1;
            index++;
            while (index < length && depth > 0) {
                char current = source.charAt(index);
                if (current == '{') {
                    depth++;
                } else if (current == '}') {
                    depth--;
                }
                index++;
            }

            if (!name.isBlank()) {
                blocks.put(name, source.substring(bodyStart, Math.max(bodyStart, index - 1)));
            }
        }
        return blocks;
    }
}
