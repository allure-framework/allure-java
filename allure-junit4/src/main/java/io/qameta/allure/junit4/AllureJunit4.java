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
package io.qameta.allure.junit4;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.AnnotationUtils;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.AnnotationUtils.getLabels;
import static io.qameta.allure.util.AnnotationUtils.getLinks;
import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createGlobalError;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createSeverityLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createTestClassLabel;
import static io.qameta.allure.util.ResultsUtils.createTestMethodLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.createTitlePathFromPackageAndClass;
import static io.qameta.allure.util.ResultsUtils.getJavadocDescription;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Allure Junit4 listener.
 */
@RunListener.ThreadSafe
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "PMD.GodClass"})
public class AllureJunit4 extends RunListener {

    private static final boolean HAS_CUCUMBERJVM7_IN_CLASSPATH = isClassAvailableOnClasspath("io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm");

    /**
     * The method name JUnit 4 gives the synthetic test it reports when a runner cannot be built for a class.
     */
    private static final String INITIALIZATION_ERROR = "initializationError";

    private final AllureLifecycle lifecycle;

    /**
     * Creates an Allure junit4 with default configuration.
     */
    public AllureJunit4() {
        this(Allure.getLifecycle());
    }

    /**
     * Creates an Allure junit4 with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public AllureJunit4(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
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
     * {@inheritDoc}
     */
    @Override
    public void testRunStarted(final Description description) {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunFinished(final Result result) {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(final Description description) {
        if (shouldIgnore(description) || isInitializationError(description)) {
            return;
        }
        final AllureExternalKey testKey = getTestKey(description);
        final TestResult result = createTestResult(description);
        getLifecycle().scheduleTest(testKey, result);
        getLifecycle().addDefaultLabels(testKey, createDefaultLabels(description));
        getLifecycle().startTest(testKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFinished(final Description description) {
        if (shouldIgnore(description) || isInitializationError(description)) {
            return;
        }
        final AllureExternalKey testKey = getTestKey(description);
        getLifecycle().updateTest(testKey, testResult -> {
            if (Objects.isNull(testResult.getStatus())) {
                testResult.setStatus(Status.PASSED);
            }
        });

        stopAndWriteTest(testKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailure(final Failure failure) {
        if (shouldIgnore(failure.getDescription())) {
            return;
        }
        if (reportRunFailure(failure, "failed")) {
            return;
        }
        final AllureExternalKey testKey = getTestKey(failure.getDescription());
        getLifecycle().updateTest(
                testKey, testResult -> {
                    testResult.setStatus(getStatus(failure.getException()).orElse(null));
                    mergeStatusDetails(testResult, getStatusDetails(failure.getException()).orElse(null));
                }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testAssumptionFailure(final Failure failure) {
        if (shouldIgnore(failure.getDescription())) {
            return;
        }
        if (reportRunFailure(failure, "was skipped by an assumption")) {
            return;
        }
        final AllureExternalKey testKey = getTestKey(failure.getDescription());
        getLifecycle().updateTest(
                testKey, testResult -> {
                    testResult.setStatus(Status.SKIPPED);
                    mergeStatusDetails(testResult, getStatusDetails(failure.getException()).orElse(null));
                }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testIgnored(final Description description) {
        if (shouldIgnore(description)) {
            return;
        }
        final AllureExternalKey testKey = getTestKey(description);

        final TestResult result = createTestResult(description);
        result.setStatus(Status.SKIPPED);
        result.getStatusDetails().setMessage(getIgnoredMessage(description));

        getLifecycle().scheduleTest(testKey, result);
        getLifecycle().addDefaultLabels(testKey, createDefaultLabels(description));
        getLifecycle().startTest(testKey);
        stopAndWriteTest(testKey);
    }

    private Optional<String> getDisplayName(final Description result) {
        return Optional.ofNullable(result.getAnnotation(DisplayName.class))
                .map(DisplayName::value);
    }

    private Optional<String> getDescription(final Description result) {
        final io.qameta.allure.Description annotation = result
                .getAnnotation(io.qameta.allure.Description.class);

        if (Objects.isNull(annotation)) {
            return Optional.empty();
        }

        if (!"".equals(annotation.value())) {
            return Optional.of(annotation.value());
        }

        // since we have no access to method & method parameter types
        // we simply find all the methods within test class that matching
        // specified method name. If there is only one result, consider it a
        // test.
        final Class<?> testClass = result.getTestClass();
        final String methodName = result.getMethodName();
        if (Objects.nonNull(testClass) && Objects.nonNull(methodName)) {
            final List<Method> found = Stream.of(testClass.getMethods())
                    .filter(method -> Objects.equals(methodName, method.getName()))
                    .collect(Collectors.toList());
            if (found.size() != 1) {
                return Optional.empty();
            }

            final Method method = found.get(0);
            return getJavadocDescription(
                    method.getDeclaringClass().getClassLoader(),
                    method
            );
        }
        return Optional.empty();
    }

    private List<Link> extractLinks(final Description description) {
        final List<Link> result = new ArrayList<>(getLinks(description.getAnnotations()));
        Optional.of(description)
                .map(Description::getTestClass)
                .map(AnnotationUtils::getLinks)
                .ifPresent(result::addAll);
        return result;
    }

    private List<Label> extractLabels(final Description description) {
        final List<Label> result = new ArrayList<>(getLabels(description.getAnnotations()));
        Optional.of(description)
                .map(Description::getTestClass)
                .map(AnnotationUtils::getLabels)
                .ifPresent(result::addAll);
        return result;
    }

    private String getTestIdentifier(final Description description) {
        return description.getClassName() + description.getMethodName();
    }

    private void stopAndWriteTest(final AllureExternalKey testKey) {
        getLifecycle().stopTest(testKey);
        getLifecycle().writeTest(testKey);
    }

    private String getPackage(final Class<?> testClass) {
        return Optional.ofNullable(testClass)
                .map(Class::getPackage)
                .map(Package::getName)
                .orElse("");
    }

    private String getIgnoredMessage(final Description description) {
        final Ignore ignore = description.getAnnotation(Ignore.class);
        return Objects.nonNull(ignore) && !ignore.value().isEmpty()
                ? ignore.value()
                : "Test ignored (without reason)!";
    }

    private AllureExternalKey getTestKey(final Description description) {
        return AllureExternalKey.of(AllureJunit4.class, "test", description);
    }

    private TestResult createTestResult(final Description description) {
        final String className = description.getClassName();
        final String methodName = description.getMethodName();
        final String name = Objects.nonNull(methodName) ? methodName : className;
        final String fullName = AllureJunit4Utils.getFullName(description);
        final String suite = Optional.ofNullable(description.getTestClass())
                .map(it -> it.getAnnotation(DisplayName.class))
                .map(DisplayName::value).orElse(className);

        final TestResult testResult = new TestResult()
                .setTestCaseId(md5(getTestIdentifier(description)))
                .setFullName(fullName)
                .setName(name)
                .setTitlePath(createTitlePathFromPackageAndClass(getPackage(description.getTestClass()), suite));

        testResult.getLabels().addAll(getProvidedLabels());
        testResult.getLabels().addAll(
                Arrays.asList(
                        createPackageLabel(className),
                        createTestClassLabel(className),
                        createHostLabel(),
                        createThreadLabel(),
                        createFrameworkLabel("junit4"),
                        createLanguageLabel("java")
                )
        );
        // the test method is unknown for class-level descriptions
        if (Objects.nonNull(methodName)) {
            testResult.getLabels().add(createTestMethodLabel(methodName));
        }
        testResult.getLabels().addAll(extractLabels(description));
        getSeverity(description)
                .map(severity -> createSeverityLabel(severity))
                .ifPresent(testResult.getLabels()::add);
        testResult.getLinks().addAll(extractLinks(description));

        testResult.setStatusDetails(
                new StatusDetails()
                        .setFlaky(isFlaky(description))
                        .setMuted(isMuted(description))
        );

        getDisplayName(description).ifPresent(testResult::setName);

        getDescription(description).ifPresent(testResult::setDescription);
        return testResult;
    }

    private boolean isFlaky(final Description description) {
        return AnnotationUtils.isFlaky(description.getAnnotations())
                || Optional.ofNullable(description.getTestClass())
                        .map(AnnotationUtils::isFlaky)
                        .orElse(false);
    }

    private boolean isMuted(final Description description) {
        return AnnotationUtils.isMuted(description.getAnnotations())
                || Optional.ofNullable(description.getTestClass())
                        .map(AnnotationUtils::isMuted)
                        .orElse(false);
    }

    private Optional<SeverityLevel> getSeverity(final Description description) {
        final Optional<SeverityLevel> methodSeverity = AnnotationUtils.getSeverity(description.getAnnotations());
        if (methodSeverity.isPresent()) {
            return methodSeverity;
        }
        return Optional.ofNullable(description.getTestClass())
                .flatMap(AnnotationUtils::getSeverity);
    }

    private static void mergeStatusDetails(final TestResult testResult, final StatusDetails details) {
        if (Objects.isNull(details)) {
            return;
        }
        // merge the exception details into the existing status details so that
        // the flaky/muted flags set at test start are not overwritten
        final StatusDetails current = testResult.getStatusDetails();
        if (Objects.isNull(current)) {
            testResult.setStatusDetails(details);
        } else {
            current.setMessage(details.getMessage())
                    .setTrace(details.getTrace())
                    .setActual(details.getActual())
                    .setExpected(details.getExpected());
        }
    }

    private List<Label> createDefaultLabels(final Description description) {
        final String className = description.getClassName();
        final String suite = Optional.ofNullable(description.getTestClass())
                .map(it -> it.getAnnotation(DisplayName.class))
                .map(DisplayName::value).orElse(className);

        return List.of(createSuiteLabel(suite));
    }

    /**
     * Reports a failure that belongs to the run rather than to a test as a run-level error, and returns whether it
     * did.
     *
     * <p>JUnit 4 sends two kinds of failure no test result can stand in for. A class-level failure — a
     * {@code @BeforeClass}, {@code @AfterClass} or {@code @ClassRule} that threw, or a {@code @BeforeClass}
     * assumption — arrives on the description of the class itself, with no events for the tests a before failure
     * prevented;
     * an {@code initializationError} — an invalid test class, or a {@code Parameterized} class whose parameters
     * method threw — arrives as a synthetic test with that name. A test result written for either is counted in the
     * run statistics under an identity no green run ever produces, so no retry could ever replace it. Nothing else
     * is backfilled either: JUnit 4 does not name the tests a class-level failure prevented, and a class whose
     * runner failed to initialise never enumerated them.</p>
     */
    private boolean reportRunFailure(final Failure failure, final String outcome) {
        final Description description = failure.getDescription();
        final String context;
        if (isInitializationError(description)) {
            context = String.format(
                    "Test class %s could not be initialized, its tests did not run",
                    description.getClassName()
            );
        } else if (isClassLevel(description)) {
            // JUnit 4 reports @BeforeClass, @AfterClass and @ClassRule failures on the same class description,
            // so whether the class's tests ran cannot be told from the event: an after failure comes once
            // they all have, a before failure before any of them. Name the count without claiming either.
            context = String.format(
                    "Test class %s %s outside its %d tests",
                    description.getClassName(),
                    outcome,
                    description.testCount()
            );
        } else {
            return false;
        }
        getLifecycle().writeGlobals(createGlobalError(context, failure.getException()));
        return true;
    }

    /**
     * Returns whether the description names a class rather than a test in it: a class-level fixture or rule
     * failure is reported on such a description, with no method name.
     */
    private static boolean isClassLevel(final Description description) {
        return description.isSuite() && Objects.isNull(description.getMethodName());
    }

    /**
     * Returns whether the description is the synthetic {@code initializationError} test JUnit 4 reports when a
     * runner could not be built for a class.
     */
    private static boolean isInitializationError(final Description description) {
        return INITIALIZATION_ERROR.equals(description.getMethodName());
    }

    private boolean shouldIgnore(final Description description) {
        return HAS_CUCUMBERJVM7_IN_CLASSPATH && AllureJunit4Utils.isCucumberTest(description);
    }

    private static boolean isClassAvailableOnClasspath(final String clazz) {
        try {
            AllureJunit4.class.getClassLoader().loadClass(clazz);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
