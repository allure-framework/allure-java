package io.qameta.allure.junit5.features;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class TestsWithSteps {

    @Test
    void testWithSteps() {
        first();
        second();
        third();
    }

    @Step
    void first() {
    }

    @Step
    void second() {
    }

    @Step
    void third() {
    }
}