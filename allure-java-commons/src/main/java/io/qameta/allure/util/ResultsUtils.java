package io.qameta.allure.util;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import org.apache.tika.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.PropertiesUtils.loadAllureProperties;
import static java.lang.Boolean.parseBoolean;
import static java.util.Objects.nonNull;

/**
 * The collection of Allure utils methods.
 */
@SuppressWarnings({
        "ClassFanOutComplexity",
        "PMD.ExcessiveImports",
        "PMD.TooManyMethods",
        "PMD.GodClass",
        "deprecation"
})
public final class ResultsUtils {

    public static final String ALLURE_HOST_NAME_SYSPROP = "allure.hostName";
    public static final String ALLURE_HOST_NAME_ENV = "ALLURE_HOST_NAME";
    public static final String ALLURE_THREAD_NAME_SYSPROP = "allure.threadName";
    public static final String ALLURE_THREAD_NAME_ENV = "ALLURE_THREAD_NAME";
    public static final String ALLURE_SEPARATE_LINES_SYSPROP = "allure.description.javadoc.separateLines";

    public static final String ISSUE_LINK_TYPE = "issue";
    public static final String TMS_LINK_TYPE = "tms";

    public static final String ALLURE_ID_LABEL_NAME = "AS_ID";
    public static final String SUITE_LABEL_NAME = "suite";
    public static final String PARENT_SUITE_LABEL_NAME = "parentSuite";
    public static final String SUB_SUITE_LABEL_NAME = "subSuite";
    public static final String EPIC_LABEL_NAME = "epic";
    public static final String FEATURE_LABEL_NAME = "feature";
    public static final String STORY_LABEL_NAME = "story";
    public static final String SEVERITY_LABEL_NAME = "severity";
    public static final String TAG_LABEL_NAME = "tag";
    public static final String OWNER_LABEL_NAME = "owner";
    public static final String LEAD_LABEL_NAME = "lead";
    public static final String HOST_LABEL_NAME = "host";
    public static final String THREAD_LABEL_NAME = "thread";
    public static final String TEST_METHOD_LABEL_NAME = "testMethod";
    public static final String TEST_CLASS_LABEL_NAME = "testClass";
    public static final String PACKAGE_LABEL_NAME = "package";
    public static final String FRAMEWORK_LABEL_NAME = "framework";
    public static final String LANGUAGE_LABEL_NAME = "language";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsUtils.class);
    private static final String ALLURE_DESCRIPTIONS_PACKAGE = "allureDescriptions/";
    private static final String MD_5 = "MD5";

    private static String cachedHost;

    private ResultsUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static Label createSuiteLabel(final String suite) {
        return new Label().setName(SUITE_LABEL_NAME).setValue(suite);
    }

    public static Label createParentSuiteLabel(final String suite) {
        return new Label().setName(PARENT_SUITE_LABEL_NAME).setValue(suite);
    }

    public static Label createSubSuiteLabel(final String suite) {
        return new Label().setName(SUB_SUITE_LABEL_NAME).setValue(suite);
    }

    public static Label createTestMethodLabel(final String testMethod) {
        return new Label().setName(TEST_METHOD_LABEL_NAME).setValue(testMethod);
    }

    public static Label createTestClassLabel(final String testClass) {
        return new Label().setName(TEST_CLASS_LABEL_NAME).setValue(testClass);
    }

    public static Label createPackageLabel(final String packageName) {
        return new Label().setName(PACKAGE_LABEL_NAME).setValue(packageName);
    }

    public static Label createEpicLabel(final String epic) {
        return new Label().setName(EPIC_LABEL_NAME).setValue(epic);
    }

    public static Label createFeatureLabel(final String feature) {
        return new Label().setName(FEATURE_LABEL_NAME).setValue(feature);
    }

    public static Label createStoryLabel(final String story) {
        return new Label().setName(STORY_LABEL_NAME).setValue(story);
    }

    public static Label createTagLabel(final String tag) {
        return new Label().setName(TAG_LABEL_NAME).setValue(tag);
    }

    public static Label createOwnerLabel(final String owner) {
        return new Label().setName(OWNER_LABEL_NAME).setValue(owner);
    }

    public static Label createSeverityLabel(final SeverityLevel severity) {
        return createSeverityLabel(severity.value());
    }

    public static Label createSeverityLabel(final String severity) {
        return new Label().setName(SEVERITY_LABEL_NAME).setValue(severity);
    }

    public static Label createHostLabel() {
        return new Label().setName(HOST_LABEL_NAME).setValue(getHostName());
    }

    public static Label createThreadLabel() {
        return new Label().setName(THREAD_LABEL_NAME).setValue(getThreadName());
    }

    public static Label createFrameworkLabel(final String framework) {
        return new Label().setName(FRAMEWORK_LABEL_NAME).setValue(framework);
    }

    public static Label createLanguageLabel(final String language) {
        return new Label().setName(LANGUAGE_LABEL_NAME).setValue(language);
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
                .setName(resolvedName)
                .setUrl(resolvedUrl)
                .setType(type);
    }

    public static Set<Label> getProvidedLabels() {
        final Properties properties = loadAllureProperties();
        final Set<String> propertyNames = properties.stringPropertyNames();
        return propertyNames.stream()
                .filter(name -> name.startsWith("allure.label."))
                .map(name -> {
                    final String labelName = name.substring(13);
                    final String labelValue = properties.getProperty(name);
                    return new Label()
                            .setName(labelName)
                            .setValue(labelValue);
                })
                .filter(label -> nonNull(label.getValue()))
                .collect(Collectors.toSet());
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
                        .setMessage(Optional.ofNullable(throwable.getMessage()).orElse(throwable.getClass().getName()))
                        .setTrace(getStackTraceAsString(throwable)));
    }

    public static Optional<String> getJavadocDescription(final ClassLoader classLoader,
                                                         final Method method) {
        final String name = method.getName();
        final List<String> parameterTypes = Stream.of(method.getParameterTypes())
                .map(Class::getTypeName)
                .collect(Collectors.toList());

        final String signatureHash = generateMethodSignatureHash(
                method.getDeclaringClass().getName(),
                name,
                parameterTypes);

        return readResource(classLoader, ALLURE_DESCRIPTIONS_PACKAGE + signatureHash)
                .map(desc -> separateLines() ? desc.replace("\n", "<br />") : desc);
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

    public static String generateMethodSignatureHash(final String className,
                                                     final String methodName,
                                                     final List<String> parameterTypes) {
        final MessageDigest md = getMd5Digest();
        md.update(className.getBytes(StandardCharsets.UTF_8));
        md.update(methodName.getBytes(StandardCharsets.UTF_8));
        parameterTypes.stream()
                .map(string -> string.getBytes(StandardCharsets.UTF_8))
                .forEach(md::update);
        final byte[] bytes = md.digest();
        return bytesToHex(bytes);
    }

    public static String md5(final String source) {
        Objects.requireNonNull(source, "null source");
        return bytesToHex(getMd5Digest().digest(source.getBytes(StandardCharsets.UTF_8)));
    }

    public static String bytesToHex(final byte[] bytes) {
        return new BigInteger(1, bytes).toString(16);
    }

    public static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Can not find hashing algorithm", e);
        }
    }

    private static String getLinkUrl(final String name, final String type) {
        final Properties properties = loadAllureProperties();
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

    /**
     * @deprecated use {@link #getJavadocDescription(ClassLoader, Method)} instead.
     */
    @Deprecated
    public static void processDescription(final ClassLoader classLoader,
                                          final Method method,
                                          final io.qameta.allure.model.ExecutableItem item) {
        if (method.isAnnotationPresent(Description.class)) {
            if (method.getAnnotation(Description.class).useJavaDoc()) {
                getJavadocDescription(classLoader, method)
                        .ifPresent(item::setDescriptionHtml);
            } else {
                final String description = method.getAnnotation(Description.class).value();
                item.setDescription(description);
            }
        }
    }

    private static Optional<String> readResource(final ClassLoader classLoader, final String resourceName) {
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            if (Objects.isNull(is)) {
                return Optional.empty();
            }
            final byte[] bytes = IOUtils.toByteArray(is);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.warn("Unable to process description resource file", e);
        }
        return Optional.empty();
    }

    private static boolean separateLines() {
        return parseBoolean(loadAllureProperties().getProperty(ALLURE_SEPARATE_LINES_SYSPROP));
    }

}
