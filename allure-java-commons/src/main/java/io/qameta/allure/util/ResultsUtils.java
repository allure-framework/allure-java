/*
 *  Copyright 2016-2026 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.SerializedLambda;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.PropertiesUtils.loadAllureProperties;
import static java.lang.Boolean.parseBoolean;
import static java.util.Objects.nonNull;

/**
 * Utility methods for creating and enriching Allure result model objects.
 *
 * <p>Use these helpers when custom integrations need labels, links, parameters, names, descriptions, status details, or test plan identifiers in the same format as the built-in adapters.</p>
 */
@SuppressWarnings(
    {
            "ClassFanOutComplexity",
            "ClassDataAbstractionCoupling",
            "PMD.GodClass",
            "PMD.TooManyMethods",
    }
)
public final class ResultsUtils {

    /**
     * System property name for allure host name.
     */
    public static final String ALLURE_HOST_NAME_SYSPROP = "allure.hostName";

    /**
     * Environment variable name for allure host name.
     */
    public static final String ALLURE_HOST_NAME_ENV = "ALLURE_HOST_NAME";

    /**
     * System property name for allure thread name.
     */
    public static final String ALLURE_THREAD_NAME_SYSPROP = "allure.threadName";

    /**
     * Environment variable name for allure thread name.
     */
    public static final String ALLURE_THREAD_NAME_ENV = "ALLURE_THREAD_NAME";

    /**
     * System property name for allure separate lines.
     */
    public static final String ALLURE_SEPARATE_LINES_SYSPROP = "allure.description.javadoc.separateLines";

    /**
     * Allure link type for issue.
     */
    public static final String ISSUE_LINK_TYPE = "issue";

    /**
     * Allure link type for tms.
     */
    public static final String TMS_LINK_TYPE = "tms";

    /**
     * Allure link type for custom.
     */
    public static final String CUSTOM_LINK_TYPE = "custom";

    /**
     * Allure label name for allure id.
     */
    public static final String ALLURE_ID_LABEL_NAME = "AS_ID";

    /**
     * Allure label name for suite.
     */
    public static final String SUITE_LABEL_NAME = "suite";

    /**
     * Allure label name for parent suite.
     */
    public static final String PARENT_SUITE_LABEL_NAME = "parentSuite";

    /**
     * Allure label name for sub suite.
     */
    public static final String SUB_SUITE_LABEL_NAME = "subSuite";

    /**
     * Allure label name for epic.
     */
    public static final String EPIC_LABEL_NAME = "epic";

    /**
     * Allure label name for feature.
     */
    public static final String FEATURE_LABEL_NAME = "feature";

    /**
     * Allure label name for story.
     */
    public static final String STORY_LABEL_NAME = "story";

    /**
     * Allure label name for severity.
     */
    public static final String SEVERITY_LABEL_NAME = "severity";

    /**
     * Allure label name for tag.
     */
    public static final String TAG_LABEL_NAME = "tag";

    /**
     * Allure label name for owner.
     */
    public static final String OWNER_LABEL_NAME = "owner";

    /**
     * Allure label name for lead.
     */
    public static final String LEAD_LABEL_NAME = "lead";

    /**
     * Allure label name for host.
     */
    public static final String HOST_LABEL_NAME = "host";

    /**
     * Allure label name for thread.
     */
    public static final String THREAD_LABEL_NAME = "thread";

    /**
     * Allure label name for test method.
     */
    public static final String TEST_METHOD_LABEL_NAME = "testMethod";

    /**
     * Allure label name for test class.
     */
    public static final String TEST_CLASS_LABEL_NAME = "testClass";

    /**
     * Allure label name for package.
     */
    public static final String PACKAGE_LABEL_NAME = "package";

    /**
     * Allure label name for framework.
     */
    public static final String FRAMEWORK_LABEL_NAME = "framework";

