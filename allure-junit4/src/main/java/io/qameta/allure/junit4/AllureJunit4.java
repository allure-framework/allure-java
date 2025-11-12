/*
 *  Copyright 2016-2024 Qameta Software Inc
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
import io.qameta.allure.AllureLifecycle;
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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.AnnotationUtils.getLabels;
import static io.qameta.allure.util.AnnotationUtils.getLinks;
import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createTestClassLabel;
import static io.qameta.allure.util.ResultsUtils.createTestMethodLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.getJavadocDescription;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Allure Junit4 listener.
 */
@RunListener.ThreadSafe
@SuppressWarnings({"checkstyle:ClassFanOutComplexity"})
public class AllureJunit4 extends RunListener {

    private static final boolean HAS_CUCUMBERJVM7_IN_CLASSPATH
            = isClassAvailableOnClasspath("io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm");

    private static final boolean HAS_CUCUMBERJVM6_IN_CLASSPATH
            = isClassAvailableOnClasspath("io.qameta.allure.cucumber6jvm.AllureCucumber6Jvm");

    private static final boolean HAS_CUCUMBERJVM5_IN_CLASSPATH
            = isClassAvailableOnClasspath("io.qameta.allure.cucumber5jvm.AllureCucumber5Jvm");

    private static final boolean HAS_CUCUMBERJVM4_IN_CLASSPATH
            = isClassAvailableOnClasspath("io.qameta.allure.cucumber4jvm.AllureCucumber4Jvm");

    private final ThreadLocal<String> testCases = new InheritableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return UUID.randomUUID().toString();
        }
    };

    private final AllureLifecycle lifecycle;

    public AllureJunit4() {
        this(Allure.getLifecycle());
    }

    public AllureJunit4(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void testRunStarted(final Description description) {
        //do nothing
    }

    @Override
    public void testRunFinished(final Result result) {
        //do nothing
    }

    @Override
    public void testStarted(final Description description) {
        if (shouldIgnore(description)) {
            return;
        }
        final String uuid = testCases.get();
        final TestResult result = createTestResult(uuid, description);
        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    @Override
    public void testFinished(final Description description) {
        if (shouldIgnore(description)) {
            return;
        }
        final String uuid = testCases.get();
        testCases.remove();
        getLifecycle().updateTestCase(uuid, testResult -> {
            if (Objects.isNull(testResult.getStatus())) {
                testResult.setStatus(Status.PASSED);
            }
        });

        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    @Override
    public void testFailure(final Failure failure) {
        if (shouldIgnore(failure.getDescription())) {
            return;
        }
        final String uuid = testCases.get();
        getLifecycle().updateTestCase(uuid, testResult -> testResult
                .setStatus(getStatus(failure.getException()).orElse(null))
                .setStatusDetails(getStatusDetails(failure.getException()).orElse(null))
        );
    }

    @Override
    public void testAssumptionFailure(final Failure failure) {
        if (shouldIgnore(failure.getDescription())) {
            return;
        }
        final String uuid = testCases.get();
        getLifecycle().updateTestCase(uuid, testResult ->
                testResult.setStatus(Status.SKIPPED)
                        .setStatusDetails(getStatusDetails(failure.getException()).orElse(null))
        );
    }

    @Override
    public void testIgnored(final Description description) {
        if (shouldIgnore(description)) {
            return;
        }
        final String uuid = testCases.get();
        testCases.remove();

        final TestResult result = createTestResult(uuid, description);
        result.setStatus(Status.SKIPPED);
        result.setStatusDetails(getIgnoredMessage(description));
        result.setStart(System.currentTimeMillis());

        getLifecycle().scheduleTestCase(result);
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
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

    private String getHistoryId(final Description description) {
        return md5(description.getClassName() + description.getMethodName());
    }

    private String getPackage(final Class<?> testClass) {
        return Optional.ofNullable(testClass)
                .map(Class::getPackage)
                .map(Package::getName)
                .orElse("");
    }

    private StatusDetails getIgnoredMessage(final Description description) {
        final Ignore ignore = description.getAnnotation(Ignore.class);
        final String message = Objects.nonNull(ignore) && !ignore.value().isEmpty()
                ? ignore.value() : "Test ignored (without reason)!";
        return new StatusDetails().setMessage(message);
    }

    private TestResult createTestResult(final String uuid, final Description description) {
        final String className = description.getClassName();
        final String methodName = description.getMethodName();
        final String name = Objects.nonNull(methodName) ? methodName : className;
        final String fullName = AllureJunit4Utils.getFullName(description);
        final String suite = Optional.ofNullable(description.getTestClass())
                .map(it -> it.getAnnotation(DisplayName.class))
                .map(DisplayName::value).orElse(className);

        final TestResult testResult = new TestResult()
                .setUuid(uuid)
                .setHistoryId(getHistoryId(description))
                .setFullName(fullName)
                .setName(name);

        testResult.getLabels().addAll(getProvidedLabels());
        testResult.getLabels().addAll(Arrays.asList(
                createPackageLabel(getPackage(description.getTestClass())),
                createTestClassLabel(className),
                createTestMethodLabel(name),
                createSuiteLabel(suite),
                createHostLabel(),
                createThreadLabel(),
                createFrameworkLabel("junit4"),
                createLanguageLabel("java")
        ));
        testResult.getLabels().addAll(extractLabels(description));
        testResult.getLinks().addAll(extractLinks(description));

        getDisplayName(description).ifPresent(testResult::setName);

        getDescription(description).ifPresent(testResult::setDescription);
        return testResult;
    }

    @SuppressWarnings({"CyclomaticComplexity", "BooleanExpressionComplexity"})
    private boolean shouldIgnore(final Description description) {
        return (HAS_CUCUMBERJVM7_IN_CLASSPATH
                || HAS_CUCUMBERJVM6_IN_CLASSPATH
                || HAS_CUCUMBERJVM5_IN_CLASSPATH
                || HAS_CUCUMBERJVM4_IN_CLASSPATH
               ) && AllureJunit4Utils.isCucumberTest(description);
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
