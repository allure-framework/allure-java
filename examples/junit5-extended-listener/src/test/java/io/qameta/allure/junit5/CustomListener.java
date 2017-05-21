package io.qameta.allure.junit5;

import io.qameta.allure.model.Status;
import org.junit.platform.launcher.TestIdentifier;

/**
 * @author charlie (Dmitry Baev).
 */
public class CustomListener extends AllureJunit5 {

    @Override
    protected Status getStatus(final Throwable throwable) {
        return Status.FAILED;
    }

    @Override
    protected String getHistoryId(final TestIdentifier testIdentifier) {
        return null;
    }
}
