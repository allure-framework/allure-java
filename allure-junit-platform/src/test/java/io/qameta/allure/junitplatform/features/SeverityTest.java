package io.qameta.allure.junitplatform.features;

import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Severity(SeverityLevel.TRIVIAL)
public class SeverityTest {

    @Severity(SeverityLevel.CRITICAL)
    @Test
    void criticalSeverityTest() {
    }

    @Test
    void defaultSeverityTest() {
    }
}
