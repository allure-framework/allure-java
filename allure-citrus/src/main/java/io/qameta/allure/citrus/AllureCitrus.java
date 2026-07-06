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
package io.qameta.allure.citrus;

import com.consol.citrus.TestAction;
import com.consol.citrus.TestCase;
import com.consol.citrus.report.TestActionListener;
import com.consol.citrus.report.TestListener;
import com.consol.citrus.report.TestSuiteListener;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.createTitlePath;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;

/**
 * Reports Citrus test execution to Allure.
 *
 * <p>Register this listener with Citrus so suite, test case, and test action events are reflected as Allure containers, fixtures, tests, and steps. The listener can use the global lifecycle or an explicitly provided lifecycle.</p>
 */
public class AllureCitrus implements TestListener, TestSuiteListener, TestActionListener {

    private final Map<TestCase, AllureExternalKey> testKeys = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AllureLifecycle lifecycle;

    /**
     * Creates an Allure citrus with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public AllureCitrus(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * Creates an Allure citrus with default configuration.
     */
    @SuppressWarnings("unused")
    public AllureCitrus() {
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
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartSuccess() {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartFailure(final Throwable cause) {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFinish() {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFinishSuccess() {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFinishFailure(final Throwable cause) {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTestStart(final TestCase test) {
        startTest(test);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTestFinish(final TestCase test) {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTestSuccess(final TestCase test) {
        stopTest(test, Status.PASSED, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTestFailure(final TestCase test, final Throwable cause) {
        final Status status = ResultsUtils.getStatus(cause).orElse(Status.BROKEN);
        final StatusDetails details = ResultsUtils.getStatusDetails(cause).orElse(null);
        stopTest(test, status, details);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTestSkipped(final TestCase test) {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTestActionStart(final TestCase testCase, final TestAction testAction) {
        getLifecycle().startStep(new StepResult().setName(testAction.getName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTestActionFinish(final TestCase testCase, final TestAction testAction) {
        getLifecycle().stopStep();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTestActionSkipped(final TestCase testCase, final TestAction testAction) {
        //do nothing
    }

    private void startTest(final TestCase testCase) {
        final AllureExternalKey testKey = createTestKey(testCase);
        final Optional<? extends Class<?>> testClass = Optional.ofNullable(testCase.getTestClass());

        final TestResult result = new TestResult()
                .setName(testCase.getName())
                .setTitlePath(
                        testClass
                                .map(ResultsUtils::createTitlePathFromJavaClass)
                                .orElseGet(() -> createTitlePath(testCase.getName()))
                );

        result.getLabels().addAll(getProvidedLabels());

        testClass.map(this::getLabels).ifPresent(result.getLabels()::addAll);
        testClass.map(this::getLinks).ifPresent(result.getLinks()::addAll);

        result.getLabels().addAll(
                Arrays.asList(
                        createHostLabel(),
                        createThreadLabel(),
                        createFrameworkLabel("citrus"),
                        createLanguageLabel("java")
                )
        );

        testClass.ifPresent(aClass -> {
            final String suiteName = aClass.getCanonicalName();
            result.getLabels().add(createSuiteLabel(suiteName));
        });

        final Optional<String> classDescription = testClass.flatMap(this::getDescription);
        final String description = Stream.of(classDescription)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n\n"));

        result.setDescription(description);

        getLifecycle().scheduleTest(testKey, result);
        getLifecycle().startTest(testKey);
    }

    private void stopTest(final TestCase testCase,
                          final Status status,
                          final StatusDetails details) {
        final AllureExternalKey testKey = removeTestKey(testCase);
        final Map<String, Object> definitions = testCase.getVariableDefinitions();
        final List<Parameter> parameters = definitions.entrySet().stream()
                .map(entry -> createParameter(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        getLifecycle().updateTest(testKey, result -> {
            result.setParameters(parameters);
            result.setStatus(status);
            result.setStatusDetails(details);
        });
        getLifecycle().stopTest(testKey);
        getLifecycle().writeTest(testKey);
    }

    private AllureExternalKey createTestKey(final TestCase testCase) {
        final AllureExternalKey testKey = AllureExternalKey.random(AllureCitrus.class);
        try {
            lock.writeLock().lock();
            testKeys.put(testCase, testKey);
        } finally {
            lock.writeLock().unlock();
        }
        return testKey;
    }

    private AllureExternalKey removeTestKey(final TestCase testCase) {
        try {
            lock.writeLock().lock();
            return testKeys.remove(testCase);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Optional<String> getDescription(final AnnotatedElement annotatedElement) {
        return getAnnotations(annotatedElement, Description.class)
                .map(Description::value)
                .findAny();
    }

    private List<Label> getLabels(final AnnotatedElement annotatedElement) {
        return Stream.of(
                getAnnotations(annotatedElement, Epic.class).map(ResultsUtils::createLabel),
                getAnnotations(annotatedElement, Feature.class).map(ResultsUtils::createLabel),
                getAnnotations(annotatedElement, Story.class).map(ResultsUtils::createLabel)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private List<Link> getLinks(final AnnotatedElement annotatedElement) {
        return Stream.of(
                getAnnotations(annotatedElement, io.qameta.allure.Link.class).map(ResultsUtils::createLink),
                getAnnotations(annotatedElement, io.qameta.allure.Issue.class).map(ResultsUtils::createLink),
                getAnnotations(annotatedElement, io.qameta.allure.TmsLink.class).map(ResultsUtils::createLink)
        )
                .reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private <T extends Annotation> Stream<T> getAnnotations(final AnnotatedElement annotatedElement,
                                                            final Class<T> annotationClass) {
        final T annotation = annotatedElement.getAnnotation(annotationClass);
        return Stream.concat(
                extractRepeatable(annotatedElement, annotationClass).stream(),
                Objects.isNull(annotation) ? Stream.empty() : Stream.of(annotation)
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> List<T> extractRepeatable(final AnnotatedElement annotatedElement,
                                                             final Class<T> annotationClass) {
        if (annotationClass.isAnnotationPresent(Repeatable.class)) {
            final Repeatable repeatable = annotationClass.getAnnotation(Repeatable.class);
            final Class<? extends Annotation> wrapper = repeatable.value();
            final Annotation annotation = annotatedElement.getAnnotation(wrapper);
            if (Objects.nonNull(annotation)) {
                try {
                    final Method value = annotation.getClass().getMethod("value");
                    final Object annotations = value.invoke(annotation);
                    return Arrays.asList((T[]) annotations);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return Collections.emptyList();
    }
}
