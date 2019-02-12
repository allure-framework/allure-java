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
package io.qameta.allure.citrus;

import com.consol.citrus.TestAction;
import com.consol.citrus.TestCase;
import com.consol.citrus.report.TestActionListener;
import com.consol.citrus.report.TestListener;
import com.consol.citrus.report.TestSuiteListener;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ObjectUtils;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureCitrus implements TestListener, TestSuiteListener, TestActionListener {

    private final Map<TestCase, String> testUuids = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AllureLifecycle lifecycle;

    public AllureCitrus(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @SuppressWarnings("unused")
    public AllureCitrus() {
        this.lifecycle = Allure.getLifecycle();
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void onStart() {
        //do nothing
    }

    @Override
    public void onStartSuccess() {
        //do nothing
    }

    @Override
    public void onStartFailure(final Throwable cause) {
        //do nothing
    }

    @Override
    public void onFinish() {
        //do nothing
    }

    @Override
    public void onFinishSuccess() {
        //do nothing
    }

    @Override
    public void onFinishFailure(final Throwable cause) {
        //do nothing
    }

    @Override
    public void onTestStart(final TestCase test) {
        startTestCase(test);
    }

    @Override
    public void onTestFinish(final TestCase test) {
        //do nothing
    }

    @Override
    public void onTestSuccess(final TestCase test) {
        stopTestCase(test, Status.PASSED, null);
    }

    @Override
    public void onTestFailure(final TestCase test, final Throwable cause) {
        final Status status = ResultsUtils.getStatus(cause).orElse(Status.BROKEN);
        final StatusDetails details = ResultsUtils.getStatusDetails(cause).orElse(null);
        stopTestCase(test, status, details);
    }

    @Override
    public void onTestSkipped(final TestCase test) {
        //do nothing
    }

    @Override
    public void onTestActionStart(final TestCase testCase, final TestAction testAction) {
        final String parentUuid = getUuid(testCase);
        final String uuid = UUID.randomUUID().toString();
        getLifecycle().startStep(parentUuid, uuid, new StepResult().setName(testAction.getName()));
    }

    @Override
    public void onTestActionFinish(final TestCase testCase, final TestAction testAction) {
        getLifecycle().stopStep();
    }

    @Override
    public void onTestActionSkipped(final TestCase testCase, final TestAction testAction) {
        //do nothing
    }

    private void startTestCase(final TestCase testCase) {
        final String uuid = createUuid(testCase);

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setName(testCase.getName())
                .setStage(Stage.RUNNING);

        result.getLabels().addAll(getProvidedLabels());


        final Optional<? extends Class<?>> testClass = Optional.ofNullable(testCase.getTestClass());
        testClass.map(this::getLabels).ifPresent(result.getLabels()::addAll);
        testClass.map(this::getLinks).ifPresent(result.getLinks()::addAll);

        result.getLabels().addAll(Arrays.asList(
                createHostLabel(),
                createThreadLabel(),
                createFrameworkLabel("citrus"),
                createLanguageLabel("java")
        ));

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

        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    private void stopTestCase(final TestCase testCase,
                              final Status status,
                              final StatusDetails details) {
        final String uuid = removeUuid(testCase);
        final Map<String, Object> definitions = testCase.getVariableDefinitions();
        final List<Parameter> parameters = definitions.entrySet().stream()
                .map(entry -> new Parameter()
                        .setName(entry.getKey())
                        .setValue(ObjectUtils.toString(entry.getValue()))
                )
                .collect(Collectors.toList());

        getLifecycle().updateTestCase(uuid, result -> {
            result.setParameters(parameters);
            result.setStage(Stage.FINISHED);
            result.setStatus(status);
            result.setStatusDetails(details);
        });
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }


    private String createUuid(final TestCase testCase) {
        final String uuid = UUID.randomUUID().toString();
        try {
            lock.writeLock().lock();
            testUuids.put(testCase, uuid);
        } finally {
            lock.writeLock().unlock();
        }
        return uuid;
    }

    private String getUuid(final TestCase testCase) {
        try {
            lock.readLock().lock();
            return testUuids.get(testCase);
        } finally {
            lock.readLock().unlock();
        }
    }

    private String removeUuid(final TestCase testCase) {
        try {
            lock.writeLock().lock();
            return testUuids.remove(testCase);
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
                getAnnotations(annotatedElement, io.qameta.allure.TmsLink.class).map(ResultsUtils::createLink))
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
