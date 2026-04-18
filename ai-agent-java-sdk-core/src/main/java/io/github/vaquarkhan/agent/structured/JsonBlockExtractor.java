package io.github.vaquarkhan.agent.structured;

import java.util.Objects;

/**
 * Extracts the first JSON object/array block from a text response.
 *
 * <p>Many model responses contain leading/trailing prose or markdown fences. This extractor
 * attempts to locate and return the first syntactically balanced JSON block.
 *
 * @author Vaquar Khan
 */
final class JsonBlockExtractor {

    private JsonBlockExtractor() {}

    static String extractFirstJsonBlock(String text) {
        Objects.requireNonNull(text, "text");

        String cleaned = stripMarkdownCodeFences(text).trim();
        int start = indexOfJsonStart(cleaned);
        if (start < 0) {
            throw new StructuredOutputParseException("No JSON object/array found in model output.");
        }

        int end = findMatchingJsonEnd(cleaned, start);
        if (end < 0) {
            throw new StructuredOutputParseException("Unterminated JSON object/array in model output.");
        }

        return cleaned.substring(start, end + 1).trim();
    }

    private static String stripMarkdownCodeFences(String text) {
        // Remove the first surrounding ```...``` fence if present.
        String t = text.trim();
        if (!t.startsWith("```")) {
            return text;
        }
        int firstNewline = t.indexOf('\n');
        if (firstNewline < 0) {
            return text;
        }
        int fenceEnd = t.lastIndexOf("```");
        if (fenceEnd <= firstNewline) {
            return text;
        }
        return t.substring(firstNewline + 1, fenceEnd);
    }

    private static int indexOfJsonStart(String text) {
        int obj = text.indexOf('{');
        int arr = text.indexOf('[');
        if (obj < 0) {
            return arr;
        }
        if (arr < 0) {
            return obj;
        }
        return Math.min(obj, arr);
    }

    private static int findMatchingJsonEnd(String text, int start) {
        char open = text.charAt(start);
        char close = open == '{' ? '}' : ']';

        int depth = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaping = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}

