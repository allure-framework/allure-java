package io.qameta.allure.hamcrest;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.TestInstance;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * All tests should cover http://hamcrest.org/JavaHamcrest/tutorial "Text" section
 * <ui>
 * <li>equalToIgnoringCase</li>
 * <li>equalToIgnoringWhiteSpace(deprecated) - suggesting in code to use equalToCompressingWhiteSpace</li>
 * <li>containsString</li>
 * <li>endsWith</li>
 * <li>startsWith</li>
 * </ui>
 */
@SuppressWarnings("all")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class AllureHamcrestTextMatchersTest {

    private static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(
                        "thebiscuit", equalToIgnoringCase("TheBiscuit"),
                        "assert \"thebiscuit\" a string equal to \"TheBiscuit\" ignoring case"
                ),
                Arguments.of(
                        "The Biscuit", equalToIgnoringWhiteSpace("TheBiscuit"),
                        "assert \"The Biscuit\" a string equal to \"TheBiscuit\" compressing white space"
                ),
                Arguments.of(
                        "The Biscuit", equalToCompressingWhiteSpace("TheBiscuit"),
                        "assert \"The Biscuit\" a string equal to \"TheBiscuit\" compressing white space"
                ),
                Arguments.of(
                        "The Biscuit", containsString("TheBiscuit"),
                        "assert \"The Biscuit\" a string containing \"TheBiscuit\""
                ),
                Arguments.of(
                        "The Biscuit", endsWith("Biscuit"),
                        "assert \"The Biscuit\" a string ending with \"Biscuit\""
                ),
                Arguments.of(
                        "The Biscuit", startsWith("Biscuit"),
                        "assert \"The Biscuit\" a string starting with \"Biscuit\""
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void hamcrestAssertNameForTextMatchers(String actual, Matcher matcher, String expectedName) {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(actual, matcher),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(expectedName);
    }
}
