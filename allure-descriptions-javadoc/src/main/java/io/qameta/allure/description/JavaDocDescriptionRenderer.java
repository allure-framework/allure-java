/*
 *  Copyright 2016-2026 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.description;

import java.util.Locale;

/**
 * Renders raw JavaDoc comment text into a safe, markdown-friendly description for Allure.
 *
 * <p>This renderer intentionally implements a small, conservative subset of the JavaDoc comment
 * specification instead of attempting to preserve the full doclet output. The goal is to keep
 * JavaDoc-backed descriptions readable in reports while ensuring that untrusted comment content is
 * never treated as executable HTML.</p>
 *
 * <p>The rendering algorithm is intentionally simple:</p>
 * <ol>
 *     <li>Take only the main description, stopping at the first block tag such as {@code @param}
 *     or {@code @throws}.</li>
 *     <li>Render the remaining content with a small parser that recognizes a limited set of inline
 *     JavaDoc tags and structural HTML tags.</li>
 *     <li>Escape or drop everything else so the output remains plain text or safe markdown.</li>
 * </ol>
 *
 * <p>Currently supported JavaDoc constructs include inline tags such as {@code {@code ...}},
 * {@code {@literal ...}}, {@code {@link ...}}, and {@code {@linkplain ...}}.</p>
 *
 * <p>The renderer also understands a small set of structural HTML tags: {@code p}, {@code br},
 * {@code ul}, {@code ol}, {@code li}, and {@code code}. Common entity references such as
 * {@code &amp;lt;}, {@code &amp;gt;}, {@code &amp;amp;}, {@code &amp;#064;},
 * {@code &amp;lbrace;}, and numeric entities are decoded before the output is escaped again.</p>
 *
 * <p>Unsupported tags are degraded to escaped text instead of being interpreted. Unknown HTML tags
 * are ignored as markup while their text content remains visible. This keeps the JavaDoc
 * description path suitable for open source projects where comments may evolve over time and where
 * security is more important than pixel-perfect parity with the standard doclet.</p>
 */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
final class JavaDocDescriptionRenderer {

    private static final String PARAGRAPH_BREAK = "\n\n";
    private static final String HTML_LT = "&lt;";
    private static final String HTML_GT = "&gt;";
    private static final String HTML_AMP = "&amp;";
    private static final String INLINE_CODE_MARKER = "`";
    private static final String ESCAPED_INLINE_CODE_MARKER = "``";
    private static final String CODE_TAG = "code";
    private static final String HTML_TAG_END = ">";
    private static final String CLOSING_TAG_PREFIX = "</";
    private static final String NEW_LINE = "\n";

    /**
     * Converts raw JavaDoc comment text into the description format stored by the annotation
     * processor.
     *
     * <p>The method extracts the JavaDoc main description, renders the supported inline and HTML
     * constructs into plain text or markdown, normalizes whitespace, and escapes unsafe content.
     * The returned value is intended for Allure's plain {@code description} field, not for
     * {@code descriptionHtml}.</p>
     *
     * @param rawDocComment the comment text returned by {@link javax.lang.model.util.Elements#getDocComment}
     * @return a safe markdown/plain-text description, or an empty string if the comment has no main
     * description
     */
    String render(final String rawDocComment) {
        final String descriptionBody = extractDescriptionBody(rawDocComment);
        if (descriptionBody.isEmpty()) {
            return "";
        }

        final StringBuilder rendered = new StringBuilder();
        renderFragment(descriptionBody, rendered);
        return cleanup(rendered.toString());
    }

    private String extractDescriptionBody(final String rawDocComment) {
        final String[] lines = normalize(rawDocComment).split(NEW_LINE, -1);
        final StringBuilder body = new StringBuilder();
        int inlineTagDepth = 0;

        for (String line : lines) {
            if (inlineTagDepth == 0 && startsBlockTag(line)) {
                return trimBlankLines(body.toString());
            }
            if (body.length() > 0) {
                body.append('\n');
            }
            body.append(trimTrailingWhitespace(line));
            inlineTagDepth = updateInlineTagDepth(line, inlineTagDepth);
        }

        return trimBlankLines(body.toString());
    }

