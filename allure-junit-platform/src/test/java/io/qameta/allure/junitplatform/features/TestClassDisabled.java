package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Disabled
public class TestClassDisabled {

    @DisplayName("First")
    @Test
    void first() {
    }

    @Feature("A")
    @Test
    void second() {
    }

    @Owner("me")
    @Test
    void third() {
    }
}
