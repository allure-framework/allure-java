package io.qameta.allure.descriptions.test;

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.qameta.allure.descriptions.DescriptionsProcessor;
import org.testng.annotations.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static io.qameta.allure.util.ResultsUtils.generateMethodSignatureHash;

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

    @Test
    public void descriptionOfTestWithParameterTypeAsInnerClass() {
        final String expectedMethodSignatureHash = generateMethodSignatureHash(
                "io.qameta.allure.descriptions.test.DescriptionSample",
                "sampleTest",
                ImmutableList.of("io.qameta.allure.descriptions.test.DescriptionSample$Inner"));

        JavaFileObject source = JavaFileObjects
                .forSourceString("io.qameta.allure.descriptions.test.DescriptionSample",
                        "package io.qameta.allure.descriptions.test;\n" +
                                "\n" +
                                "import io.qameta.allure.Description;\n" +
                                "import org.testng.annotations.DataProvider;\n" +
                                "import org.testng.annotations.Test;\n" +
                                "\n" +
                                "public class DescriptionSample {\n" +
                                "\n" +
                                "    /**\n" +
                                "     * Captured javadoc fixture description\n" +
                                "     */\n" +
                                "    @Description(useJavaDoc = true)\n" +
                                "    @DataProvider(name = \"dataProvider\")\n" +
                                "    public Object[][] getTestData() {\n" +
                                "        return new Inner[][]{\n" +
                                "                {new Inner()}\n" +
                                "        };\n" +
                                "    }\n" +
                                "\n" +
                                "    /**\n" +
                                "     * Captured javadoc test description\n" +
                                "     */\n" +
                                "    @Test(dataProvider = \"dataProvider\")\n" +
                                "    @Description(useJavaDoc = true)\n" +
                                "    public void sampleTest(Inner dataProvider) {\n" +
                                "    }\n" +
                                "\n" +
                                "    class Inner {}\n" +
                                "\n" +
                                "}");
        Compiler compiler = javac().withProcessors(new DescriptionsProcessor());
        Compilation compilation = compiler.compile(source);
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, ALLURE_PACKAGE_NAME, expectedMethodSignatureHash);
    }

}
