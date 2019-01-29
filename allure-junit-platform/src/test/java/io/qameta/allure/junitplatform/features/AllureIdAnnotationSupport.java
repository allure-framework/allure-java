package io.qameta.allure.junitplatform.features;

import io.qameta.allure.AllureId;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureIdAnnotationSupport {

    @AllureId("123")
    @Test
    void single() {
    }
}
