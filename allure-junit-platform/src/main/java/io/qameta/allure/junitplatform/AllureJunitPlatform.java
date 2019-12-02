/*
 *  Copyright 2019 Qameta Software OÜ
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
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.util.AnnotationUtils;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static io.qameta.allure.model.Status.SKIPPED;
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
 * @author ehborisov
 */
@SuppressWarnings({"deprecation", "ClassFanOutComplexity", "MultipleStringLiterals", "PMD.GodClass"})
public class AllureJunitPlatform implements TestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureJunitPlatform.class);

    public static final String ALLURE_FIXTURE = "allure.fixture";

    public static final String PREPARE = "prepare";
    public static final String TEAR_DOWN = "tear_down";

    public static final String EVENT_START = "start";
    public static final String EVENT_STOP = "stop";
    public static final String EVENT_FAILURE = "failure";

    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String TXT_EXTENSION = ".txt";

    private final ThreadLocal<TestPlan> testPlanStorage = new InheritableThreadLocal<>();

    private final Uuids tests = new Uuids();
    private final Uuids containers = new Uuids();

    private final AllureLifecycle lifecycle;

    public AllureJunitPlatform(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureJunitPlatform() {
        this.lifecycle = Allure.getLifecycle();
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void testPlanExecutionStarted(final TestPlan testPlan) {
        testPlanStorage.set(testPlan);
    }

    @Override
    public void testPlanExecutionFinished(final TestPlan testPlan) {
        testPlanStorage.remove();
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        // skip root
        if (!testIdentifier.getParentId().isPresent()) {
            return;
        }
        // create container for every TestIdentifier. We need containers for tests in order
        // to support method fixtures.
        startTestContainer(testIdentifier);

        if (testIdentifier.isTest()) {
            startTestCase(testIdentifier);
        }
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier,
                                  final TestExecutionResult testExecutionResult) {
        // skip root
        if (!testIdentifier.getParentId().isPresent()) {
            return;
        }
        final Status status = extractStatus(testExecutionResult);
        final StatusDetails statusDetails = testExecutionResult.getThrowable()
                .flatMap(ResultsUtils::getStatusDetails)
                .orElse(null);

        if (testIdentifier.isTest()) {
            stopTestCase(testIdentifier, status, statusDetails);
        } else if (testExecutionResult.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
            // report failed containers as fake test results
            startTestCase(testIdentifier);
            stopTestCase(testIdentifier, status, statusDetails);
        }
        stopTestContainer(testIdentifier);
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier,
                                 final String reason) {
        // skip root
        if (!testIdentifier.getParentId().isPresent()) {
            return;
        }
        final TestPlan testPlan = testPlanStorage.get();
        if (Objects.isNull(testPlan)) {
            return;
        }
        reportNested(
                testPlan,
                testIdentifier,
                SKIPPED,
                new StatusDetails().setMessage(reason),
                new HashSet<>()
        );
    }

    @SuppressWarnings({"ReturnCount", "PMD.NcssCount"})
    @Override
    public void reportingEntryPublished(final TestIdentifier testIdentifier,
                                        final ReportEntry entry) {
        final Map<String, String> keyValuePairs = entry.getKeyValuePairs();
        if (keyValuePairs.containsKey(ALLURE_FIXTURE)) {
            final String type = keyValuePairs.get(ALLURE_FIXTURE);
            final String event = keyValuePairs.get("event");

            // skip for invalid events
            if (Objects.isNull(type) || Objects.isNull(event)) {
                return;
            }

            switch (event) {
                case EVENT_START:
                    final Optional<String> maybeParent = containers.get(testIdentifier);
                    if (!maybeParent.isPresent()) {
                        return;
                    }
                    final String parentUuid = maybeParent.get();
                    startFixture(parentUuid, type, keyValuePairs);
                    return;
                case EVENT_FAILURE:
                    failFixture(keyValuePairs);
                    resetContext(testIdentifier);
                    return;
                case EVENT_STOP:
                    stopFixture(keyValuePairs);
                    resetContext(testIdentifier);
                    return;
                default:
                    break;
            }
            return;
        }

        if (keyValuePairs.containsKey(STDOUT)) {
            final String content = keyValuePairs.getOrDefault(STDOUT, "");
            getLifecycle().addAttachment("Stdout", TEXT_PLAIN, TXT_EXTENSION, content.getBytes(UTF_8));
        }
        if (keyValuePairs.containsKey(STDERR)) {
            final String content = keyValuePairs.getOrDefault(STDERR, "");
            getLifecycle().addAttachment("Stderr", TEXT_PLAIN, TXT_EXTENSION, content.getBytes(UTF_8));
        }

    }

    private void resetContext(final TestIdentifier testIdentifier) {
        // in case of fixtures that reported within a test we need to return current
        // test case uuid to allure thread local storage
        Optional.of(testIdentifier)
                .filter(TestIdentifier::isTest)
                .flatMap(tests::get)
                .ifPresent(Allure.getLifecycle()::setCurrentTestCase);
    }

    private void reportNested(final TestPlan testPlan,
                              final TestIdentifier testIdentifier,
                              final Status status,
                              final StatusDetails statusDetails,
                              final Set<TestIdentifier> visited) {
        final Set<TestIdentifier> children = testPlan.getChildren(testIdentifier);
        if (testIdentifier.isTest() || children.isEmpty()) {
            startTestCase(testIdentifier);
            stopTestCase(testIdentifier, status, statusDetails);
        }
        visited.add(testIdentifier);
        children.stream()
                .filter(id -> !visited.contains(id))
                .forEach(child -> reportNested(testPlan, child, status, statusDetails, visited));
    }

    protected Status getStatus(final Throwable throwable) {
        return ResultsUtils.getStatus(throwable).orElse(FAILED);
    }

    private void startTestContainer(final TestIdentifier testIdentifier) {
        final String uuid = containers.getOrCreate(testIdentifier);
        final TestResultContainer result = new TestResultContainer()
                .setUuid(uuid)
                .setName(testIdentifier.getDisplayName());

        getLifecycle().startTestContainer(result);
    }

    private void stopTestContainer(final TestIdentifier testIdentifier) {
        final Optional<String> maybeUuid = containers.get(testIdentifier);
        if (!maybeUuid.isPresent()) {
            return;
        }
        final String uuid = maybeUuid.get();
        final TestPlan context = testPlanStorage.get();
        final Set<String> children = Optional.ofNullable(context)
                .map(tp -> tp.getDescendants(testIdentifier))
                .orElseGet(Collections::emptySet)
                .stream()
                .filter(TestIdentifier::isTest)
                .map(tests::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(HashSet::new));

        if (testIdentifier.isTest()) {
            tests.get(testIdentifier).ifPresent(children::add);
        }

        getLifecycle().updateTestContainer(uuid, container -> container.setChildren(children));
        getLifecycle().stopTestContainer(uuid);
        getLifecycle().writeTestContainer(uuid);
    }

    private void startFixture(final String parentUuid,
                              final String type,
                              final Map<String, String> keyValue) {
        final String uuid = keyValue.get("uuid");
        if (Objects.isNull(uuid)) {
            return;
        }
        final String name = keyValue.getOrDefault("name", "Unknown");
        final FixtureResult result = new FixtureResult().setName(name);

        switch (type) {
            case PREPARE:
                getLifecycle().startPrepareFixture(parentUuid, uuid, result);
                return;
            case TEAR_DOWN:
                getLifecycle().startTearDownFixture(parentUuid, uuid, result);
                return;
            default:
                LOGGER.debug("unknown fixture type {}", type);
                break;
        }

    }

    private void failFixture(final Map<String, String> keyValue) {
        final String uuid = keyValue.get("uuid");
        if (Objects.isNull(uuid)) {
            return;
        }
        getLifecycle().updateFixture(uuid, fixtureResult -> {
            Optional.of(keyValue.get("status"))
                    .map(Status::fromValue)
                    .ifPresent(fixtureResult::setStatus);
            fixtureResult.setStatusDetails(new StatusDetails());
            Optional.of(keyValue.get("message"))
                    .ifPresent(fixtureResult.getStatusDetails()::setMessage);
            Optional.of(keyValue.get("trace"))
                    .ifPresent(fixtureResult.getStatusDetails()::setTrace);
        });
        getLifecycle().stopFixture(uuid);
    }

    private void stopFixture(final Map<String, String> keyValue) {
        final String uuid = keyValue.get("uuid");
        if (Objects.isNull(uuid)) {
            return;
        }
        getLifecycle().updateFixture(uuid, fixtureResult -> fixtureResult.setStatus(PASSED));
        getLifecycle().stopFixture(uuid);
    }

    @SuppressWarnings("PMD.NcssCount")
    private void startTestCase(final TestIdentifier testIdentifier) {
        final String uuid = tests.getOrCreate(testIdentifier);

        final Optional<TestSource> testSource = testIdentifier.getSource();
        final Optional<Method> testMethod = testSource.flatMap(this::getTestMethod);
        final Optional<Class<?>> testClass = testSource.flatMap(this::getTestClass);

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setName(testIdentifier.getDisplayName())
                .setLabels(getTags(testIdentifier))
                .setHistoryId(getHistoryId(testIdentifier))
                .setStage(Stage.RUNNING);

        result.getLabels().addAll(getProvidedLabels());

        testClass.map(AnnotationUtils::getLabels).ifPresent(result.getLabels()::addAll);
        testMethod.map(AnnotationUtils::getLabels).ifPresent(result.getLabels()::addAll);

        testClass.map(AnnotationUtils::getLinks).ifPresent(result.getLinks()::addAll);
        testMethod.map(AnnotationUtils::getLinks).ifPresent(result.getLinks()::addAll);

        result.getLabels().addAll(Arrays.asList(
                createHostLabel(),
                createThreadLabel(),
                createFrameworkLabel("junit-platform"),
                createLanguageLabel("java")
        ));

        testSource.flatMap(this::getFullName).ifPresent(result::setFullName);
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

        testMethod.ifPresent(method -> ResultsUtils.processDescription(
                method.getDeclaringClass().getClassLoader(),
                method,
                result
        ));

        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    private void stopTestCase(final TestIdentifier testIdentifier,
                              final Status status,
                              final StatusDetails statusDetails) {
        final Optional<String> maybeUuid = tests.get(testIdentifier);
        if (!maybeUuid.isPresent()) {
            return;
        }
        final String uuid = maybeUuid.get();
        getLifecycle().updateTestCase(uuid, result -> {
            result.setStage(Stage.FINISHED);
            result.setStatus(status);
            result.setStatusDetails(statusDetails);
        });
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
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

    private Optional<String> getFullName(final TestSource source) {
        if (source instanceof MethodSource) {
            final MethodSource ms = (MethodSource) source;
            return Optional.of(String.format("%s.%s", ms.getClassName(), ms.getMethodName()));
        }
        if (source instanceof ClassSource) {
            final ClassSource cs = (ClassSource) source;
            return Optional.ofNullable(cs.getClassName());
        }
        return Optional.empty();
    }

    private Optional<Class<?>> getTestClass(final TestSource source) {
        if (source instanceof MethodSource) {
            return getTestClass(((MethodSource) source).getClassName());
        }
        if (source instanceof ClassSource) {
            return Optional.of(((ClassSource) source).getJavaClass());
        }
        return Optional.empty();
    }

    private Optional<Class<?>> getTestClass(final String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException e) {
            LOGGER.trace("Could not get test class from test source {}", className, e);
        }
        return Optional.empty();
    }

    private Optional<Method> getTestMethod(final TestSource source) {
        if (source instanceof MethodSource) {
            return getTestMethod((MethodSource) source);
        }
        return Optional.empty();
    }

    private Optional<Method> getTestMethod(final MethodSource source) {
        try {
            final Class<?> aClass = Class.forName(source.getClassName());
            return Stream.of(aClass.getDeclaredMethods())
                    .filter(method -> MethodSource.from(method).equals(source))
                    .findAny();
        } catch (ClassNotFoundException e) {
            LOGGER.trace("Could not get test method from method source {}", source, e);
        }
        return Optional.empty();
    }

    private static class Uuids {

        private final Map<TestIdentifier, String> storage = new ConcurrentHashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        public Optional<String> get(final TestIdentifier testIdentifier) {
            try {
                lock.readLock().lock();
                return Optional.ofNullable(storage.get(testIdentifier));
            } finally {
                lock.readLock().unlock();
            }
        }

        private String getOrCreate(final TestIdentifier testIdentifier) {
            try {
                lock.writeLock().lock();
                return storage.computeIfAbsent(testIdentifier, ti -> UUID.randomUUID().toString());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}