    /**
     * Allure label name for language.
     */
    public static final String LANGUAGE_LABEL_NAME = "language";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsUtils.class);
    private static final String ALLURE_DESCRIPTIONS_FOLDER = "META-INF/allureDescriptions/";
    private static final String MD_5 = "MD5";
    private static final String DOT = ".";
    private static final String ACTUAL = "actual";
    private static final String EXPECTED = "expected";
    private static final String DEFINED_PROPERTY_SUFFIX = "Defined";
    private static final String NEW_LINE = "\n";

    private static String cachedHost;

    private ResultsUtils() {
        throw new IllegalStateException("Do not instance");
    }

    /**
     * Creates and returns the parameter.
     *
     * @param name the display name or logical name to use
     * @param value the value to set
     * @return the parameter
     */
    public static Parameter createParameter(final String name, final Object value) {
        return createParameter(name, value, null, null);
    }

    /**
     * Creates and returns the parameter.
     *
     * @param name the display name or logical name to use
     * @param value the value to set
     * @param excluded the excluded
     * @param mode the mode
     * @return the parameter
     */
    public static Parameter createParameter(final String name, final Object value,
                                            final Boolean excluded, final Parameter.Mode mode) {
        return new Parameter()
                .setName(name)
                .setValue(ObjectUtils.toString(value))
                .setExcluded(excluded)
                .setMode(mode);
    }

    /**
     * Creates and returns the suite label.
     *
     * @param suite the TestNG suite callback argument
     * @return the suite label
     */
    public static Label createSuiteLabel(final String suite) {
        return createLabel(SUITE_LABEL_NAME, suite);
    }

    /**
     * Creates and returns the parent suite label.
     *
     * @param suite the TestNG suite callback argument
     * @return the parent suite label
     */
    public static Label createParentSuiteLabel(final String suite) {
        return createLabel(PARENT_SUITE_LABEL_NAME, suite);
    }

    /**
     * Creates and returns the sub suite label.
     *
     * @param suite the TestNG suite callback argument
     * @return the sub suite label
     */
    public static Label createSubSuiteLabel(final String suite) {
        return createLabel(SUB_SUITE_LABEL_NAME, suite);
    }

    /**
     * Creates and returns the test method label.
     *
     * @param testMethod the test method
     * @return the test method label
     */
    public static Label createTestMethodLabel(final String testMethod) {
        return createLabel(TEST_METHOD_LABEL_NAME, testMethod);
    }

    /**
     * Creates and returns the test class label.
     *
     * @param testClass the test class
     * @return the test class label
     */
    public static Label createTestClassLabel(final String testClass) {
        return createLabel(TEST_CLASS_LABEL_NAME, testClass);
    }

    /**
     * Creates and returns the package label.
     *
     * @param packageName the package name
     * @return the package label
     */
    public static Label createPackageLabel(final String packageName) {
        return createLabel(PACKAGE_LABEL_NAME, packageName);
    }

    /**
     * Creates and returns the title path.
     *
     * @param values the values
     * @return the title path
     */
    public static List<String> createTitlePath(final String... values) {
        return createTitlePath(Arrays.asList(values));
    }

    /**
     * Creates and returns the title path.
     *
     * @param values the values
     * @return the title path
     */
    public static List<String> createTitlePath(final Collection<String> values) {
        if (Objects.isNull(values)) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Creates and returns the title path from package and class.
     *
     * @param packageName the package name
     * @param className the class name
     * @return the title path from package and class
     */
    public static List<String> createTitlePathFromPackageAndClass(final String packageName, final String className) {
        final List<String> result = createTitlePathFromPackage(packageName);
        if (result.isEmpty()) {
            return createTitlePathFromQualifiedClassName(className);
        }
        getClassTitle(String.join(DOT, result), className).ifPresent(result::add);
        return result;
    }

    /**
     * Creates and returns the title path from qualified class name.
     *
     * @param className the class name
     * @return the title path from qualified class name
     */
    public static List<String> createTitlePathFromQualifiedClassName(final String className) {
        return getPackageName(className)
                .map(packageName -> createTitlePathFromPackageAndClass(packageName, className))
                .orElseGet(() -> createTitlePath(className));
    }

    /**
     * Creates and returns the title path from java class.
     *
     * @param clazz the clazz
     * @return the title path from java class
     */
    public static List<String> createTitlePathFromJavaClass(final Class<?> clazz) {
        if (Objects.isNull(clazz)) {
            return createTitlePath();
        }
        final String packageName = Optional.ofNullable(clazz.getPackage())
                .map(Package::getName)
                .orElse("");
        return createTitlePathFromPackageAndClass(packageName, clazz.getName());
    }

    /**
     * Creates and returns the title path from package.
     *
     * @param packageName the package name
     * @return the title path from package
     */
    public static List<String> createTitlePathFromPackage(final String packageName) {
        return split(packageName, DOT);
    }

    /**
     * Creates and returns the title path from source path.
     *
     * @param sourcePath the source path
     * @return the title path from source path
     */
    public static List<String> createTitlePathFromSourcePath(final String sourcePath) {
        return split(normalizeSourcePath(sourcePath), "/");
    }

    /**
     * Creates and returns the epic label.
     *
     * @param epic the epic
     * @return the epic label
     */
    public static Label createEpicLabel(final String epic) {
        return createLabel(EPIC_LABEL_NAME, epic);
    }

    /**
     * Creates and returns the feature label.
     *
     * @param feature the feature
     * @return the feature label
     */
    public static Label createFeatureLabel(final String feature) {
        return createLabel(FEATURE_LABEL_NAME, feature);
    }

    /**
     * Creates and returns the story label.
     *
     * @param story the story
     * @return the story label
     */
    public static Label createStoryLabel(final String story) {
        return createLabel(STORY_LABEL_NAME, story);
    }

    /**
     * Creates and returns the tag label.
     *
     * @param tag the tag
     * @return the tag label
     */
    public static Label createTagLabel(final String tag) {
        return createLabel(TAG_LABEL_NAME, tag);
    }

    /**
     * Creates and returns the owner label.
     *
     * @param owner the owner
     * @return the owner label
     */
    public static Label createOwnerLabel(final String owner) {
        return createLabel(OWNER_LABEL_NAME, owner);
    }

    /**
     * Creates and returns the severity label.
     *
     * @param severity the severity
     * @return the severity label
     */
    public static Label createSeverityLabel(final SeverityLevel severity) {
        return createSeverityLabel(severity.value());
    }

    /**
     * Creates and returns the severity label.
     *
     * @param severity the severity
     * @return the severity label
     */
    public static Label createSeverityLabel(final String severity) {
        return createLabel(SEVERITY_LABEL_NAME, severity);
    }

    /**
     * Creates and returns the host label.
     *
     * @return the host label
     */
    public static Label createHostLabel() {
        return createLabel(HOST_LABEL_NAME, getHostName());
    }

    /**
     * Creates and returns the thread label.
     *
     * @return the thread label
     */
    public static Label createThreadLabel() {
        return createLabel(THREAD_LABEL_NAME, getThreadName());
    }

    /**
     * Creates and returns the framework label.
     *
     * @param framework the framework
     * @return the framework label
     */
    public static Label createFrameworkLabel(final String framework) {
        return createLabel(FRAMEWORK_LABEL_NAME, framework);
    }

    /**
     * Creates and returns the language label.
     *
     * @param language the language
     * @return the language label
     */
    public static Label createLanguageLabel(final String language) {
        return createLabel(LANGUAGE_LABEL_NAME, language);
    }

    /**
     * Creates and returns the label.
     *
     * @param name the display name or logical name to use
     * @param value the value to set
     * @return the label
     */
    public static Label createLabel(final String name, final String value) {
        return new Label().setName(name).setValue(value);
    }

    /**
     * Creates and returns the label.
     *
     * @param owner the owner
     * @return the label
     */
    public static Label createLabel(final Owner owner) {
        return createOwnerLabel(owner.value());
    }

    /**
     * Creates and returns the label.
     *
     * @param severity the severity
     * @return the label
     */
    public static Label createLabel(final Severity severity) {
        return createSeverityLabel(severity.value());
    }

    /**
     * Creates and returns the label.
     *
     * @param story the story
     * @return the label
     */
    public static Label createLabel(final Story story) {
        return createStoryLabel(story.value());
    }

    /**
     * Creates and returns the label.
     *
     * @param feature the feature
     * @return the label
     */
    public static Label createLabel(final Feature feature) {
        return createFeatureLabel(feature.value());
    }

    /**
     * Creates and returns the label.
     *
     * @param epic the epic
     * @return the label
     */
    public static Label createLabel(final Epic epic) {
        return createEpicLabel(epic.value());
    }

    /**
     * Creates and returns the issue link.
     *
     * @param value the value to set
     * @return the issue link
     */
    public static Link createIssueLink(final String value) {
        return createLink(value, null, null, ISSUE_LINK_TYPE);
    }

    /**
     * Creates and returns the tms link.
     *
     * @param value the value to set
     * @return the tms link
     */
    public static Link createTmsLink(final String value) {
        return createLink(value, null, null, TMS_LINK_TYPE);
    }

    /**
     * Creates and returns the link.
     *
     * @param link the link
     * @return the link
     */
    public static Link createLink(final io.qameta.allure.Link link) {
        return createLink(link.value(), link.name(), link.url(), link.type());
    }

    /**
     * Creates and returns the link.
     *
     * @param link the link
     * @return the link
     */
    public static Link createLink(final io.qameta.allure.Issue link) {
        return createIssueLink(link.value());
    }

    /**
     * Creates and returns the link.
     *
     * @param link the link
     * @return the link
     */
    public static Link createLink(final io.qameta.allure.TmsLink link) {
        return createTmsLink(link.value());
    }

    /**
     * Creates and returns the link.
     *
     * @param value the value to set
     * @param name the display name or logical name to use
     * @param url the request URL or service method name
     * @param type the event or label type
     * @return the link
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static Link createLink(final String value, final String name,
                                  final String url, final String type) {
        final String resolvedName = firstNonEmpty(value).orElse(name);
        final String resolvedUrl = firstNonEmpty(url)
                .orElseGet(
                        () -> isHttpOrHttpsUrl(resolvedName)
                                ? resolvedName
                                : getLinkUrl(resolvedName, type)
                );
        return new Link()
                .setName(resolvedName)
                .setUrl(resolvedUrl)
                .setType(type);
    }

    /**
     * Returns the provided labels.
     *
     * @return the provided labels
     */
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

    /**
     * Returns the host name.
     *
     * @return the host name
     */
    public static String getHostName() {
        final String fromProperty = System.getProperty(ALLURE_HOST_NAME_SYSPROP);
        final String fromEnv = System.getenv(ALLURE_HOST_NAME_ENV);
        return Stream.of(fromProperty, fromEnv)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(ResultsUtils::getRealHostName);
    }

    /**
     * Returns the thread name.
     *
     * @return the thread name
     */
    public static String getThreadName() {
        final String fromProperty = System.getProperty(ALLURE_THREAD_NAME_SYSPROP);
        final String fromEnv = System.getenv(ALLURE_THREAD_NAME_ENV);
        return Stream.of(fromProperty, fromEnv)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(ResultsUtils::getRealThreadName);
    }

    /**
     * Returns the status.
     *
     * @param throwable the throwable
     * @return the status
     */
    public static Optional<Status> getStatus(final Throwable throwable) {
        return Optional.ofNullable(throwable)
                .map(t -> t instanceof AssertionError ? Status.FAILED : Status.BROKEN);
    }

    /**
     * Returns the status details.
     *
     * @param e the e
     * @return the status details
     */
    public static Optional<StatusDetails> getStatusDetails(final Throwable e) {
        return Optional.ofNullable(e)
                .map(throwable -> {
                    final StatusDetails details = new StatusDetails()
                            .setMessage(
                                    Optional
                                            .ofNullable(throwable.getMessage())
                                            .orElse(throwable.getClass().getName())
                            )
                            .setTrace(getStackTraceAsString(throwable));
                    getRichErrorProperty(throwable, ACTUAL).ifPresent(details::setActual);
                    getRichErrorProperty(throwable, EXPECTED).ifPresent(details::setExpected);
                    return details;
                });
    }

    /**
     * Returns the javadoc description.
     *
     * @param classLoader the class loader to use for resource lookup
     * @param method the framework or Java method to inspect
     * @return the javadoc description
     */
    public static Optional<String> getJavadocDescription(final ClassLoader classLoader,
                                                         final Method method) {
        final String name = method.getName();
        final List<String> parameterTypes = Stream.of(method.getParameterTypes())
                .map(Class::getTypeName)
                .collect(Collectors.toList());

        final String signatureHash = generateMethodSignatureHash(
                method.getDeclaringClass().getName(),
                name,
                parameterTypes
        );

        return readResource(classLoader, ALLURE_DESCRIPTIONS_FOLDER + signatureHash)
                .map(desc -> separateLines() ? toMarkdownLineBreaks(desc) : desc);
    }

    /**
     * Returns the first non empty.
     *
     * @param items the map entries to convert
     * @return the first non empty
     */
    public static Optional<String> firstNonEmpty(final String... items) {
        return Stream.of(items)
                .filter(Objects::nonNull)
                .filter(item -> !item.isEmpty())
                .findFirst();
    }

    /**
     * Returns the link type pattern property name.
     *
     * @param type the event or label type
     * @return the link type pattern property name
     */
    public static String getLinkTypePatternPropertyName(final String type) {
        return String.format("allure.link.%s.pattern", type);
    }

    /**
     * Generates and returns the method signature hash.
     *
     * @param className the class name
     * @param methodName the method name
     * @param parameterTypes the parameter types
     * @return the generated method signature hash
     */
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

    /**
     * Returns the md5.
     *
     * @param source the source
     * @return the md5
     */
    public static String md5(final String source) {
        Objects.requireNonNull(source, "null source");
        return bytesToHex(getMd5Digest().digest(source.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Returns the lambda name.
     *
     * @param lambda the lambda
     * @return the lambda name
     */
    public static Optional<String> getLambdaName(final Object lambda) {
        return getSerializedLambda(lambda)
                .flatMap(ResultsUtils::formatLambdaName);
    }

    /**
     * Returns the bytes to hex.
     *
     * @param bytes the bytes
     * @return the bytes to hex
     */
    public static String bytesToHex(final byte[] bytes) {
        return new BigInteger(1, bytes).toString(16);
    }

    /**
     * Returns the md5 digest.
     *
     * @return the md5 digest
     */
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

    private static boolean isHttpOrHttpsUrl(final String value) {
        if (Objects.isNull(value)) {
            return false;
        }

        try {
            final URI uri = URI.create(value);
            final String scheme = uri.getScheme();
            return nonNull(uri.getHost())
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String getRealHostName() {
        if (Objects.isNull(cachedHost)) {
            try {
                cachedHost = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOGGER.debug("Could not get host name", e);
                cachedHost = "default";
            }
        }
        return cachedHost;
    }

    private static String getRealThreadName() {
        return String.format(
                "%s.%s(%s)",
                ManagementFactory.getRuntimeMXBean().getName(),
                Thread.currentThread().getName(),
                Thread.currentThread().getId()
        );
    }

    private static String getStackTraceAsString(final Throwable throwable) {
        final StringWriter stringWriter = new StringWriter();
        try {
            throwable.printStackTrace(new PrintWriter(stringWriter));
        } catch (RuntimeException e) {
            if (stringWriter.getBuffer().length() == 0) {
                stringWriter.append(throwable.getClass().getName());
            }
            stringWriter
                    .append(System.lineSeparator())
                    .append("[Unable to render the complete stack trace: ")
                    .append(e.getClass().getName())
                    .append(']');
        }
        return stringWriter.toString();
    }

    private static Optional<String> getRichErrorProperty(final Throwable throwable, final String propertyName) {
        final Boolean propertyDefined = ReflectionUtils.getBooleanValue(
                throwable,
                propertyName + DEFINED_PROPERTY_SUFFIX
        );
        // Some assertion libraries expose isActualDefined/isExpectedDefined to distinguish absent values
        // from values that are present but null.
        if (Boolean.FALSE.equals(propertyDefined)) {
            return Optional.empty();
        }
        final Object value = ReflectionUtils.getValue(throwable, propertyName);
        return Optional.ofNullable(value).map(ObjectUtils::toString);
    }

    /**
     * Handles the process description callback.
     *
     * @param classLoader the class loader to use for resource lookup
     * @param method the framework or Java method to inspect
     * @param setDescription the set description
     * @param setDescriptionHtml the set description html
     */
    public static void processDescription(final ClassLoader classLoader,
                                          final Method method,
                                          final Consumer<String> setDescription,
                                          final Consumer<String> setDescriptionHtml) {
        if (method.isAnnotationPresent(Description.class)) {
            final Description annotation = method.getAnnotation(Description.class);
            if ("".equals(annotation.value())) {
                getJavadocDescription(classLoader, method)
                        .ifPresent(setDescription);
            } else {
                final String description = annotation.value();
                setDescription.accept(description);
            }
        }
    }

    private static String toMarkdownLineBreaks(final String description) {
        final String[] lines = description.split(NEW_LINE, -1);
        final StringBuilder markdown = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                final String previousLine = lines[index - 1];
                final String currentLine = lines[index];
                markdown.append(previousLine.isEmpty() || currentLine.isEmpty() ? NEW_LINE : "  \n");
            }
            markdown.append(lines[index]);
        }
        return markdown.toString();
    }

    private static Optional<String> readResource(final ClassLoader classLoader, final String resourceName) {
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            if (Objects.isNull(is)) {
                return Optional.empty();
            }
            final byte[] bytes = toBytes(is);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.warn("Unable to process description resource file", e);
        }
        return Optional.empty();
    }

    private static byte[] toBytes(final InputStream is) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int n;
        do {
            n = is.read(buffer);
            if (n > 0) {
                output.write(buffer, 0, n);
            }
        } while (-1 != n);
        return output.toByteArray();
    }

    private static boolean separateLines() {
        return parseBoolean(loadAllureProperties().getProperty(ALLURE_SEPARATE_LINES_SYSPROP));
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static Optional<SerializedLambda> getSerializedLambda(final Object value) {
        if (Objects.isNull(value)) {
            return Optional.empty();
        }
        try {
            final Method writeReplace = value.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            final Object replacement = writeReplace.invoke(value);
            if (replacement instanceof SerializedLambda) {
                return Optional.of((SerializedLambda) replacement);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<String> formatLambdaName(final SerializedLambda lambda) {
        final String methodName = lambda.getImplMethodName();
        if (methodName.startsWith("lambda$")) {
            return Optional.empty();
        }
        return Optional.of(simpleClassName(lambda.getImplClass()) + "::" + getLambdaMethodName(methodName));
    }

    private static Optional<String> getPackageName(final String className) {
        if (Objects.isNull(className)) {
            return Optional.empty();
        }
        final int index = className.lastIndexOf('.');
        if (index < 0) {
            return Optional.empty();
        }
        return Optional.of(className.substring(0, index));
    }

    private static Optional<String> getClassTitle(final String packageName, final String className) {
        if (Objects.isNull(className)) {
            return Optional.empty();
        }
        final String trimmedClassName = className.trim();
        if (trimmedClassName.isEmpty()) {
            return Optional.empty();
        }
        final String prefix = packageName + DOT;
        if (!packageName.isEmpty() && trimmedClassName.startsWith(prefix)) {
            return Optional.of(trimmedClassName.substring(prefix.length()));
        }
        return Optional.of(trimmedClassName);
    }

    private static List<String> split(final String value, final String delimiter) {
        if (Objects.isNull(value) || value.isEmpty()) {
            return new ArrayList<>();
        }
        return Stream.of(value.split(Pattern.quote(delimiter)))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    private static String normalizeSourcePath(final String sourcePath) {
        if (Objects.isNull(sourcePath)) {
            return "";
        }
        try {
            final URI uri = URI.create(sourcePath);
            if (nonNull(uri.getScheme())) {
                final URI normalized = uri.normalize();
                if (!normalized.isOpaque()) {
                    final URI relative = new File("").toURI().relativize(normalized);
                    final String path = relative.getPath();
                    if (nonNull(path) && !path.isEmpty()) {
                        return path.replace('\\', '/');
                    }
                }
                final String schemeSpecificPart = normalized.getSchemeSpecificPart();
                if (nonNull(schemeSpecificPart)) {
                    return schemeSpecificPart.replace('\\', '/');
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to plain path normalization below.
        }
        return sourcePath.replace('\\', '/');
    }

    private static String getLambdaMethodName(final String methodName) {
        if ("<init>".equals(methodName)) {
            return "new";
        }
        return methodName;
    }

    private static String simpleClassName(final String name) {
        final String normalized = name.replace('/', '.');
        final int packageIndex = normalized.lastIndexOf('.');
        final int nestedClassIndex = normalized.lastIndexOf('$');
        final int index = Math.max(packageIndex, nestedClassIndex);
        return index < 0 ? normalized : normalized.substring(index + 1);
    }

}
