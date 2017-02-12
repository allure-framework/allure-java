package io.qameta.allure;

import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;


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
                        .withTrace(getStackTraceAsString(throwable)));
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
