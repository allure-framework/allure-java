package io.qameta.allure.testng;

import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;

import java.util.List;

/**
 * An example of custom listener that changes broken statuses to failed and disables history.
 *
 * @author charlie (Dmitry Baev).
 */
public class CustomListener extends AllureTestNg {

    @Override
    protected Status getStatus(final Throwable throwable) {
        return Status.FAILED;
    }

    @Override
    protected String getHistoryId(final String name, final List<Parameter> parameters) {
        return null;
    }
}