    private boolean startsBlockTag(final String line) {
        final String trimmed = line.trim();
        return trimmed.length() > 1 && trimmed.charAt(0) == '@' && Character.isJavaIdentifierStart(trimmed.charAt(1));
    }

    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private void renderFragment(final String fragment, final StringBuilder rendered) {
        int index = 0;
        while (index < fragment.length()) {
            final char current = fragment.charAt(index);
            if (current == '{' && index + 1 < fragment.length() && fragment.charAt(index + 1) == '@') {
                final int nextIndex = renderInlineTag(fragment, index, rendered);
                if (nextIndex > index) {
                    index = nextIndex;
                    continue;
                }
            }
            if (current == '<') {
                final int nextIndex = renderHtmlTag(fragment, index, rendered);
                if (nextIndex > index) {
                    index = nextIndex;
                    continue;
                }
                rendered.append(HTML_LT);
                index++;
                continue;
            }
            if (current == '&') {
                final int nextIndex = renderEntityReference(fragment, index, rendered);
                if (nextIndex > index) {
                    index = nextIndex;
                    continue;
                }
                rendered.append(HTML_AMP);
                index++;
                continue;
            }
            if (current == '>') {
                rendered.append(HTML_GT);
                index++;
                continue;
            }
            rendered.append(current);
            index++;
        }
    }

    @SuppressWarnings("checkstyle:ReturnCount")
    private int renderInlineTag(final String fragment, final int start, final StringBuilder rendered) {
        final int end = findInlineTagEnd(fragment, start);
        if (end < 0) {
            return start;
        }

        final String content = fragment.substring(start + 2, end).trim();
        if (content.isEmpty()) {
            return end + 1;
        }

        final int separator = findWhitespace(content);
        final String tag = separator < 0 ? content : content.substring(0, separator);
        final String payload = separator < 0 ? "" : content.substring(separator + 1).trim();

        if (CODE_TAG.equals(tag)) {
            appendCode(rendered, payload);
            return end + 1;
        }
        if ("literal".equals(tag)) {
            rendered.append(escapeText(payload));
            return end + 1;
        }
        if ("link".equals(tag) || "linkplain".equals(tag)) {
            appendLink(rendered, payload);
            return end + 1;
        }

        rendered.append(escapeText(content));
        return end + 1;
    }

    @SuppressWarnings(
        {
                "checkstyle:CyclomaticComplexity",
                "checkstyle:NPathComplexity",
                "PMD.CognitiveComplexity",
                "checkstyle:ReturnCount"}
    )
    private int renderHtmlTag(final String fragment, final int start, final StringBuilder rendered) {
        if (start + 1 >= fragment.length() || Character.isWhitespace(fragment.charAt(start + 1))) {
            return start;
        }

        final int end = fragment.indexOf('>', start + 1);
        if (end < 0) {
            return start;
        }

        final String rawTag = fragment.substring(start + 1, end).trim();
        if (rawTag.isEmpty()) {
            return start;
        }

        boolean closing = false;
        String tag = rawTag;
        if (tag.charAt(0) == '/') {
            closing = true;
            tag = tag.substring(1).trim();
        }

        if (tag.endsWith("/")) {
            tag = tag.substring(0, tag.length() - 1).trim();
        }

        final int separator = findTagNameEnd(tag);
        if (separator <= 0) {
            return end + 1;
        }

        final String name = tag.substring(0, separator).toLowerCase(Locale.ROOT);
        if ("br".equals(name)) {
            appendLineBreak(rendered);
            return end + 1;
        }
        if ("p".equals(name) || "ul".equals(name) || "ol".equals(name)) {
            appendParagraphBreak(rendered);
            return end + 1;
        }
        if ("li".equals(name)) {
            if (!closing) {
                startListItem(rendered);
            }
            return end + 1;
        }
        if (CODE_TAG.equals(name)) {
            if (closing) {
                return end + 1;
            }
            final int closingStart = findClosingTag(fragment, end + 1, CODE_TAG);
            if (closingStart <= end) {
                return end + 1;
            }
            appendCode(rendered, fragment.substring(end + 1, closingStart));
            return closingStart + (CLOSING_TAG_PREFIX + CODE_TAG + HTML_TAG_END).length();
        }

        return end + 1;
    }

