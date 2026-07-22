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
package io.qameta.allure.spock2;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureId;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.testfilter.FileTestPlanSupplier;
import io.qameta.allure.testfilter.TestPlan;
import io.qameta.allure.testfilter.TestPlanV1_0;
import io.qameta.allure.util.AnnotationUtils;
import io.qameta.allure.util.ExceptionUtils;
import io.qameta.allure.util.ResultsUtils;
import org.spockframework.runtime.AbstractRunListener;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.extension.IMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.model.ErrorInfo;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.IterationInfo;
import org.spockframework.runtime.model.MethodInfo;
import org.spockframework.runtime.model.MethodKind;
import org.spockframework.runtime.model.SpecInfo;
import org.spockframework.runtime.model.TestTag;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import static io.qameta.allure.util.ResultsUtils.createTitlePath;
import static io.qameta.allure.util.ResultsUtils.createTitlePathFromPackage;
import static io.qameta.allure.util.ResultsUtils.firstNonEmpty;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Reports Spock 2 specifications to Allure.
 *
 * <p>Register this extension with Spock to convert specification, feature, iteration, fixture, and error events into Allure results. The constructor accepting a test plan enables Allure test plan filtering before execution.</p>
 */
public class AllureSpock2 extends AbstractRunListener implements IGlobalExtension {

    private final ThreadLocal<String> testResults = new ThreadLocal<>();

    private final AllureLifecycle lifecycle;

    private final AllureSpock2BlockListener blockListener;

    private final TestPlan testPlan;

    /**
     * Creates an Allure spock2 with default configuration.
     */
    @SuppressWarnings("unused")
    public AllureSpock2() {
        this(Allure.getLifecycle());
    }

