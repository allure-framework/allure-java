package io.qameta.allure;

import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * @author charlie (Dmitry Baev).
 */
public final class ResultsUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsUtils.class);

    public static final String ALLURE_HOST_NAME_SYSPROP = "allure.hostName";
    public static final String ALLURE_HOST_NAME_ENV = "ALLURE_HOST_NAME";

    public static final String ALLURE_THREAD_NAME_SYSPROP = "allure.threadName";
    public static final String ALLURE_THREAD_NAME_ENV = "ALLURE_THREAD_NAME";

    private static String CACHED_HOST = null;

    ResultsUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static String getHostName() {
        String fromProperty = System.getProperty(ALLURE_HOST_NAME_SYSPROP);
        String fromEnv = System.getenv(ALLURE_HOST_NAME_ENV);
        return Stream.of(fromProperty, fromEnv)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(ResultsUtils::getRealHostName);
    }

    private static String getRealHostName() {
        if (Objects.isNull(CACHED_HOST)) {
            try {
                CACHED_HOST = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) { //NOSONAR
                LOGGER.debug("Could not get host name {}", e);
                CACHED_HOST = "default";
            }
        }
        return CACHED_HOST;
    }

    public static String getTheadName() {
        String fromProperty = System.getProperty(ALLURE_THREAD_NAME_SYSPROP);
        String fromEnv = System.getenv(ALLURE_THREAD_NAME_ENV);
        return Stream.of(fromProperty, fromEnv)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(ResultsUtils::getRealTheadName);
    }

    private static String getRealTheadName() {
        return String.format("%s.%s(%s)",
                ManagementFactory.getRuntimeMXBean().getName(),
                Thread.currentThread().getName(),
                Thread.currentThread().getId());
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
