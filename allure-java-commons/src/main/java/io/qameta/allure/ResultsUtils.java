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

    public static io.qameta.allure.model.Link createLink(String value, String name, String url, String type) {
        String resolvedName = firstNonEmpty(value).orElse(name);
        String resolvedUrl = firstNonEmpty(url)
                .orElseGet(() -> getLinkUrl(resolvedName, type));
        return new io.qameta.allure.model.Link()
                .withName(resolvedName)
                .withUrl(resolvedUrl)
                .withType(type);
    }

    public static String getHostName() {
        String fromProperty = System.getProperty(ALLURE_HOST_NAME_SYSPROP);
        String fromEnv = System.getenv(ALLURE_HOST_NAME_ENV);
        return Stream.of(fromProperty, fromEnv)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(ResultsUtils::getRealHostName);
    }

    public static String getThreadName() {
        String fromProperty = System.getProperty(ALLURE_THREAD_NAME_SYSPROP);
        String fromEnv = System.getenv(ALLURE_THREAD_NAME_ENV);
        return Stream.of(fromProperty, fromEnv)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(ResultsUtils::getRealThreadName);
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

    public static Optional<String> firstNonEmpty(String... items) {
        return Stream.of(items)
                .filter(Objects::nonNull)
                .filter(item -> !item.isEmpty())
                .findFirst();
    }

    public static String getLinkTypePatternPropertyName(String type) {
        return String.format("allure.link.%s.pattern", type);
    }

    private static String getLinkUrl(String name, String type) {
        String pattern = System.getProperty(getLinkTypePatternPropertyName(type));
        if (Objects.isNull(pattern)) {
            return null;
        }
        return pattern.replaceAll("\\{}", Objects.isNull(name) ? "" : name);
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

    private static String getRealThreadName() {
        return String.format("%s.%s(%s)",
                ManagementFactory.getRuntimeMXBean().getName(),
                Thread.currentThread().getName(),
                Thread.currentThread().getId());
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