    /**
     * Creates an Allure spock2 with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public AllureSpock2(final AllureLifecycle lifecycle) {
        this(lifecycle, new FileTestPlanSupplier().supply().orElse(null));
    }

    /**
     * Creates an Allure spock2 with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     * @param plan the plan
     */
    public AllureSpock2(final AllureLifecycle lifecycle, final TestPlan plan) {
        this.lifecycle = lifecycle;
        this.blockListener = new AllureSpock2BlockListener(lifecycle);
        this.testPlan = plan;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitSpec(final SpecInfo spec) {
        spec.getAllFeatures().forEach(featureInfo -> {
            featureInfo.setSkipped(this.isSkipped(featureInfo));
            featureInfo.addBlockListener(blockListener);
        });

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

        // A spec-scoped fixture runs once, so associate its container with the first executed test only.
        final AtomicBoolean specContainerLinked = new AtomicBoolean();
        spec.getAllFeatures().stream()
                .map(FeatureInfo::getFeatureMethod)
                .filter(Objects::nonNull)
                .forEach(fm -> fm.addInterceptor(i -> {
                    getLifecycle().getCurrentExecutableKey()
                            .filter(key -> specContainerLinked.compareAndSet(false, true))
                            .ifPresent(key -> getLifecycle().addTestToScope(scopeKey(specContainerUuid), key));
                    i.proceed();
                }));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeIteration(final IterationInfo iteration) {
        final String uuid = UUID.randomUUID().toString();

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
        // the declared feature name is the source-level identity of the feature method,
        // stable across data-driven iterations, unlike the resolved iteration display name
        final String testMethodName = feature.getName();
        final String displayName = iteration.getDisplayName();

        final List<Label> defaultLabels = new ArrayList<>(
                Collections.singletonList(createSuiteLabel(specName))
        );

        if (Objects.nonNull(subSpec)) {
            defaultLabels.add(createSubSuiteLabel(subSpec.getName()));
        }

        if (Objects.nonNull(superSpec)) {
            defaultLabels.add(createParentSuiteLabel(superSpec.getName()));
        }

        final List<Label> testTags = feature.getTestTags().stream()
                .map(TestTag::getValue)
                .map(ResultsUtils::createTagLabel)
                .collect(Collectors.toList());

        final List<Label> labels = new ArrayList<>(
                Arrays.asList(
                        createPackageLabel(packageName),
                        createTestClassLabel(testClassName),
                        createTestMethodLabel(testMethodName),
                        createHostLabel(),
                        createThreadLabel(),
                        createFrameworkLabel("spock"),
                        createLanguageLabel("java")
                )
        );
        labels.addAll(testTags);
        labels.addAll(featureLabels);
        labels.addAll(specLabels);
        AnnotationUtils.getSeverity(method)
                .map(Optional::of)
                .orElseGet(() -> AnnotationUtils.getSeverity(clazz))
                .map(ResultsUtils::createSeverityLabel)
                .ifPresent(labels::add);
        labels.addAll(getProvidedLabels());

        final List<Link> links = new ArrayList<>(featureLinks);
        links.addAll(specLinks);

        final String qualifiedName = getQualifiedName(iteration);
        final List<String> titlePath = new ArrayList<>(createTitlePathFromPackage(packageName));
        titlePath.addAll(
                createTitlePath(
                        Objects.nonNull(superSpec) ? superSpec.getName() : null,
                        specName,
                        Objects.nonNull(subSpec) ? subSpec.getName() : null
                )
        );
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setTestCaseName(iteration.getName())
                .setTestCaseId(md5(qualifiedName))
                .setFullName(qualifiedName)
                .setTitlePath(titlePath)
                .setName(
                        firstNonEmpty(displayName, iteration.getName(), qualifiedName)
                                .orElse("Unknown")
                )
                .setStatusDetails(
                        new StatusDetails()
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

        testResults.set(uuid);
        final AllureExternalKey testKey = testKey(uuid);
        getLifecycle().scheduleTest(testKey, result);
        getLifecycle().addDefaultLabels(testKey, defaultLabels);
        getLifecycle().startTest(testKey);
        blockListener.beforeIteration(testKey);

    }

    private String getQualifiedName(final IterationInfo iteration) {
        return this.getQualifiedName(iteration.getFeature().getSpec().getReflection().getName(), iteration.getName());
    }

    private String getQualifiedName(final FeatureInfo featureInfo) {
        return this.getQualifiedName(featureInfo.getSpec().getReflection().getName(), featureInfo.getName());
    }

    private String getQualifiedName(final String specName, final String testName) {
        return specName + "." + testName;
    }

    private boolean isSkipped(final FeatureInfo featureInfo) {
        if (Objects.isNull(this.testPlan)) {
            return false;
        }
        if (this.testPlan instanceof TestPlanV1_0) {
            final TestPlanV1_0 tp = (TestPlanV1_0) testPlan;
            return !Objects.isNull(tp.getTests()) && tp.getTests()
                    .stream()
                    .filter(Objects::nonNull)
                    .noneMatch(
                            tc -> this.match(
                                    tc,
                                    this.getAllureId(featureInfo),
                                    this.getQualifiedName(featureInfo)
                            )
                    );
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

    private boolean match(final TestPlanV1_0.TestCase tc, final String allureId, final String qualifiedName) {
        return Objects.equals(allureId, tc.getId()) || Objects.equals(qualifiedName, tc.getSelector());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final ErrorInfo error) {
        final String uuid = testResults.get();
        if (Objects.isNull(uuid)) {
            return;
        }
        final Throwable exception = error.getException();
        blockListener.error(error);
        getLifecycle().updateTest(testKey(uuid), testResult -> {
            testResult.setStatus(getStatus(exception).orElse(null));

            final StatusDetails details = getStatusDetails(exception).orElse(null);
            if (Objects.isNull(details)) {
                return;
            }
            // merge the exception details into the existing status details so that
            // the flaky/muted flags set in beforeIteration are not overwritten
            final StatusDetails current = testResult.getStatusDetails();
            if (Objects.isNull(current)) {
                testResult.setStatusDetails(details);
            } else {
                current.setMessage(details.getMessage())
                        .setTrace(details.getTrace())
                        .setActual(details.getActual())
                        .setExpected(details.getExpected());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterIteration(final IterationInfo iteration) {
        final String uuid = testResults.get();
        if (Objects.isNull(uuid)) {
            testResults.remove();
            blockListener.afterIteration();
            return;
        }

        try {
            blockListener.afterIteration();
            final AllureExternalKey testKey = testKey(uuid);
            getLifecycle().updateTest(testKey, testResult -> {
                if (Objects.isNull(testResult.getStatus())) {
                    testResult.setStatus(Status.PASSED);
                }
            });
            getLifecycle().stopTest(testKey);
            getLifecycle().writeTest(testKey);
        } finally {
            testResults.remove();
        }
    }

    private List<Parameter> getParameters(final List<String> names, final Object... values) {
        return IntStream.range(0, Math.min(names.size(), values.length))
                .mapToObj(index -> createParameter(names.get(index), values[index]))
                .collect(Collectors.toList());
    }

    /**
     * Returns the lifecycle.
     *
     * @return the Allure lifecycle used by this integration
     */
    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    private AllureExternalKey scopeKey(final String uuid) {
        return AllureExternalKey.of(AllureSpock2.class, "scope", uuid);
    }

    private AllureExternalKey testKey(final String uuid) {
        return AllureExternalKey.of(AllureSpock2.class, "test", uuid);
    }

    private AllureExternalKey fixtureKey(final String uuid) {
        return AllureExternalKey.of(AllureSpock2.class, "fixture", uuid);
    }

    /**
     * IMethodInterceptor that reports feature fixture methods.
     * Creates container and fixture result for feature fixtures. Such fixtures (setup/cleanup) are
     * executed within iteration, so we can access current test uuid
     * from Allure context. But then we need to not forget to restore it to context, because
     * thread context is cleared on fixture start event.
     */
    private final class AllureFeatureFixtureMethodInterceptor extends AllureSpecFixtureMethodInterceptor {

        private AllureFeatureFixtureMethodInterceptor() {
            this(UUID.randomUUID().toString());
        }

        private AllureFeatureFixtureMethodInterceptor(final String containerUuid) {
            super(containerUuid);
        }

        @Override
        public void intercept(final IMethodInvocation invocation) throws Throwable {
            final AllureExternalKey testKey = getLifecycle()
                    .getCurrentRootKey()
                    .orElse(null);
            if (Objects.isNull(testKey)) {
                invocation.proceed();
                return;
            }

            final AllureExternalKey scopeKey = scopeKey(containerUuid);
            getLifecycle().registerScope(scopeKey);
            getLifecycle().addTestToScope(scopeKey, testKey);

            try {
                super.intercept(invocation);
            } finally {
                getLifecycle().writeScope(scopeKey);
                getLifecycle().setCurrent(testKey);
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

            final AllureExternalKey fixtureKey = fixtureKey(fixtureUuid);
            if (kind.isSetupMethod()) {
                getLifecycle().startBeforeFixture(
                        scopeKey(containerUuid),
                        fixtureKey,
                        fixtureResult
                );
            } else {
                getLifecycle().startAfterFixture(
                        scopeKey(containerUuid),
                        fixtureKey,
                        fixtureResult
                );
            }

            try {
                invocation.proceed();
                getLifecycle().updateFixture(
                        fixtureKey,
                        f -> f.setStatus(Status.PASSED)
                );
            } catch (Throwable throwable) {
                getLifecycle().updateFixture(
                        fixtureKey,
                        f -> f
                                .setStatus(getStatus(throwable).orElse(Status.BROKEN))
                                .setStatusDetails(getStatusDetails(throwable).orElse(null))
                );
                throw ExceptionUtils.sneakyThrow(throwable);
            } finally {
                getLifecycle().stopFixture(fixtureKey);
            }
        }

    }

    /**
     * IMethodInterceptor that creates container per spec.
     */
    private final class AllureContainerInterceptor implements IMethodInterceptor {

        private final String containerUuid;

        private AllureContainerInterceptor(final String containerUuid) {
            this.containerUuid = containerUuid;
        }

        @Override
        public void intercept(final IMethodInvocation invocation) throws Throwable {
            final AllureExternalKey scopeKey = scopeKey(containerUuid);
            getLifecycle().registerScope(scopeKey);

            try {
                invocation.proceed();
            } finally {
                getLifecycle().writeScope(scopeKey);
            }
        }
    }

}
