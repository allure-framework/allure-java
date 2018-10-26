package io.qameta.allure.junitplatform;

import io.qameta.allure.model.Status;
import org.junit.platform.launcher.TestIdentifier;

/**
 * @author charlie (Dmitry Baev).
 */
public class CustomListener extends AllureJunitPlatform {

    @Override
    protected Status getStatus(final Throwable throwable) {
        return Status.FAILED;
    }

    @Override
    protected String getHistoryId(final TestIdentifier testIdentifier) {
        return null;
    }
}
