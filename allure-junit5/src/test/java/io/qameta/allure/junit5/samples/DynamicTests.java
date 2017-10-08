package io.qameta.allure.junit5.samples;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author charlie (Dmitry Baev).
 */
public class DynamicTests {

    @TestFactory
    @Test
    @Epic("epic1")
    @Epic("epic2")
    @Epic("epic3")
    @Feature("feature1")
    @Feature("feature2")
    @Feature("feature3")
    @Story("story1")
    @Stories({
            @Story("story2"),
            @Story("story3")
    })
    @Owner("some-owner")
    Stream<DynamicTest> dynamicTestsFromStream() {
        return Stream.of("A", "B", "C").map(str -> dynamicTest("test" + str, () -> {
        }));
    }

}
