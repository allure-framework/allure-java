package io.qameta.allure;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
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
 * The collection of Allure utils methods.
 */
public final class ResultsUtils {

    public static final String ALLURE_HOST_NAME_SYSPROP = "allure.hostName";

    public static final String ALLURE_HOST_NAME_ENV = "ALLURE_HOST_NAME";
    public static final String ALLURE_THREAD_NAME_SYSPROP = "allure.threadName";

    public static final String ALLURE_THREAD_NAME_ENV = "ALLURE_THREAD_NAME";
    public static final String ISSUE_LINK_TYPE = "issue";

    public static final String TMS_LINK_TYPE = "tms";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsUtils.class);

    private static String cachedHost;

    private ResultsUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static Label createLabel(final Owner owner) {
        return new Label().withName("owner").withValue(owner.value());
    }

    public static Label createLabel(final Severity severity) {
        return new Label().withName("severity").withValue(severity.value().value());
    }

    public static Label createLabel(final Story story) {
        return new Label().withName("story").withValue(story.value());
    }

    public static Label createLabel(final Feature feature) {
        return new Label().withName("feature").withValue(feature.value());
    }

    public static Label createLabel(final Epic epic) {
        return new Label().withName("epic").withValue(epic.value());
    }

    public static Link createIssueLink(final String value) {
        return createLink(value, null, null, ISSUE_LINK_TYPE);
    }

    public static Link createTmsLink(final String value) {
        return createLink(value, null, null, TMS_LINK_TYPE);
    }

    public static Link createLink(final io.qameta.allure.Link link) {
        return createLink(link.value(), link.name(), link.url(), link.type());
    }

    public static Link createLink(final io.qameta.allure.Issue link) {
        return createIssueLink(link.value());
    }

    public static Link createLink(final io.qameta.allure.TmsLink link) {
        return createTmsLink(link.value());
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static Link createLink(final String value, final String name,
                                  final String url, final String type) {
        final String resolvedName = firstNonEmpty(value).orElse(name);
        final String resolvedUrl = firstNonEmpty(url)
                .orElseGet(() -> getLinkUrl(resolvedName, type));
        return new Link()
                .withName(resolvedName)
                .withUrl(resolvedUrl)
                .withType(type);
    }

    public static String getHostName() {
        final String fromProperty = System.getProperty(ALLURE_HOST_NAME_SYSPROP);
        final String fromEnv = System.getenv(ALLURE_HOST_NAME_ENV);
        return Stream.of(fromProperty, fromEnv)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(ResultsUtils::getRealHostName);
    }

    public static String getThreadName() {
        final String fromProperty = System.getProperty(ALLURE_THREAD_NAME_SYSPROP);
        final String fromEnv = System.getenv(ALLURE_THREAD_NAME_ENV);
        return Stream.of(fromProperty, fromEnv)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(ResultsUtils::getRealThreadName);
    }

    public static Optional<Status> getStatus(final Throwable throwable) {
        return Optional.ofNullable(throwable)
                .map(t -> t instanceof AssertionError ? Status.FAILED : Status.BROKEN);
    }

    public static Optional<StatusDetails> getStatusDetails(final Throwable e) {
        return Optional.ofNullable(e)
                .map(throwable -> new StatusDetails()
                        .withMessage(throwable.getMessage())
                        .withTrace(getStackTraceAsString(throwable)));
    }

    public static Optional<String> firstNonEmpty(final String... items) {
        return Stream.of(items)
                .filter(Objects::nonNull)
                .filter(item -> !item.isEmpty())
                .findFirst();
    }

    public static String getLinkTypePatternPropertyName(final String type) {
        return String.format("allure.link.%s.pattern", type);
    }

    private static String getLinkUrl(final String name, final String type) {
        final String pattern = System.getProperty(getLinkTypePatternPropertyName(type));
        if (Objects.isNull(pattern)) {
            return null;
        }
        return pattern.replaceAll("\\{}", Objects.isNull(name) ? "" : name);
    }

    private static String getRealHostName() {
        if (Objects.isNull(cachedHost)) {
            try {
                cachedHost = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOGGER.debug("Could not get host name {}", e);
                cachedHost = "default";
            }
        }
        return cachedHost;
    }

    private static String getRealThreadName() {
        return String.format("%s.%s(%s)",
                ManagementFactory.getRuntimeMXBean().getName(),
                Thread.currentThread().getName(),
                Thread.currentThread().getId());
    }

    private static String getStackTraceAsString(final Throwable throwable) {
        final StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
