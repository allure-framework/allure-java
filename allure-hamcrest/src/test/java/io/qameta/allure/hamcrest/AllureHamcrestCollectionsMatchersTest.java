package io.qameta.allure.hamcrest;

import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * All tests should cover http://hamcrest.org/JavaHamcrest/tutorial "Collections" section
 * <ui>
 * <li>array</li>
 * <li>hasEntry</li>
 * <li>hasKey</li>
 * <li>hasValue</li>
 * <li>hasItem</li>
 * <li>hasItems</li>
 * <li>hasItemInArray</li>
 * </ui>
 */
@SuppressWarnings("all")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class AllureHamcrestCollectionsMatchersTest {

    @Test
    void hamcrestAssertNameForArrayMatchers() {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(new Integer[]{1,2,3}, is(array(equalTo(1), equalTo(2), equalTo(3)))),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly("assert \"[1, 2, 3]\" is [<1>, <2>, <3>]");
    }

    private static Stream<Arguments> mapTestCases() {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("key1", 1);
        map.put("key2", 2);
        map.put("key3", 3);

        return Stream.of(
                Arguments.of(
                        map, hasEntry(equalTo("key1"), equalTo(1)),
                        "assert \"{key1=1, key2=2, key3=3}\" map containing [\"key1\"-><1>]"
                ),
                Arguments.of(
                        map, hasKey(equalTo("key2")),
                        "assert \"{key1=1, key2=2, key3=3}\" map containing [\"key2\"->ANYTHING]"
                ),
                Arguments.of(
                        map, hasValue(equalTo(3)),
                        "assert \"{key1=1, key2=2, key3=3}\" map containing [ANYTHING-><3>]"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("mapTestCases")
    void hamcrestAssertNameForMapMatchers(Map actual, Matcher matcher, String expectedName) {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(actual, matcher),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(expectedName);
    }

    private static Stream<Arguments> iterableTestCases() {
        List<String> list = Arrays.asList("val1", "val2", "val3");

        return Stream.of(
                Arguments.of(
                        list, hasItem(equalTo("key1")),
                        "assert \"[val1, val2, val3]\" a collection containing \"key1\""
                ),
                Arguments.of(
                        list, hasItems(startsWith("v"), endsWith("l2")),
                        "assert \"[val1, val2, val3]\" (a collection containing a string starting with \"v\" and a collection containing a string ending with \"l2\")"
                ),
                Arguments.of(
                        list, hasItemInArray(Arrays.asList("val1", "val2")),
                        "assert \"[val1, val2, val3]\" an array containing <[val1, val2]>"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("iterableTestCases")
    void hamcrestAssertNameForIterableMatchers(Iterable actual, Matcher matcher, String expectedName) {
        final TestResult testResult = runWithinTestContext(
                () -> assertThat(actual, matcher),
                AllureHamcrestAssert::setLifecycle
        ).getTestResults().get(0);

        Assertions.assertThat(testResult.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(expectedName);
    }
}
