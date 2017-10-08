package io.qameta.allure.junit5.features;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author charlie (Dmitry Baev).
 */
public class DynamicTests {

    @TestFactory
    Stream<DynamicTest> dynamicTestsFromStream() {
        return Stream.of("A", "B", "C").map(
                str -> dynamicTest("test" + str, () -> {
                }));
    }

}
