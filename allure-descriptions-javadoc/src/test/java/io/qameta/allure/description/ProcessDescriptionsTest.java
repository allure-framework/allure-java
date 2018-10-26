package io.qameta.allure.description;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.testng.annotations.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class ProcessDescriptionsTest {

    private static final String ALLURE_PACKAGE_NAME = "allureDescriptions";

    @Test
    public void captureDescriptionTest() {
        final String expectedMethodSignatureHash = "4e7f896021ef2fce7c1deb7f5b9e38fb";

        JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.description.test.DescriptionSample",
                "package io.qameta.allure.description.test;",
                "import io.qameta.allure.Description;",
                "import org.testng.annotations.Test;",
                "",
                "public class DescriptionSample {",
                "",
                "/**",
                "* Captured javadoc description",
                "*/",
                "@Test",
                "@Description(useJavaDoc = true)",
                "public void sampleTest() {",
                "}",
                "}"
        );

        Compiler compiler = javac().withProcessors(new JavaDocDescriptionsProcessor());
        Compilation compilation = compiler.compile(source);
        assertThat(compilation).generatedFile(
                StandardLocation.CLASS_OUTPUT,
                ALLURE_PACKAGE_NAME,
                expectedMethodSignatureHash
        );
    }
}
