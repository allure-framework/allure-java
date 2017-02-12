package io.qameta.allure;

import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;

import java.util.Optional;

import static com.google.common.base.Throwables.getStackTraceAsString;

/**
 * @author charlie (Dmitry Baev).
 */
public final class ResultsUtils {

    ResultsUtils() {
    }

    public static Optional<Status> getStatus(Throwable throwable) {
        return Optional.ofNullable(throwable)
                .map(t -> t instanceof AssertionError ? Status.FAILED : Status.BROKEN);
    }

    public static Optional<StatusDetails> getStatusDetails(Throwable e) {
        return Optional.ofNullable(e)
                .map(throwable -> new StatusDetails()
                        .withMessage(throwable.getMessage())
                        .withTrace(Throwables.getStackTraceAsString(throwable)));
    }
}
