package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author charlie (Dmitry Baev).
 */
public class ParameterisedTests {

    @ParameterizedTest
    @ValueSource(strings = {"Hello", "World"})
    void testWithStringParameter(String argument) {
    }

}
