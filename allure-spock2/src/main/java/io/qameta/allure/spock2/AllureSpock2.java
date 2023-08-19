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
package io.qameta.allure.spock2;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureId;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.testfilter.FileTestPlanSupplier;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanUnknown;
import io.qameta.allure.testfilter.TestPlanV1_0;
import io.qameta.allure.util.AnnotationUtils;
import io.qameta.allure.util.ExceptionUtils;
import io.qameta.allure.util.ResultsUtils;
import org.spockframework.runtime.AbstractRunListener;
import org.spockframework.runtime.IStandardStreamsListener;
import org.spockframework.runtime.StandardStreamsCapturer;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.extension.IMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.model.ErrorInfo;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.IterationInfo;
import org.spockframework.runtime.model.MethodInfo;
import org.spockframework.runtime.model.MethodKind;
import org.spockframework.runtime.model.SpecInfo;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.qameta.allure.util.ResultsUtils.bytesToHex;
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
import static io.qameta.allure.util.ResultsUtils.getMd5Digest;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings({
        "PMD.NcssCount"
})
public class AllureSpock2 extends AbstractRunListener implements IGlobalExtension, IStandardStreamsListener {

    private final StandardStreamsCapturer streamsCapturer = new StandardStreamsCapturer();

    private final ThreadLocal<String> testResults = new InheritableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return UUID.randomUUID().toString();
        }
    };

    private final AllureLifecycle lifecycle;

    private final TestPlan testPlan;

    @SuppressWarnings("unused")
    public AllureSpock2() {
        this(Allure.getLifecycle());
    }

    public AllureSpock2(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.streamsCapturer.addStandardStreamsListener(this);
        this.testPlan = new FileTestPlanSupplier().supply().orElse(new TestPlanUnknown());
    }

    public AllureSpock2(final AllureLifecycle lifecycle, TestPlan plan) {
        this.lifecycle = lifecycle;
        this.streamsCapturer.addStandardStreamsListener(this);
        this.testPlan = plan;
    }

    @Override
    public void start() {
        //do nothing at this point
    }

    @Override
    public void visitSpec(final SpecInfo spec) {
        spec.getAllFeatures().forEach(methodInfo -> methodInfo.setSkipped(this.isSkipped(methodInfo)));

        spec.addListener(this);

        final String specContainerUuid = UUID.randomUUID().toString();
        spec.addInterceptor(new AllureContainerInterceptor(specContainerUuid));

        spec.getAllFixtureMethods().forEach(methodInfo -> {
            if (methodInfo.getKind().isSpecScopedFixtureMethod()) {
                methodInfo.addInterceptor(new AllureSpecFixtureMethodInterceptor(specContainerUuid));
            }
            if (methodInfo.getKind().isFeatureScopedFixtureMethod()) {
                methodInfo.addInterceptor(new AllureFeatureFixtureMethodInterceptor());
            }
        });

        // add each feature to this container
        spec.getAllFeatures().stream()
                .map(FeatureInfo::getFeatureMethod)
                .filter(Objects::nonNull)
                .forEach(fm -> fm.addInterceptor(i -> {
                    getLifecycle().getCurrentTestCaseOrStep().ifPresent(uuid -> {
                        getLifecycle().updateTestContainer(
                                specContainerUuid,
                                c -> c.getChildren().add(uuid)
                        );
                    });
                    i.proceed();
                }));
    }

    @Override
    public void stop() {
        //do nothing at this point
    }

    @Override
    public void standardOut(final String message) {
        logMessage(message, Status.PASSED);
    }

    @Override
    public void standardErr(final String message) {
        logMessage(message, Status.BROKEN);
    }

    private void logMessage(final String message, final Status status) {
        if (Objects.isNull(message)) {
            return;
        }

        final String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        getLifecycle().getCurrentTestCaseOrStep().ifPresent(parentUuid -> {
            final String uuid = UUID.randomUUID().toString();
            getLifecycle().startStep(
                    parentUuid, uuid,
                    new StepResult().setName(trimmed).setStatus(status)
            );
            getLifecycle().stopStep(uuid);
        });
    }


    @Override
    public void beforeIteration(final IterationInfo iteration) {
        final String uuid = testResults.get();

        final FeatureInfo feature = iteration.getFeature();
        final MethodInfo methodInfo = feature.getFeatureMethod();
        final Method method = methodInfo.getReflection();
        final Set<Label> featureLabels = AnnotationUtils.getLabels(method);
        final Set<Link> featureLinks = AnnotationUtils.getLinks(method);
        final SpecInfo specInfo = feature.getSpec();
        final Class<?> clazz = specInfo.getReflection();
        final Set<Label> specLabels = AnnotationUtils.getLabels(clazz);
        final Set<Link> specLinks = AnnotationUtils.getLinks(clazz);
        final boolean flaky = AnnotationUtils.isFlaky(method) || AnnotationUtils.isFlaky(clazz);
        final boolean muted = AnnotationUtils.isMuted(method) || AnnotationUtils.isMuted(clazz);

        final List<Parameter> parameters = getParameters(feature.getDataVariables(), iteration.getDataValues());
        final SpecInfo subSpec = specInfo.getSubSpec();
        final SpecInfo superSpec = specInfo.getSuperSpec();
        final String packageName = specInfo.getPackage();
        final String specName = specInfo.getName();
        final String testClassName = feature.getSpec().getReflection().getName();
        final String testMethodName = iteration.getDisplayName();

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

        labels.addAll(featureLabels);
        labels.addAll(specLabels);
        labels.addAll(getProvidedLabels());

        final List<Link> links = new ArrayList<>(featureLinks);
        links.addAll(specLinks);

        final String qualifiedName = getQualifiedName(iteration);
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setHistoryId(getHistoryId(qualifiedName, parameters))
                .setTestCaseName(iteration.getName())
                .setTestCaseId(md5(qualifiedName))
                .setFullName(qualifiedName)
                .setName(
                        firstNonEmpty(testMethodName, iteration.getName(), qualifiedName)
                                .orElse("Unknown")
                )
                .setStatusDetails(new StatusDetails()
                        .setFlaky(flaky)
                        .setMuted(muted)
                )
                .setParameters(parameters)
                .setLinks(links)
                .setLabels(labels);

        ResultsUtils.processDescription(
                getClass().getClassLoader(),
                method,
                result::setDescription,
                result::setDescriptionHtml
        );

        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);

    }

    private String getQualifiedName(final IterationInfo iteration) {
        return this.getQualifiedName(iteration.getFeature().getSpec().getReflection().getName(), iteration.getName());

    }

    private String getQualifiedName(final FeatureInfo featureInfo) {
        return this.getQualifiedName(featureInfo.getSpec().getReflection().getName(), featureInfo.getName());
    }

    private String getQualifiedName(String specName, String testName) {
        return specName + "." + testName;
    }

    private String getHistoryId(final String name, final List<Parameter> parameters) {
        final MessageDigest digest = getMd5Digest();
        digest.update(name.getBytes(UTF_8));
        parameters.stream()
                .sorted(comparing(Parameter::getName).thenComparing(Parameter::getValue))
                .forEachOrdered(parameter -> {
                    digest.update(parameter.getName().getBytes(UTF_8));
                    digest.update(parameter.getValue().getBytes(UTF_8));
                });
        final byte[] bytes = digest.digest();
        return bytesToHex(bytes);
    }

    private boolean isSkipped(final FeatureInfo featureInfo) {
        if (this.testPlan instanceof TestPlanV1_0) {
            final TestPlanV1_0 tp = (TestPlanV1_0) testPlan;
            return !Objects.isNull(tp.getTests()) && tp.getTests()
                    .stream()
                    .filter(Objects::nonNull)
                    .noneMatch(tc -> this.match(tc, this.getAllureId(featureInfo), this.getQualifiedName(featureInfo)));
        }
        return false;
    }

    private String getAllureId(final FeatureInfo featureInfo) {
        final AllureId annotation = featureInfo.getFeatureMethod().getAnnotation(AllureId.class);
        if (Objects.nonNull(annotation)) {
            return annotation.value();
        }
        return null;
    }

    private boolean match(TestPlanV1_0.TestCase tc, String allureId, String qualifiedName) {
        return Objects.equals(allureId, tc.getId()) || Objects.equals(qualifiedName, tc.getSelector());
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

    @Override
    public void beforeSpec(final SpecInfo spec) {
        streamsCapturer.start();
    }

    @Override
    public void afterSpec(final SpecInfo spec) {
        streamsCapturer.stop();
    }

    private List<Parameter> getParameters(final List<String> names, final Object... values) {
        return IntStream.range(0, Math.min(names.size(), values.length))
                .mapToObj(index -> createParameter(names.get(index), values[index]))
                .collect(Collectors.toList());
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * IMethodInterceptor that reports feature fixture methods.
     * Creates container and fixture result for feature fixtures. Such fixtures (setup/cleanup) are
     * executed within iteration, so we can access current test uuid
     * from Allure context. But then we need to not forget to restore it to context, because
     * thread context is cleared on fixture start event.
     */
    @SuppressWarnings("FinalClass")
    private class AllureFeatureFixtureMethodInterceptor extends AllureSpecFixtureMethodInterceptor {

        private AllureFeatureFixtureMethodInterceptor() {
            this(UUID.randomUUID().toString());
        }

        private AllureFeatureFixtureMethodInterceptor(final String containerUuid) {
            super(containerUuid);
        }

        @Override
        public void intercept(final IMethodInvocation invocation) throws Throwable {
            final String uuid = getLifecycle().getCurrentTestCase().orElse(null);
            if (Objects.isNull(uuid)) {
                invocation.proceed();
                return;
            }

            final TestResultContainer container = new TestResultContainer()
                    .setUuid(containerUuid);

            container.getChildren().add(uuid);

            getLifecycle().startTestContainer(container);

            try {
                super.intercept(invocation);
            } finally {
                getLifecycle().stopTestContainer(containerUuid);
                getLifecycle().writeTestContainer(containerUuid);
                getLifecycle().setCurrentTestCase(uuid);
            }
        }
    }

    /**
     * IMethodInterceptor that reports spec fixture methods. All spec fixture methods
     * are using the same container. Container is managed by {@link  AllureContainerInterceptor}.
     */
    @SuppressWarnings("FinalClass")
    private class AllureSpecFixtureMethodInterceptor implements IMethodInterceptor {

        protected final String containerUuid;

        private AllureSpecFixtureMethodInterceptor(final String containerUuid) {
            this.containerUuid = containerUuid;
        }

        @Override
        public void intercept(final IMethodInvocation invocation) throws Throwable {
            final String fixtureUuid = UUID.randomUUID().toString();

            final MethodKind kind = invocation.getMethod().getKind();
            final String fixtureName = kind.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');

            final FixtureResult fixtureResult = new FixtureResult()
                    .setName(fixtureName);

            if (kind.isSetupMethod()) {
                getLifecycle().startPrepareFixture(
                        containerUuid,
                        fixtureUuid,
                        fixtureResult
                );
            } else {
                getLifecycle().startTearDownFixture(
                        containerUuid,
                        fixtureUuid,
                        fixtureResult
                );
            }

            try {
                invocation.proceed();
                getLifecycle().updateFixture(
                        fixtureUuid,
                        f -> f.setStatus(Status.PASSED)
                );
            } catch (Throwable throwable) {
                getLifecycle().updateFixture(
                        fixtureUuid,
                        f -> f
                                .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                                .setStatusDetails(getStatusDetails(throwable).orElse(null))
                );
                ExceptionUtils.sneakyThrow(throwable);
            } finally {
                getLifecycle().stopFixture(fixtureUuid);
            }
        }

    }

    /**
     * IMethodInterceptor that creates container per spec.
     */
    @SuppressWarnings("FinalClass")
    private class AllureContainerInterceptor implements IMethodInterceptor {

        private final String containerUuid;

        private AllureContainerInterceptor(final String containerUuid) {
            this.containerUuid = containerUuid;
        }

        @Override
        public void intercept(final IMethodInvocation invocation) throws Throwable {
            final TestResultContainer container = new TestResultContainer()
                    .setUuid(containerUuid);

            getLifecycle().startTestContainer(container);

            try {
                invocation.proceed();
            } finally {
                getLifecycle().stopTestContainer(containerUuid);
                getLifecycle().writeTestContainer(containerUuid);
            }
        }
    }

}
