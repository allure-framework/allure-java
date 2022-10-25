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
package io.qameta.allure.spock;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Flaky;
import io.qameta.allure.Muted;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.AnnotationUtils;
import org.spockframework.runtime.AbstractRunListener;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.model.ErrorInfo;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.IterationInfo;
import org.spockframework.runtime.model.SpecInfo;
import org.spockframework.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.createParentSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createSubSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createTestClassLabel;
import static io.qameta.allure.util.ResultsUtils.createTestMethodLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.firstNonEmpty;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings({
        "PMD.UnnecessaryFullyQualifiedName",
        "PMD.ExcessiveImports",
        "ClassFanOutComplexity",
        "PMD.CouplingBetweenObjects"
})
public class AllureSpock extends AbstractRunListener implements IGlobalExtension {

    private static final String MD_5 = "md5";
    private static final Method GET_DISPLAY_NAME = 
            ReflectionUtil.getDeclaredMethodByName(IterationInfo.class, "getDisplayName");

    private final ThreadLocal<String> testResults
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final AllureLifecycle lifecycle;

    @SuppressWarnings("unused")
    public AllureSpock() {
        this(Allure.getLifecycle());
    }

    public AllureSpock(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void start() {
        //do nothing at this point
    }

    @Override
    public void visitSpec(final SpecInfo spec) {
        spec.addListener(this);
    }

    @Override
    public void stop() {
        //do nothing at this point
    }

    @Override
    public void beforeIteration(final IterationInfo iteration) {
        final String uuid = testResults.get();
        final FeatureInfo feature = iteration.getFeature();
        final SpecInfo spec = feature.getSpec();
        final List<Parameter> parameters = getParameters(feature.getDataVariables(), iteration.getDataValues());
        final SpecInfo subSpec = spec.getSubSpec();
        final SpecInfo superSpec = spec.getSuperSpec();
        final String packageName = spec.getPackage();
        final String specName = spec.getName();
        final String testClassName = feature.getSpec().getReflection().getName();
        final String testMethodName = GET_DISPLAY_NAME == null ? iteration.getName() : 
            (String) ReflectionUtil.invokeMethod(iteration, GET_DISPLAY_NAME);

        final List<Label> labels = new ArrayList<>(Arrays.asList(
                createPackageLabel(packageName),
                createTestClassLabel(testClassName),
                createTestMethodLabel(testMethodName),
                createSuiteLabel(specName),
                createHostLabel(),
                createThreadLabel(),
                createFrameworkLabel("spock"),
                createLanguageLabel("java")
        ));

        if (Objects.nonNull(subSpec)) {
            labels.add(createSubSuiteLabel(subSpec.getName()));
        }

        if (Objects.nonNull(superSpec)) {
            labels.add(createParentSuiteLabel(superSpec.getName()));
        }

        labels.addAll(getLabels(iteration));
        labels.addAll(getProvidedLabels());

        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setHistoryId(getHistoryId(getQualifiedName(iteration), parameters))
                .setName(firstNonEmpty(
                        testMethodName,
                        iteration.getName(),
                        getQualifiedName(iteration)).orElse("Unknown"))
                .setFullName(getQualifiedName(iteration))
                .setStatusDetails(new StatusDetails()
                        .setFlaky(isFlaky(iteration))
                        .setMuted(isMuted(iteration)))
                .setParameters(parameters)
                .setLinks(getLinks(iteration))
                .setLabels(labels);
        processDescription(iteration, result);
        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    private List<Label> getLabels(final IterationInfo iterationInfo) {
        final Set<Label> labels = AnnotationUtils.getLabels(
                iterationInfo.getFeature().getFeatureMethod().getReflection().getAnnotations()
        );
        labels.addAll(AnnotationUtils.getLabels(
                iterationInfo.getFeature().getSpec().getReflection().getAnnotations()
        ));
        return new ArrayList<>(labels);
    }

    private void processDescription(final IterationInfo iterationInfo, final TestResult item) {
        final List<io.qameta.allure.Description> annotationsOnFeature = getFeatureAnnotations(
                iterationInfo, io.qameta.allure.Description.class);
        if (!annotationsOnFeature.isEmpty()) {
            item.setDescription(annotationsOnFeature.get(0).value());
        }
    }

    private String getQualifiedName(final IterationInfo iteration) {
        return iteration.getFeature().getSpec().getReflection().getName() + "." + iteration.getName();
    }

    private String getHistoryId(final String name, final List<Parameter> parameters) {
        final MessageDigest digest = getMessageDigest();
        digest.update(name.getBytes(UTF_8));
        parameters.stream()
                .sorted(comparing(Parameter::getName).thenComparing(Parameter::getValue))
                .forEachOrdered(parameter -> {
                    digest.update(parameter.getName().getBytes(UTF_8));
                    digest.update(parameter.getValue().getBytes(UTF_8));
                });
        final byte[] bytes = digest.digest();
        return new BigInteger(1, bytes).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

    private boolean isFlaky(final IterationInfo iteration) {
        return hasAnnotation(iteration, Flaky.class);
    }

    private boolean isMuted(final IterationInfo iteration) {
        return hasAnnotation(iteration, Muted.class);
    }

    private boolean hasAnnotation(final IterationInfo iteration, final Class<? extends Annotation> clazz) {
        return hasAnnotationOnFeature(iteration, clazz) || hasAnnotationOnSpec(iteration, clazz);
    }

    private boolean hasAnnotationOnSpec(final IterationInfo iteration, final Class<? extends Annotation> clazz) {
        return !getSpecAnnotations(iteration, clazz).isEmpty();
    }

    private boolean hasAnnotationOnFeature(final IterationInfo iteration, final Class<? extends Annotation> clazz) {
        return !getFeatureAnnotations(iteration, clazz).isEmpty();
    }

    private List<io.qameta.allure.model.Link> getLinks(final IterationInfo iteration) {
        final List<io.qameta.allure.model.Link> links = new ArrayList<>();
        links.addAll(AnnotationUtils.getLinks(iteration.
                                              getFeature().
                                              getFeatureMethod().
                                              getReflection().
                                              getAnnotations()));
        links.addAll(AnnotationUtils.getLinks(iteration.
                                              getFeature().
                                              getSpec().
                                              getReflection().
                                              getAnnotations()));
        return links;
    }

    private <T extends Annotation> List<T> getAnnotationsOnMethod(final AnnotatedElement result, final Class<T> clazz) {
        final T annotation = result.getAnnotation(clazz);
        return Stream.concat(
                extractRepeatable(result, clazz).stream(),
                Objects.isNull(annotation) ? Stream.empty() : Stream.of(annotation)
        ).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> List<T> extractRepeatable(final AnnotatedElement result, final Class<T> clazz) {
        if (clazz.isAnnotationPresent(Repeatable.class)) {
            final Repeatable repeatable = clazz.getAnnotation(Repeatable.class);
            final Class<? extends Annotation> wrapper = repeatable.value();
            final Annotation annotation = result.getAnnotation(wrapper);
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

    private <T extends Annotation> List<T> getAnnotationsOnClass(final Class<?> result, final Class<T> clazz) {
        return Stream.of(result)
                .map(testClass -> testClass.getAnnotationsByType(clazz))
                .flatMap(Stream::of)
                .collect(Collectors.toList());
    }

    private <T extends Annotation> List<T> getFeatureAnnotations(final IterationInfo iteration, final Class<T> clazz) {
        return getAnnotationsOnMethod(iteration.getFeature().getFeatureMethod().getReflection(), clazz);
    }

    private <T extends Annotation> List<T> getSpecAnnotations(final IterationInfo iteration, final Class<T> clazz) {
        final SpecInfo spec = iteration.getFeature().getSpec();
        return getAnnotationsOnClass(spec.getReflection(), clazz);
    }


    @Override
    public void error(final ErrorInfo error) {
        final String uuid = testResults.get();
        getLifecycle().updateTestCase(uuid, testResult -> testResult
                .setStatus(getStatus(error.getException()).orElse(null))
                .setStatusDetails(getStatusDetails(error.getException()).orElse(null))
        );
    }

    @Override
    public void afterIteration(final IterationInfo iteration) {
        final String uuid = testResults.get();
        testResults.remove();

        getLifecycle().updateTestCase(uuid, testResult -> {
            if (Objects.isNull(testResult.getStatus())) {
                testResult.setStatus(Status.PASSED);
            }
        });
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    private List<Parameter> getParameters(final List<String> names, final Object... values) {
        return IntStream.range(0, Math.min(names.size(), values.length))
                .mapToObj(index -> createParameter(names.get(index), values[index]))
                .collect(Collectors.toList());
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }
        
}
