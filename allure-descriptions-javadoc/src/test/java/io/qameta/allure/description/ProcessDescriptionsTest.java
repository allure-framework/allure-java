/*
 *  Copyright 2019 Qameta Software OÜ
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
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class ProcessDescriptionsTest {
    @Test
    void captureDescriptionTest() {
        final String expectedMethodSignatureHash = "4e7f896021ef2fce7c1deb7f5b9e38fb";

        JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "",
                "public class DescriptionSample {",
                "",
                "/**",
                "* Captured javadoc description",
                "*/",
                "@Description(useJavaDoc = true)",
                "public void sampleTest() {",
                "}",
                "}"
        );

        Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor())
                .withOptions("-Werror");
        Compilation compilation = compiler.compile(source);
        assertThat(compilation).generatedFile(
                StandardLocation.CLASS_OUTPUT,
                "",
                ResultsUtils.ALLURE_DESCRIPTIONS_FOLDER + expectedMethodSignatureHash
        );
    }

    @Test
    void skipUncommentedMethodTest() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "",
                "public class DescriptionSample {",
                "",
                "@Description(useJavaDoc = true)",
                "public void sampleTestWithoutJavadocComment() {",
                "}",
                "}"
        );

        Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor());
        Compilation compilation = compiler.compile(source);
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .hadWarningContaining("Unable to create resource for method "
                        + "sampleTestWithoutJavadocComment[] as it does not have a docs comment");
    }

    @Test
    void captureDescriptionParametrizedTestWithGenericParameterTest() {
        final String expectedMethodSignatureHash = "e90e26691bf14511db819d78624ba716";

        JavaFileObject source = JavaFileObjects.forSourceLines(
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
                "@Description(useJavaDoc = true)",
                "public void sampleParametrizedTestWithGenericParameterAndJavadocComment(List<String> stringList) {",
                "}",
                "",
                "private static Stream<List<String>> provideStringListParameters() {",
                "return Stream.of(Arrays.asList(\"foo\", \"bar\"));",
                "}",
                "}"
        );

        Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor());
        Compilation compilation = compiler.compile(source);
        assertThat(compilation).generatedFile(
                StandardLocation.CLASS_OUTPUT,
                "",
                ResultsUtils.ALLURE_DESCRIPTIONS_FOLDER + expectedMethodSignatureHash
        );
    }

    @Test
    void captureDescriptionParametrizedTestWithPrimitivesParameterTest() {
        final String expectedMethodSignatureHash = "edeeeaa02f01218cc206e0c6ff024c7a";

        JavaFileObject source = JavaFileObjects.forSourceLines(
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
                "@Description(useJavaDoc = true)",
                "public void sampleParametrizedTestWithPrimitivesParameterAndJavadocComment(int someIntValue) {",
                "}",
                "}"
        );

        Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor());
        Compilation compilation = compiler.compile(source);
        assertThat(compilation).generatedFile(
                StandardLocation.CLASS_OUTPUT,
                "",
                ResultsUtils.ALLURE_DESCRIPTIONS_FOLDER + expectedMethodSignatureHash
        );
    }
}
