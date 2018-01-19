package io.qameta.allure.util;

import com.google.common.io.Resources;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The collection of Allure utils methods.
 */
@SuppressWarnings({"ClassFanOutComplexity", "PMD.ExcessiveImports", "PMD.TooManyMethods"})
public final class ResultsUtils {

    public static final String ALLURE_HOST_NAME_SYSPROP = "allure.hostName";
    public static final String ALLURE_HOST_NAME_ENV = "ALLURE_HOST_NAME";
    public static final String ALLURE_THREAD_NAME_SYSPROP = "allure.threadName";
    public static final String ALLURE_THREAD_NAME_ENV = "ALLURE_THREAD_NAME";

    public static final String ISSUE_LINK_TYPE = "issue";
    public static final String TMS_LINK_TYPE = "tms";

    public static final String EPIC_LABEL_NAME = "epic";
    public static final String FEATURE_LABEL_NAME = "feature";
    public static final String STORY_LABEL_NAME = "story";
    public static final String SEVERITY_LABEL_NAME = "severity";
    public static final String TAG_LABEL_NAME = "tag";
    public static final String OWNER_LABEL_NAME = "owner";
    public static final String HOST_LABEL_NAME = "host";
    public static final String THREAD_LABEL_NAME = "thread";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsUtils.class);
    private static final String ALLURE_DESCRIPTIONS_PACKAGE = "allureDescriptions/";
    private static final String MD_5 = "MD5";

    private static String cachedHost;

    private ResultsUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static Label createEpicLabel(final String epic) {
        return new Label().withName(EPIC_LABEL_NAME).withValue(epic);
    }

    public static Label createFeatureLabel(final String feature) {
        return new Label().withName(FEATURE_LABEL_NAME).withValue(feature);
    }

    public static Label createStoryLabel(final String story) {
        return new Label().withName(STORY_LABEL_NAME).withValue(story);
    }

    public static Label createTagLabel(final String tag) {
        return new Label().withName(TAG_LABEL_NAME).withValue(tag);
    }

    public static Label createOwnerLabel(final String owner) {
        return new Label().withName(OWNER_LABEL_NAME).withValue(owner);
    }

    public static Label createSeverityLabel(final SeverityLevel severity) {
        return createSeverityLabel(severity.value());
    }

    public static Label createSeverityLabel(final String severity) {
        return new Label().withName(SEVERITY_LABEL_NAME).withValue(severity);
    }

    public static Label createHostLabel() {
        return new Label().withName(HOST_LABEL_NAME).withValue(getHostName());
    }

    public static Label createThreadLabel() {
        return new Label().withName(THREAD_LABEL_NAME).withValue(getThreadName());
    }

    public static Label createLabel(final Owner owner) {
        return createOwnerLabel(owner.value());
    }

    public static Label createLabel(final Severity severity) {
        return createSeverityLabel(severity.value());
    }

    public static Label createLabel(final Story story) {
        return createStoryLabel(story.value());
    }

    public static Label createLabel(final Feature feature) {
        return createFeatureLabel(feature.value());
    }

    public static Label createLabel(final Epic epic) {
        return createEpicLabel(epic.value());
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
                        .withMessage(Optional.ofNullable(throwable.getMessage()).orElse(throwable.getClass().getName()))
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

    public static String generateMethodSignatureHash(final String methodName, final List<String> parameterTypes) {
        final MessageDigest md = getMd5Digest();
        md.update(methodName.getBytes(StandardCharsets.UTF_8));
        parameterTypes.stream()
                .map(string -> string.getBytes(StandardCharsets.UTF_8))
                .forEach(md::update);

        return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
    }

    public static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Can not find hashing algorithm", e);
        }
    }

    private static String getLinkUrl(final String name, final String type) {
        final Properties properties = PropertiesUtils.loadAllureProperties();
        final String pattern = properties.getProperty(getLinkTypePatternPropertyName(type));
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

    public static void processDescription(final ClassLoader classLoader, final Method method,
                                          final ExecutableItem item) {
        if (method.isAnnotationPresent(Description.class)) {
            if (method.getAnnotation(Description.class).useJavaDoc()) {
                final String name = method.getName();
                final List<String> parameterTypes = Stream.of(method.getParameterTypes()).map(Class::getTypeName)
                        .collect(Collectors.toList());
                final String signatureHash = generateMethodSignatureHash(name, parameterTypes);
                final String description;
                try {
                    final URL resource = Optional.ofNullable(classLoader
                            .getResource(ALLURE_DESCRIPTIONS_PACKAGE + signatureHash))
                            .orElseThrow(IOException::new);
                    description = Resources.toString(resource, Charset.defaultCharset());
                    item.withDescriptionHtml(description);
                } catch (IOException e) {
                    LOGGER.warn("Unable to process description resource file for method {} {}", name, e.getMessage());
                }
            } else {
                final String description = method.getAnnotation(Description.class).value();
                item.withDescription(description);
            }
        }
    }

}