    private void appendLink(final StringBuilder rendered, final String payload) {
        if (payload.isEmpty()) {
            return;
        }

        final int separator = findWhitespace(payload);
        final String label = separator < 0 ? "" : payload.substring(separator + 1).trim();
        if (label.isEmpty()) {
            final String reference = separator < 0 ? payload : payload.substring(0, separator);
            rendered.append(escapeText(shortenReference(reference)));
            return;
        }

        renderFragment(label, rendered);
    }

    private String shortenReference(final String reference) {
        final String trimmed = reference.trim();
        final int hashIndex = trimmed.lastIndexOf('#');
        if (hashIndex >= 0 && hashIndex + 1 < trimmed.length()) {
            return trimmed.substring(hashIndex + 1);
        }

        final int dotIndex = trimmed.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex + 1 < trimmed.length()) {
            return trimmed.substring(dotIndex + 1);
        }

        return trimmed;
    }

    private void appendCode(final StringBuilder rendered, final String payload) {
        final String escaped = escapeText(payload);
        final String marker = escaped.contains(INLINE_CODE_MARKER)
                ? ESCAPED_INLINE_CODE_MARKER
                : INLINE_CODE_MARKER;
        rendered.append(marker)
                .append(escaped)
                .append(marker);
    }

    private void startListItem(final StringBuilder rendered) {
        trimTrailingSpaces(rendered);
        if (rendered.length() > 0 && rendered.charAt(rendered.length() - 1) != '\n') {
            rendered.append('\n');
        }
        rendered.append("- ");
    }

    private void appendParagraphBreak(final StringBuilder rendered) {
        trimTrailingSpaces(rendered);
        if (rendered.length() == 0 || endsWith(rendered, PARAGRAPH_BREAK)) {
            return;
        }
        if (rendered.charAt(rendered.length() - 1) == '\n') {
            rendered.append('\n');
            return;
        }
        rendered.append(PARAGRAPH_BREAK);
    }

    private void appendLineBreak(final StringBuilder rendered) {
        trimTrailingSpaces(rendered);
        if (rendered.length() == 0 || rendered.charAt(rendered.length() - 1) == '\n') {
            return;
        }
        rendered.append('\n');
    }

    private String cleanup(final String rendered) {
        final String[] lines = normalize(rendered).split(NEW_LINE, -1);
        final StringBuilder cleaned = new StringBuilder();
        boolean blankLinePending = false;

        for (String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (cleaned.length() > 0) {
                    blankLinePending = true;
                }
                continue;
            }

            if (cleaned.length() > 0) {
                cleaned.append(blankLinePending ? PARAGRAPH_BREAK : NEW_LINE);
            }
            cleaned.append(trimmed);
            blankLinePending = false;
        }

        return cleaned.toString();
    }

    private String trimBlankLines(final String value) {
        final String[] lines = normalize(value).split(NEW_LINE, -1);
        int start = 0;
        int end = lines.length;

        while (start < end && isBlank(lines[start])) {
            start++;
        }
        while (end > start && isBlank(lines[end - 1])) {
            end--;
        }

        final StringBuilder result = new StringBuilder();
        for (int index = start; index < end; index++) {
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(lines[index]);
        }
        return result.toString();
    }

    private int updateInlineTagDepth(final String line, final int initialDepth) {
        int depth = initialDepth;
        int index = 0;
        while (index < line.length()) {
            final char current = line.charAt(index);
            if (depth == 0) {
                if (current == '{' && index + 1 < line.length() && line.charAt(index + 1) == '@') {
                    depth = 1;
                    index += 2;
                    continue;
                }
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
            }
            index++;
        }
        return depth;
    }

    private void trimTrailingSpaces(final StringBuilder builder) {
        while (builder.length() > 0) {
            final char current = builder.charAt(builder.length() - 1);
            if (current != ' ' && current != '\t') {
                break;
            }
            builder.deleteCharAt(builder.length() - 1);
        }
    }

    private boolean endsWith(final StringBuilder builder, final String suffix) {
        return builder.length() >= suffix.length()
                && builder.substring(builder.length() - suffix.length()).equals(suffix);
    }

    private int findWhitespace(final String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private int findTagNameEnd(final String tag) {
        for (int index = 0; index < tag.length(); index++) {
            final char current = tag.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '-' || current == '_')) {
                return index;
            }
        }
        return tag.length();
    }

    private int findClosingTag(final String fragment, final int fromIndex, final String tagName) {
        return fragment.toLowerCase(Locale.ROOT).indexOf(CLOSING_TAG_PREFIX + tagName + HTML_TAG_END, fromIndex);
    }

    private int findInlineTagEnd(final String fragment, final int start) {
        int depth = 1;
        for (int index = start + 2; index < fragment.length(); index++) {
            final char current = fragment.charAt(index);
            if (current == '{') {
                depth++;
                continue;
            }
            if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private String trimTrailingWhitespace(final String line) {
        int end = line.length();
        while (end > 0) {
            final char current = line.charAt(end - 1);
            if (current != ' ' && current != '\t') {
                break;
            }
            end--;
        }
        return line.substring(0, end);
    }

    private String normalize(final String value) {
        return value.replace("\r\n", NEW_LINE).replace('\r', '\n');
    }

    private boolean isBlank(final String value) {
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isWhitespace(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private int renderEntityReference(final String fragment, final int start, final StringBuilder rendered) {
        final int end = fragment.indexOf(';', start + 1);
        if (end < 0) {
            return start;
        }

        final String decoded = decodeEntity(fragment.substring(start + 1, end));
        if (decoded == null) {
            return start;
        }

        rendered.append(escapeText(decoded));
        return end + 1;
    }

    @SuppressWarnings(
        {
                "checkstyle:CyclomaticComplexity",
                "checkstyle:NPathComplexity",
                "checkstyle:ReturnCount"}
    )
    private String decodeEntity(final String entity) {
        if (entity.isEmpty()) {
            return null;
        }

        if (entity.charAt(0) == '#') {
            return decodeNumericEntity(entity);
        }

        if ("amp".equals(entity)) {
            return Character.toString('&');
        }
        if ("lt".equals(entity)) {
            return Character.toString('<');
        }
        if ("gt".equals(entity)) {
            return Character.toString('>');
        }
        if ("quot".equals(entity)) {
            return "\"";
        }
        if ("apos".equals(entity)) {
            return "'";
        }
        if ("nbsp".equals(entity)) {
            return " ";
        }
        if ("lbrace".equals(entity)) {
            return "{";
        }
        if ("rbrace".equals(entity)) {
            return "}";
        }
        if ("commat".equals(entity)) {
            return Character.toString('@');
        }
        return null;
    }

    private String decodeNumericEntity(final String entity) {
        try {
            final int codePoint;
            if (entity.startsWith("#x") || entity.startsWith("#X")) {
                codePoint = Integer.parseInt(entity.substring(2), 16);
            } else {
                codePoint = Integer.parseInt(entity.substring(1), 10);
            }
            return new String(Character.toChars(codePoint));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String escapeText(final String value) {
        final StringBuilder escaped = new StringBuilder();
        int index = 0;
        while (index < value.length()) {
            final char current = value.charAt(index);
            if (current == '&') {
                final int nextIndex = renderEntityReference(value, index, escaped);
                if (nextIndex > index) {
                    index = nextIndex;
                    continue;
                }
                escaped.append(HTML_AMP);
            } else if (current == '<') {
                escaped.append(HTML_LT);
            } else if (current == '>') {
                escaped.append(HTML_GT);
            } else {
                escaped.append(current);
            }
            index++;
        }
        return escaped.toString();
    }
}
