package io.qameta.allure.descriptions.test;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.qameta.allure.descriptions.DescriptionsProcessor;
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
        final String expectedMethodSignatureHash = "3bfd90093a92af33104abb88bc989a90";

        JavaFileObject source = JavaFileObjects.forSourceLines(
                "io.qameta.allure.descriptions.test.DescriptionSample",
                "package io.qameta.allure.descriptions.test;",
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

        Compiler compiler = javac().withProcessors(new DescriptionsProcessor());
        Compilation compilation = compiler.compile(source);
        assertThat(compilation).generatedFile(StandardLocation.CLASS_OUTPUT,
                ALLURE_PACKAGE_NAME, expectedMethodSignatureHash);
    }
}
