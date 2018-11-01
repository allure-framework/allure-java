package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Owner;
import org.junit.jupiter.api.RepeatedTest;

/**
 * @author charlie (Dmitry Baev).
 */
public class RepeatedTests {

    @Owner("me")
    @RepeatedTest(5)
    void first() {
    }
}
