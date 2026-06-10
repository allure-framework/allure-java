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

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
class ProcessDescriptionsTest {

    private static final String ALLURE_DESCRIPTIONS_FOLDER = "META-INF/allureDescriptions/";

    @Test
    void captureDescriptionTest() {
        final String expectedMethodSignatureHash = "4e7f896021ef2fce7c1deb7f5b9e38fb";

        final JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "",
                "public class DescriptionSample {",
                "",
                "/**",
                "* Captured javadoc description",
                "*/",
                "@Description",
                "public void sampleTest() {",
                "}",
                "}"
        );

        final Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor())
                .withOptions("-Werror");
        final Compilation compilation = compiler.compile(source);
        assertThat(compilation)
                .generatedFile(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        ALLURE_DESCRIPTIONS_FOLDER + expectedMethodSignatureHash
                )
                .contentsAsUtf8String()
                .isEqualTo("Captured javadoc description");
    }

    @Test
    void captureDescriptionTestIfNoUseJavadocIsSpecified() {
        final String expectedMethodSignatureHash = "4e7f896021ef2fce7c1deb7f5b9e38fb";

        final JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "",
                "public class DescriptionSample {",
                "",
                "/**",
                "* Captured javadoc description",
                "*/",
                "@Description",
                "public void sampleTest() {",
                "}",
                "}"
        );

        final Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor())
                .withOptions("-Werror");
        final Compilation compilation = compiler.compile(source);
        assertThat(compilation)
                .generatedFile(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        ALLURE_DESCRIPTIONS_FOLDER + expectedMethodSignatureHash
                )
                .contentsAsUtf8String()
                .contains("Captured javadoc description");
    }

    @Test
    void skipUncommentedMethodTest() {
        final JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "",
                "public class DescriptionSample {",
                "",
                "@Description",
                "public void sampleTestWithoutJavadocComment() {",
                "}",
                "}"
        );

        final Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor());
        final Compilation compilation = compiler.compile(source);
        assertThat(compilation).succeeded();
    }

    @Test
    void captureDescriptionParametrizedTestWithGenericParameterTest() {
        final String expectedMethodSignatureHash = "e90e26691bf14511db819d78624ba716";

        final JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "import org.junit.jupiter.params.ParameterizedTest;",
                "import org.junit.jupiter.params.provider.MethodSource;",
                "import java.util.Arrays;",
                "import java.util.List;",
                "import java.util.stream.Stream;",
                "",
                "public class DescriptionSample {",
                "",
                "/**",
                "* Captured javadoc description",
                "*/",
                "@ParameterizedTest",
                "@MethodSource(\"provideStringListParameters\")",
                "@Description",
                "public void sampleParametrizedTestWithGenericParameterAndJavadocComment(List<String> stringList) {",
                "}",
                "",
                "private static Stream<List<String>> provideStringListParameters() {",
                "return Stream.of(Arrays.asList(\"foo\", \"bar\"));",
                "}",
                "}"
        );

        final Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor());
        final Compilation compilation = compiler.compile(source);
        assertThat(compilation).generatedFile(
                StandardLocation.CLASS_OUTPUT,
                "",
                ALLURE_DESCRIPTIONS_FOLDER + expectedMethodSignatureHash
        );
    }

    @Test
    void captureDescriptionParametrizedTestWithPrimitivesParameterTest() {
        final String expectedMethodSignatureHash = "edeeeaa02f01218cc206e0c6ff024c7a";

        final JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "import org.junit.jupiter.params.ParameterizedTest;",
                "import org.junit.jupiter.params.provider.ValueSource;",
                "",
                "public class DescriptionSample {",
                "",
                "/**",
                "* Captured javadoc description",
                "*/",
                "@ParameterizedTest",
                "@ValueSource(ints = {1, 2, 3})",
                "@Description",
                "public void sampleParametrizedTestWithPrimitivesParameterAndJavadocComment(int someIntValue) {",
                "}",
                "}"
        );

        final Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor());
        final Compilation compilation = compiler.compile(source);
        assertThat(compilation)
                .generatedFile(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        ALLURE_DESCRIPTIONS_FOLDER + expectedMethodSignatureHash
                )
                .contentsAsUtf8String()
                .isEqualTo("Captured javadoc description");
    }

    @Test
    void shouldIgnoreBlockTagsAndRenderSafeMarkdown() {
        final String expectedMethodSignatureHash = "4e7f896021ef2fce7c1deb7f5b9e38fb";

        final JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "",
                "public class DescriptionSample {",
                "",
                "/**",
                "* This is my test description with {@code sample} and {@literal <safe>}.",
                "*",
                "* <p>Use {@link java.lang.String String} for values.</p>",
                "* <ul>",
                "*     <li>first item</li>",
                "*     <li>second <b>item</b></li>",
                "* </ul>",
                "* <script>alert(\"xss\")</script>",
                "*",
                "* @throws Exception",
                "*             Thrown when the test unexpectedly fails.",
                "*/",
                "@Description",
                "public void sampleTest() throws Exception {",
                "}",
                "}"
        );

        final Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor())
                .withOptions("-Werror");
        final Compilation compilation = compiler.compile(source);
        assertThat(compilation)
                .generatedFile(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        ALLURE_DESCRIPTIONS_FOLDER + expectedMethodSignatureHash
                )
                .contentsAsUtf8String()
                .isEqualTo(
                        "This is my test description with `sample` and &lt;safe&gt;.\n\n"
                                + "Use String for values.\n\n"
                                + "- first item\n"
                                + "- second item\n\n"
                                + "alert(\"xss\")"
                );
    }

    @Test
    void shouldCaptureComplexModernJavadocDescriptionSafely() {
        final String expectedMethodSignatureHash = "4e7f896021ef2fce7c1deb7f5b9e38fb";

        final JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "",
                "public class DescriptionSample {",
                "",
                "/**",
                "* Fetches release metadata for the current build.",
                "*",
                "* <p>Use {@link java.net.URI URIs} for endpoint configuration.</p>",
                "* <ul>",
                "*     <li>Supports café, Привет, 東京, and λ.</li>",
                "*     <li>See the <a href=\"https://docs.oracle.com/\">Javadoc specification</a>",
                "*     and {@linkplain java.lang.String#formatted(Object...) formatted examples}.</li>",
                "* </ul>",
                "* Example: <code>client.fetch(\"v2\")</code>",
                "* &#064;beta remains prose.",
                "*",
                "* @version 2.3.0",
                "* @since 2.0",
                "* @see <a href=\"https://download.java.net/java/early_access/jdk27/docs/specs/javadoc/doc-comment-spec.html\">Javadoc spec</a>",
                "*/",
                "@Description",
                "public void sampleTest() {",
                "}",
                "}"
        );

        final Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor())
                .withOptions("-Werror");
        final Compilation compilation = compiler.compile(source);
        assertThat(compilation)
                .generatedFile(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        ALLURE_DESCRIPTIONS_FOLDER + expectedMethodSignatureHash
                )
                .contentsAsUtf8String()
                .isEqualTo(
                        "Fetches release metadata for the current build.\n\n"
                                + "Use URIs for endpoint configuration.\n\n"
                                + "- Supports café, Привет, 東京, and λ.\n"
                                + "- See the Javadoc specification\n"
                                + "and formatted examples.\n\n"
                                + "Example: `client.fetch(\"v2\")`\n"
                                + "@beta remains prose."
                );
    }
}
