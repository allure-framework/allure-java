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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaDocDescriptionRendererTest {

    private final JavaDocDescriptionRenderer renderer = new JavaDocDescriptionRenderer();

    @Test
    void shouldRenderPlainTextAndTrimBlankLines() {
        final String rendered = renderer.render(
                "\r\n"
                + "  First line   \r\n"
                + "\r\n"
                + "  Second line\t\r\n"
                + "\r\n"
        );

        assertThat(rendered)
                .isEqualTo("First line\n\nSecond line");
    }

    @Test
    void shouldReturnEmptyStringWhenBodyContainsOnlyBlockTags() {
        final String rendered = renderer.render(
                "@param value description\n"
                + "@throws Exception description"
        );

        assertThat(rendered)
                .isEmpty();
    }

    @Test
    void shouldIgnoreBlockTagsAndEverythingAfterThem() {
        final String rendered = renderer.render(
                "Summary paragraph.\n"
                + "\n"
                + "@param value Description of the value.\n"
                + "Continuation that should also be ignored."
        );

        assertThat(rendered)
                .isEqualTo("Summary paragraph.");
    }

    @Test
    void shouldIgnoreStandardBlockTagsAfterMainDescription() {
        final List<String> blockTags = List.of(
                "author",
                "deprecated",
                "exception",
                "hidden",
                "param",
                "provides",
                "return",
                "see",
                "serial",
                "serialData",
                "serialField",
                "since",
                "spec",
                "throws",
                "uses",
                "version"
        );

        for (String blockTag : blockTags) {
            assertThat(renderer.render("Summary paragraph.\n@" + blockTag + " metadata"))
                    .as(blockTag)
                    .isEqualTo("Summary paragraph.");
        }
    }

    @Test
    void shouldNotTreatAtSignsInsideTextAsBlockTags() {
        final String rendered = renderer.render(
                "Email support@example.com\n"
                + "Use @smoke in prose."
        );

        assertThat(rendered)
                .isEqualTo("Email support@example.com\nUse @smoke in prose.");
    }

    @Test
    void shouldDecodeEscapedAtEntityBeforeBlockTags() {
        final String rendered = renderer.render(
                "&#064;version stays in prose.\n"
                + "@version 2.4.0"
        );

        assertThat(rendered)
                .isEqualTo("@version stays in prose.");
    }

    @Test
    void shouldPreserveUnicodeCharactersInDescriptions() {
        final String rendered = renderer.render("Release notes: cafe, café, Привет, 東京, λ.");

        assertThat(rendered)
                .isEqualTo("Release notes: cafe, café, Привет, 東京, λ.");
    }

    @Test
    void shouldDecodeSupportedNamedAndNumericEntities() {
        final String rendered = renderer.render(
                "Use &lt;tag&gt;, &amp;, &lbrace;x&rbrace;, &#064;, &#955;, and &#x03BB;."
        );

        assertThat(rendered)
                .isEqualTo("Use &lt;tag&gt;, &amp;, {x}, @, λ, and λ.");
    }

    @Test
    void shouldRenderSupportedInlineTags() {
        final String rendered = renderer.render(
                "Use {@code a < b}, {@literal <safe>}, "
                + "{@link java.lang.String}, "
                + "{@linkplain java.lang.String#valueOf(Object)}, "
                + "{@link java.util.List list docs}."
        );

        assertThat(rendered)
                .isEqualTo("Use `a &lt; b`, &lt;safe&gt;, String, valueOf(Object), list docs.");
    }

    @Test
    void shouldSupportBalancedBracesInsideInlineTags() {
        final String rendered = renderer.render(
                "Payload {@code {\"outer\": {\"inner\": true}}}."
        );

        assertThat(rendered)
                .isEqualTo("Payload `{\"outer\": {\"inner\": true}}`.");
    }

    @Test
    void shouldNotTreatAtLinesInsideBalancedInlineTagsAsBlockTags() {
        final String rendered = renderer.render(
                "Summary {@literal first line\n"
                + "@notATag\n"
                + "last line}\n"
                + "@param ignored"
        );

        assertThat(rendered)
                .isEqualTo("Summary first line\n@notATag\nlast line");
    }

    @Test
    void shouldRenderNestedInlineTagsInsideLinkLabels() {
        final String rendered = renderer.render(
                "See {@linkplain java.util.List docs with {@code List}}."
        );

        assertThat(rendered)
                .isEqualTo("See docs with `List`.");
    }

    @Test
    void shouldSafelyDegradeUnsupportedStandardInlineTags() {
        final String rendered = renderer.render(
                "Fallbacks: {@docRoot}, {@inheritDoc}, {@index release}, "
                + "{@summary quick summary}, {@systemProperty user.home}, "
                + "{@value java.lang.Integer#MAX_VALUE}."
        );

        assertThat(rendered)
                .isEqualTo(
                        "Fallbacks: docRoot, inheritDoc, index release, summary quick summary, "
                        + "systemProperty user.home, value java.lang.Integer#MAX_VALUE."
                );
    }

    @Test
    void shouldSafelyDegradeSnippetTags() {
        final String rendered = renderer.render(
                "Snippet {@snippet :\n"
                + "int answer = 42;\n"
                + "@highlight substring=\"answer\"\n"
                + "}."
        );

        assertThat(rendered)
                .isEqualTo("Snippet snippet :\nint answer = 42;\n@highlight substring=\"answer\".");
    }

    @Test
    void shouldEscapeUnknownInlineTags() {
        final String rendered = renderer.render("Unsupported {@unknown <tag>} clause.");

        assertThat(rendered)
                .isEqualTo("Unsupported unknown &lt;tag&gt; clause.");
    }

    @Test
    void shouldPreserveMalformedInlineTagsAsText() {
        final String rendered = renderer.render("Broken {@code tag");

        assertThat(rendered)
                .isEqualTo("Broken {@code tag");
    }

    @Test
    void shouldRenderSupportedHtmlStructure() {
        final String rendered = renderer.render(
                "First<p>Second<br>Third<ul><li>one</li><li>two</li></ul><ol><li>three</li></ol>"
        );

        assertThat(rendered)
                .isEqualTo("First\n\nSecond\nThird\n\n- one\n- two\n\n- three");
    }

    @Test
    void shouldIgnoreUnclosedHtmlTagsSafely() {
        final String rendered = renderer.render("Broken <b>bold <i>text");

        assertThat(rendered)
                .isEqualTo("Broken bold text");
    }

    @Test
    void shouldPreserveAngleBracketComparisonsAsText() {
        final String rendered = renderer.render("Math says a < b > c.");

        assertThat(rendered)
                .isEqualTo("Math says a &lt; b &gt; c.");
    }

    @Test
    void shouldIgnoreUnmatchedCodeHtmlTagsSafely() {
        final String rendered = renderer.render("Broken <code>value < limit and stray </code>tag");

        assertThat(rendered)
                .isEqualTo("Broken `value &lt; limit and stray `tag");
    }

    @Test
    void shouldIgnoreOpeningCodeTagWithoutClosingTag() {
        final String rendered = renderer.render("Broken <code>value < limit");

        assertThat(rendered)
                .isEqualTo("Broken value &lt; limit");
    }

    @Test
    void shouldIgnoreClosingCodeTagWithoutOpeningTag() {
        final String rendered = renderer.render("Broken </code>tag");

        assertThat(rendered)
                .isEqualTo("Broken tag");
    }

    @Test
    void shouldRenderHtmlCodeTagAsCodeSpan() {
        final String rendered = renderer.render("<code>name < value & more</code>");

        assertThat(rendered)
                .isEqualTo("`name &lt; value &amp; more`");
    }

    @Test
    void shouldLeaveUnknownEntitiesEscaped() {
        final String rendered = renderer.render("Keep &notAnEntity; literal.");

        assertThat(rendered)
                .isEqualTo("Keep &amp;notAnEntity; literal.");
    }

    @Test
    void shouldDropUnknownHtmlTagsButKeepTheirTextContentEscaped() {
        final String rendered = renderer.render(
                "prefix <script>alert(\"x\")</script> <div>safe & sound</div>"
        );

        assertThat(rendered)
                .isEqualTo("prefix alert(\"x\") safe &amp; sound");
    }

    @Test
    void shouldRenderComplexModernJavadocExampleSafely() {
        final String rendered = renderer.render(
                "Fetches release metadata for the current build.\n"
                + "\n"
                + "<p>Use {@link java.net.URI URIs} for endpoint configuration.</p>\n"
                + "<ul>\n"
                + "<li>Supports café, Привет, 東京, and λ.</li>\n"
                + "<li>See the <a href=\"https://docs.oracle.com/\">Javadoc specification</a> "
                + "and {@linkplain java.lang.String#formatted(Object...) formatted examples}.</li>\n"
                + "</ul>\n"
                + "Example: <code>client.fetch(\"v2\")</code>\n"
                + "&#064;beta remains prose.\n"
                + "@author Jane Doe\n"
                + "@version 2.3.0\n"
                + "@since 2.0"
        );

        assertThat(rendered)
                .isEqualTo(
                        "Fetches release metadata for the current build.\n\n"
                        + "Use URIs for endpoint configuration.\n\n"
                        + "- Supports café, Привет, 東京, and λ.\n"
                        + "- See the Javadoc specification and formatted examples.\n\n"
                        + "Example: `client.fetch(\"v2\")`\n"
                        + "@beta remains prose."
                );
    }
}
