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
package io.qameta.allure.junitplatform;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.AnnotationUtils;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static io.qameta.allure.model.Status.SKIPPED;
import static io.qameta.allure.util.ResultsUtils.ALLURE_ID_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createTestClassLabel;
import static io.qameta.allure.util.ResultsUtils.createTestMethodLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.getMd5Digest;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Reports JUnit Platform execution to Allure.
 *
 * <p>Register this listener with the JUnit Platform launcher to translate test plan, container, test, report-entry, and failure events into Allure results. It is the core listener used by Platform-based Allure adapters.</p>
 *
 * <p>Scopes and tests are addressed by lifecycle keys recomputed from JUnit Platform unique ids — see
 * {@link #scopeKey(String)} and {@link #testKey(String)} — so companion integrations such as the Jupiter extension
 * can enrich them through the Allure lifecycle directly, without a shared registry.</p>
 */
@SuppressWarnings(
    {
            "ClassDataAbstractionCoupling",
            "ClassFanOutComplexity",
            "MultipleStringLiterals",
            "PMD.GodClass",
            "PMD.TooManyMethods",
    }
)
public class AllureJunitPlatform implements TestExecutionListener {

    /**
     * Configuration key for allure report entry blank prefix.
     */
    public static final String ALLURE_REPORT_ENTRY_BLANK_PREFIX = "ALLURE_REPORT_ENTRY_BLANK_PREFIX__";

    /**
     * Configuration key for allure parameter.
     */
    public static final String ALLURE_PARAMETER = "allure.parameter";

    /**
     * Configuration key for allure parameter value key.
     */
    public static final String ALLURE_PARAMETER_VALUE_KEY = "value";

    /**
     * Configuration key for allure parameter mode key.
     */
    public static final String ALLURE_PARAMETER_MODE_KEY = "mode";

    /**
     * Configuration key for allure parameter excluded key.
     */
    public static final String ALLURE_PARAMETER_EXCLUDED_KEY = "excluded";

    /**
     * Constant value for junit platform unique id.
     */
    public static final String JUNIT_PLATFORM_UNIQUE_ID = "junit.platform.uniqueid";
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureJunitPlatform.class);
    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";
    private static final String TEXT_PLAIN = "text/plain";

    private static final String TEST_TEMPLATE_INVOCATION_SEGMENT = "test-template-invocation";
    private static final String CLASS_TEMPLATE_INVOCATION_SEGMENT = "class-template-invocation";

    private static final boolean HAS_SPOCK2_IN_CLASSPATH = isClassAvailableOnClasspath("io.qameta.allure.spock2.AllureSpock2");

    private static final boolean HAS_CUCUMBERJVM7_IN_CLASSPATH = isClassAvailableOnClasspath("io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm");

    private static final String ENGINE_SPOCK2 = "spock";
    private static final String ENGINE_CUCUMBER = "cucumber";

    private final ThreadLocal<TestPlan> testPlanStorage = new InheritableThreadLocal<>();

    private final AllureLifecycle lifecycle;

    /**
     * Creates an Allure junit platform with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public AllureJunitPlatform(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * Creates an Allure junit platform with default configuration.
     */
    public AllureJunitPlatform() {
        this.lifecycle = Allure.getLifecycle();
    }

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Returns the lifecycle key of the Allure scope registered for the JUnit Platform node with the given unique id.
     *
     * <p>The key is recomputable: any code that knows the unique id of a running node — such as the Jupiter
     * extension reporting fixtures — addresses the same scope without access to this listener. Unique ids are
     * unique within a test plan, so the key is unique per live scope of a single launch.</p>
     *
     * @param uniqueId the JUnit Platform unique id of the node
     * @return the scope key
     */
    public static AllureExternalKey scopeKey(final String uniqueId) {
        return AllureExternalKey.of(AllureJunitPlatform.class, "scope", uniqueId);
    }

    /**
     * Returns the lifecycle key of the Allure test started for the JUnit Platform node with the given unique id.
     *
     * <p>The key is recomputable: any code that knows the unique id of a running node — such as the Jupiter
     * extension reporting parameters — addresses the same test without access to this listener. Unique ids are
     * unique within a test plan, so the key is unique per live test of a single launch.</p>
     *
     * @param uniqueId the JUnit Platform unique id of the node
     * @return the test key
     */
    public static AllureExternalKey testKey(final String uniqueId) {
        return AllureExternalKey.of(AllureJunitPlatform.class, "test", uniqueId);
    }

    @SuppressWarnings({"CyclomaticComplexity", "BooleanExpressionComplexity"})
    private boolean shouldSkipReportingFor(final TestIdentifier testIdentifier) {
        // Always skip root
        if (!testIdentifier.getParentId().isPresent()) {
            return true;
        }

        final Optional<String> maybeEngine = getEngine(testIdentifier);
        // can't find the engine, don't know if it's possible but just in case
        // keep reporting such nodes.
        if (!maybeEngine.isPresent()) {
            return false;
        }

        final String engine = maybeEngine.get();

        return HAS_SPOCK2_IN_CLASSPATH && ENGINE_SPOCK2.equals(engine)
                || HAS_CUCUMBERJVM7_IN_CLASSPATH && ENGINE_CUCUMBER.equals(engine);
    }

    private Optional<String> getEngine(final TestIdentifier testIdentifier) {
        final UniqueId uniqueId = testIdentifier.getUniqueIdObject();
        final List<UniqueId.Segment> segments = uniqueId.getSegments();
        // since junit-platform-suite engine creates nested engine segments
        // we need to lookup for the last one with type engine
        // to determinate the actual used engine:
        // [engine:junit-platform-suite]/[suite:org.example.JUnitRunnerTest]/[engine:cucumber]/...
        for (int i = segments.size() - 1; i >= 0; i--) {
            final UniqueId.Segment segment = segments.get(i);
            if ("engine".equals(segment.getType())) {
                return Optional.of(segment.getValue());
            }
        }
        return Optional.empty();
    }

    private static boolean isClassAvailableOnClasspath(final String clazz) {
        try {
            AllureJunitPlatform.class.getClassLoader().loadClass(clazz);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testPlanExecutionStarted(final TestPlan testPlan) {
        testPlanStorage.set(testPlan);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testPlanExecutionFinished(final TestPlan testPlan) {
        testPlanStorage.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        if (shouldSkipReportingFor(testIdentifier)) {
            return;
        }
        // register a scope for every TestIdentifier. We need scopes for tests in order
        // to support method fixtures.
        getLifecycle().registerScope(scopeKey(testIdentifier.getUniqueId()));

        if (testIdentifier.isTest()) {
            final List<AllureExternalKey> scopeKeys = getParentScopeKeys(testIdentifier);
            scopeKeys.add(scopeKey(testIdentifier.getUniqueId()));
            startTest(testIdentifier, scopeKeys);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executionFinished(final TestIdentifier testIdentifier,
                                  final TestExecutionResult testExecutionResult) {
        if (shouldSkipReportingFor(testIdentifier)) {
            return;
        }
        final Status status = extractStatus(testExecutionResult);
        final StatusDetails statusDetails = testExecutionResult.getThrowable()
                .flatMap(ResultsUtils::getStatusDetails)
                .orElse(null);

        if (testIdentifier.isTest()) {
            stopTest(testIdentifier, status, statusDetails);
        } else if (testExecutionResult.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
            // report failed containers as fake test results, linked to their own scope only
            startTest(testIdentifier, Collections.singletonList(scopeKey(testIdentifier.getUniqueId())));
            stopTest(testIdentifier, status, statusDetails);
        }
        getLifecycle().writeScope(scopeKey(testIdentifier.getUniqueId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executionSkipped(final TestIdentifier testIdentifier,
                                 final String reason) {
        if (shouldSkipReportingFor(testIdentifier)) {
            return;
        }
        final TestPlan testPlan = testPlanStorage.get();
        if (Objects.isNull(testPlan)) {
            return;
        }
        // only ancestors of the skipped node have running scopes: nodes inside the skipped
        // subtree never start, so their scopes are never registered
        final List<AllureExternalKey> scopeKeys = getParentScopeKeys(testIdentifier);
        reportNested(
                testPlan,
                testIdentifier,
                SKIPPED,
                new StatusDetails().setMessage(reason),
                new HashSet<>(),
                scopeKeys
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reportingEntryPublished(final TestIdentifier testIdentifier,
                                        final ReportEntry entry) {
        if (shouldSkipReportingFor(testIdentifier)) {
            return;
        }

        final Map<String, String> keyValuePairs = entry.getKeyValuePairs();
        if (keyValuePairs.containsKey(ALLURE_PARAMETER)) {
            processParameterEvent(keyValuePairs);
            return;
        }

        // captured output is genuinely ambient: it lands under whatever executable is current,
        // and is silently skipped (unsupported executables) — no key to address it by
        final AllureLifecycle lifecycle = getLifecycle();
        lifecycle.getCurrentExecutableKey().ifPresent(key -> {
            if (keyValuePairs.containsKey(STDOUT)) {
                lifecycle.addAttachment(
                        key,
                        "Stdout",
                        TEXT_PLAIN,
                        new ByteArrayInputStream(keyValuePairs.getOrDefault(STDOUT, "").getBytes(UTF_8)),
                        AttachmentOptions.empty()
                );
            }
            if (keyValuePairs.containsKey(STDERR)) {
                lifecycle.addAttachment(
                        key,
                        "Stderr",
                        TEXT_PLAIN,
                        new ByteArrayInputStream(keyValuePairs.getOrDefault(STDERR, "").getBytes(UTF_8)),
                        AttachmentOptions.empty()
                );
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fileEntryPublished(final TestIdentifier testIdentifier,
                                   final FileEntry file) {
        if (shouldSkipReportingFor(testIdentifier)) {
            return;
        }
        final Path path = file.getPath();
        if (!Files.isRegularFile(path)) {
            // TestReporter#publishDirectory entries are not supported
            LOGGER.debug("skip published file entry {}: not a regular file", path);
            return;
        }
        // published files are ambient, same as captured output: they land under
        // whatever executable is current on the publishing thread
        getLifecycle().getCurrentExecutableKey().ifPresent(key -> {
            final String fileName = String.valueOf(path.getFileName());
            try (InputStream content = Files.newInputStream(path)) {
                getLifecycle().addAttachment(
                        key,
                        fileName,
                        file.getMediaType().orElse(null),
                        content,
                        attachmentOptions(fileName)
                );
            } catch (IOException e) {
                LOGGER.warn("could not attach published file entry {}", path, e);
            }
        });
    }

    private static AttachmentOptions attachmentOptions(final String fileName) {
        final int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 && dotIndex < fileName.length() - 1
                ? AttachmentOptions.withFileExtension(fileName.substring(dotIndex + 1))
                : AttachmentOptions.empty();
    }

    private void processParameterEvent(final Map<String, String> keyValuePairs) {
        final String name = keyValuePairs.get(ALLURE_PARAMETER);
        final String value = keyValuePairs.get(ALLURE_PARAMETER_VALUE_KEY);
        final Parameter parameter;
        if (Objects.nonNull(value) && value.startsWith(ALLURE_REPORT_ENTRY_BLANK_PREFIX)) {
            parameter = ResultsUtils.createParameter(
                    name,
                    value.substring(ALLURE_REPORT_ENTRY_BLANK_PREFIX.length())
            );
        } else {
            parameter = ResultsUtils.createParameter(name, value);
        }
        if (keyValuePairs.containsKey(ALLURE_PARAMETER_MODE_KEY)) {
            final String modeString = keyValuePairs.get(ALLURE_PARAMETER_MODE_KEY);
            Stream.of(Parameter.Mode.values())
                    .filter(mode -> mode.name().equalsIgnoreCase(modeString))
                    .findAny()
                    .ifPresent(parameter::setMode);
        }
        if (keyValuePairs.containsKey(ALLURE_PARAMETER_EXCLUDED_KEY)) {
            final String excludedString = keyValuePairs.get(ALLURE_PARAMETER_EXCLUDED_KEY);
            Optional.ofNullable(excludedString)
                    .map(Boolean::parseBoolean)
                    .ifPresent(parameter::setExcluded);
        }

        getLifecycle().updateTest(
                tr -> tr.getParameters()
                        .add(parameter)
        );
    }

    private void reportNested(final TestPlan testPlan,
                              final TestIdentifier testIdentifier,
                              final Status status,
                              final StatusDetails statusDetails,
                              final Set<TestIdentifier> visited,
                              final List<AllureExternalKey> scopeKeys) {
        final Set<TestIdentifier> children = testPlan.getChildren(testIdentifier);
        if (testIdentifier.isTest() || children.isEmpty()) {
            startTest(testIdentifier, scopeKeys);
            stopTest(testIdentifier, status, statusDetails);
        }
        visited.add(testIdentifier);
        children.stream()
                .filter(id -> !visited.contains(id))
                .forEach(child -> reportNested(testPlan, child, status, statusDetails, visited, scopeKeys));
    }

    /**
     * Returns the status.
     *
     * @param throwable the throwable
     * @return the status
     */
    protected Status getStatus(final Throwable throwable) {
        return ResultsUtils.getStatus(throwable).orElse(FAILED);
    }

    private List<AllureExternalKey> getParentScopeKeys(final TestIdentifier testIdentifier) {
        return getParents(testIdentifier).stream()
                // roots are engines: reporting for them is skipped, so they have no scopes
                .filter(parent -> parent.getParentId().isPresent())
                .map(parent -> scopeKey(parent.getUniqueId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String getName(final TestIdentifier testIdentifier,
                           final boolean testTemplate,
                           final Optional<TestIdentifier> maybeParent) {
        final String baseName = testTemplate && maybeParent.isPresent()
                ? maybeParent.get().getDisplayName() + " " + testIdentifier.getDisplayName()
                : testIdentifier.getDisplayName();
        // prefix the name with parameterized class invocation display names, so results
        // of the same method from different invocations are distinguishable
        final String prefix = getParents(testIdentifier).stream()
                .filter(
                        parent -> CLASS_TEMPLATE_INVOCATION_SEGMENT
                                .equals(parent.getUniqueIdObject().getLastSegment().getType())
                )
                .map(TestIdentifier::getDisplayName)
                .collect(Collectors.joining(" "));
        return prefix.isEmpty() ? baseName : prefix + " " + baseName;
    }

    /**
     * Returns the test case id — the identity of the logical test case shared by every invocation: the unique id
     * with all template invocation segments removed.
     *
     * @param uniqueId the unique id of the started test
     * @return the test case id
     */
    private static String getTestCaseId(final UniqueId uniqueId) {
        final List<UniqueId.Segment> segments = uniqueId.getSegments();
        final List<UniqueId.Segment> kept = segments.stream()
                .filter(segment -> !isInvocationSegment(segment))
                .collect(Collectors.toList());
        if (kept.size() == segments.size()) {
            return uniqueId.toString();
        }
        UniqueId testCaseId = UniqueId.root(kept.get(0).getType(), kept.get(0).getValue());
        for (int i = 1; i < kept.size(); i++) {
            testCaseId = testCaseId.append(kept.get(i));
        }
        return testCaseId.toString();
    }

    private static boolean isInvocationSegment(final UniqueId.Segment segment) {
        return TEST_TEMPLATE_INVOCATION_SEGMENT.equals(segment.getType())
                || CLASS_TEMPLATE_INVOCATION_SEGMENT.equals(segment.getType());
    }

    private void startTest(final TestIdentifier testIdentifier,
                           final List<AllureExternalKey> scopeKeys) {
        final Optional<TestSource> testSource = testIdentifier.getSource();
        final Optional<Method> testMethod = testSource
                .flatMap(AllureJunitPlatformUtils::getTestMethod);
        final Optional<Class<?>> testClass = testSource
                .flatMap(AllureJunitPlatformUtils::getTestClass);

        final UniqueId uniqueId = testIdentifier.getUniqueIdObject();
        final boolean testTemplate = TEST_TEMPLATE_INVOCATION_SEGMENT
                .equals(uniqueId.getLastSegment().getType());
        final boolean parameterized = uniqueId.getSegments().stream()
                .anyMatch(AllureJunitPlatform::isInvocationSegment);

        final Optional<TestIdentifier> maybeParent = Optional.of(testPlanStorage)
                .map(ThreadLocal::get)
                .flatMap(tp -> tp.getParent(testIdentifier));

        final TestResult result = new TestResult()
                .setName(getName(testIdentifier, testTemplate, maybeParent))
                .setTitlePath(getTitlePath(testIdentifier, testClass))
                .setLabels(getTags(testIdentifier))
                .setTestCaseId(getTestCaseId(uniqueId))
                .setTestCaseName(
                        testTemplate
                                ? maybeParent.map(TestIdentifier::getDisplayName)
                                        .orElseGet(testIdentifier::getDisplayName)
                                : testIdentifier.getDisplayName()
                )
                .setHistoryId(getHistoryId(testIdentifier));

        if (parameterized) {
            // history id is ignored in Allure TestOps, so we add a hidden parameter
            // to make sure results of different invocations are not considered as retries
            result.getParameters().add(
                    new Parameter()
                            .setMode(Parameter.Mode.HIDDEN)
                            .setName("UniqueId")
                            .setValue(testIdentifier.getUniqueId())
            );
        }

        result.getLabels().addAll(getProvidedLabels());

        result.getLabels().add(getJUnitPlatformUniqueId(testIdentifier));

        // add annotations from outer classes (support for @Nested tests in JUnit 5)
        testClass.ifPresent(clazz -> {
            Class<?> clazz1 = clazz;
            do {
                final Set<Label> labels = AnnotationUtils.getLabels(clazz1);
                result.getLabels().addAll(labels);
                clazz1 = clazz1.getDeclaringClass();
            } while (Objects.nonNull(clazz1));
        });

        testMethod.map(AnnotationUtils::getLabels).ifPresent(result.getLabels()::addAll);

        testClass.map(AnnotationUtils::getLinks).ifPresent(result.getLinks()::addAll);
        testMethod.map(AnnotationUtils::getLinks).ifPresent(result.getLinks()::addAll);

        result.getLabels().addAll(
                Arrays.asList(
                        createHostLabel(),
                        createThreadLabel(),
                        createFrameworkLabel("junit-platform"),
                        createLanguageLabel("java")
                )
        );

        testSource.flatMap(AllureJunitPlatformUtils::getFullName).ifPresent(result::setFullName);
        testSource.map(this::getSourceLabels).ifPresent(result.getLabels()::addAll);
        testClass.ifPresent(aClass -> {
            final String suiteName = getDisplayName(aClass).orElse(aClass.getCanonicalName());
            result.getLabels().add(createSuiteLabel(suiteName));
        });

        final Optional<String> classDescription = testClass.flatMap(this::getDescription);
        final Optional<String> methodDescription = testMethod.flatMap(this::getDescription);

        final String description = Stream.of(classDescription, methodDescription)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n\n"));

        result.setDescription(description);

        testMethod.map(this::getSeverity)
                .filter(Optional::isPresent)
                .orElse(testClass.flatMap(this::getSeverity))
                .map(ResultsUtils::createSeverityLabel)
                .ifPresent(result.getLabels()::add);

        testMethod.ifPresent(
                method -> ResultsUtils.processDescription(
                        method.getDeclaringClass().getClassLoader(),
                        method,
                        result::setDescription,
                        result::setDescriptionHtml
                )
        );

        final AllureExternalKey testKey = testKey(testIdentifier.getUniqueId());
        getLifecycle().scheduleTest(scopeKeys, testKey, result);
        getLifecycle().startTest(testKey);
    }

    private void stopTest(final TestIdentifier testIdentifier,
                          final Status status,
                          final StatusDetails statusDetails) {
        final AllureExternalKey testKey = testKey(testIdentifier.getUniqueId());
        getLifecycle().updateTest(testKey, result -> {
            if (!testIdentifier.isTest()) {
                result.getLabels().add(new Label().setName(ALLURE_ID_LABEL_NAME).setValue("-1"));
            }
            result.setStatus(status);

            final StatusDetails currentSd = result.getStatusDetails();
            if (Objects.isNull(currentSd)) {
                result.setStatusDetails(statusDetails);
            } else if (Objects.nonNull(statusDetails)) {
                Optional.of(statusDetails)
                        .map(StatusDetails::getMessage)
                        .ifPresent(currentSd::setMessage);

                Optional.of(statusDetails)
                        .map(StatusDetails::getTrace)
                        .ifPresent(currentSd::setTrace);

                Optional.of(statusDetails)
                        .map(StatusDetails::getActual)
                        .ifPresent(currentSd::setActual);

                Optional.of(statusDetails)
                        .map(StatusDetails::getExpected)
                        .ifPresent(currentSd::setExpected);

                currentSd.setMuted(currentSd.isMuted() || statusDetails.isMuted());
                currentSd.setFlaky(currentSd.isFlaky() || statusDetails.isFlaky());
                currentSd.setKnown(currentSd.isKnown() || statusDetails.isKnown());
            }
        });
        getLifecycle().stopTest(testKey);
        getLifecycle().writeTest(testKey);
    }

    private Status extractStatus(final TestExecutionResult testExecutionResult) {
        switch (testExecutionResult.getStatus()) {
            case FAILED:
                return testExecutionResult.getThrowable().isPresent()
                        ? getStatus(testExecutionResult.getThrowable().get())
                        : FAILED;
            case SUCCESSFUL:
                return PASSED;
            default:
                return SKIPPED;
        }
    }

    private List<Label> getTags(final TestIdentifier testIdentifier) {
        return testIdentifier.getTags().stream()
                .map(TestTag::getName)
                .map(ResultsUtils::createTagLabel)
                .collect(Collectors.toList());
    }

    private List<String> getTitlePath(final TestIdentifier testIdentifier,
                                      final Optional<Class<?>> testClass) {
        final List<String> result = testClass
                .map(this::getClassTitlePath)
                .orElseGet(ArrayList::new);

        getParents(testIdentifier).stream()
                .filter(parent -> !"engine".equals(parent.getUniqueIdObject().getLastSegment().getType()))
                .filter(parent -> !parent.isTest())
                .filter(parent -> !parent.getSource().filter(ClassSource.class::isInstance).isPresent())
                .map(TestIdentifier::getDisplayName)
                .forEach(result::add);

        return ResultsUtils.createTitlePath(result);
    }

    private List<String> getClassTitlePath(final Class<?> testClass) {
        final String packageName = Optional.ofNullable(testClass.getPackage())
                .map(Package::getName)
                .orElse("");
        final List<String> result = ResultsUtils.createTitlePathFromPackage(packageName);
        final List<String> classNames = new ArrayList<>();
        Class<?> current = testClass;
        while (Objects.nonNull(current)) {
            classNames.add(getDisplayName(current).orElse(current.getSimpleName()));
            current = current.getDeclaringClass();
        }
        Collections.reverse(classNames);
        result.addAll(classNames);
        return result;
    }

    private List<TestIdentifier> getParents(final TestIdentifier testIdentifier) {
        final List<TestIdentifier> result = new ArrayList<>();
        Optional<TestIdentifier> parent = Optional.ofNullable(testPlanStorage.get())
                .flatMap(testPlan -> testPlan.getParent(testIdentifier));
        while (parent.isPresent()) {
            final TestIdentifier value = parent.get();
            result.add(value);
            parent = Optional.ofNullable(testPlanStorage.get())
                    .flatMap(testPlan -> testPlan.getParent(value));
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Returns the history id.
     *
     * @param testIdentifier the test identifier
     * @return the history id
     */
    protected String getHistoryId(final TestIdentifier testIdentifier) {
        return md5(testIdentifier.getUniqueId());
    }

    private String md5(final String source) {
        final byte[] bytes = getMd5Digest().digest(source.getBytes(UTF_8));
        return new BigInteger(1, bytes).toString(16);
    }

    private Optional<SeverityLevel> getSeverity(final AnnotatedElement annotatedElement) {
        return getAnnotations(annotatedElement, Severity.class)
                .map(Severity::value)
                .findAny();
    }

    private Optional<String> getDisplayName(final AnnotatedElement annotatedElement) {
        return getAnnotations(annotatedElement, DisplayName.class)
                .map(DisplayName::value)
                .findAny();
    }

    private Optional<String> getDescription(final AnnotatedElement annotatedElement) {
        return getAnnotations(annotatedElement, Description.class)
                .map(Description::value)
                .findAny();
    }

    private <T extends Annotation> Stream<T> getAnnotations(final AnnotatedElement annotatedElement,
                                                            final Class<T> annotationClass) {
        return Stream.of(annotatedElement.getAnnotationsByType(annotationClass));
    }

    private List<Label> getSourceLabels(final TestSource source) {
        if (source instanceof MethodSource) {
            final MethodSource ms = (MethodSource) source;
            return Arrays.asList(
                    createPackageLabel(ms.getClassName()),
                    createTestClassLabel(ms.getClassName()),
                    createTestMethodLabel(ms.getMethodName())
            );
        }
        if (source instanceof ClassSource) {
            final ClassSource cs = (ClassSource) source;
            return Arrays.asList(
                    createPackageLabel(cs.getClassName()),
                    createTestClassLabel(cs.getClassName())
            );
        }
        return Collections.emptyList();
    }

    private Label getJUnitPlatformUniqueId(final TestIdentifier testIdentifier) {
        final Label label = new Label();
        label.setName(JUNIT_PLATFORM_UNIQUE_ID);
        label.setValue(testIdentifier.getUniqueId());
        return label;
    }

}
