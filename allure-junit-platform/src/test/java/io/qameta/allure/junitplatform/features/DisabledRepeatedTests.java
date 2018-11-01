package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Owner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

/**
 * @author charlie (Dmitry Baev).
 */
public class DisabledRepeatedTests {

    @Owner("other guy")
    @Disabled
    @RepeatedTest(5)
    void first() {
    }
}
