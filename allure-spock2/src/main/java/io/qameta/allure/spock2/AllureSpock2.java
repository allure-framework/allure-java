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
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.util.AnnotationUtils;
import io.qameta.allure.util.ExceptionUtils;
import io.qameta.allure.util.ResultsUtils;
import org.spockframework.runtime.AbstractRunListener;
import org.spockframework.runtime.extension.IGlobalExtension;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
public class AllureSpock2 extends AbstractRunListener implements IGlobalExtension {

    private final ThreadLocal<String> testResults = new InheritableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return UUID.randomUUID().toString();
        }
    };

    private final ThreadLocal<Uuids> containers = new InheritableThreadLocal<Uuids>() {
        @Override
        protected Uuids initialValue() {
            return new Uuids();
        }
    };

    private final AllureLifecycle lifecycle;

    @SuppressWarnings("unused")
    public AllureSpock2() {
        this(Allure.getLifecycle());
    }

    public AllureSpock2(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void start() {
        //do nothing at this point
    }

    @Override
    public void visitSpec(final SpecInfo spec) {
        spec.addListener(this);
        spec.addSetupSpecInterceptor(this::specFixtureInterceptor);
        spec.addCleanupSpecInterceptor(this::specFixtureInterceptor);
        spec.addSetupInterceptor(this::featureFixtureInterceptor);
        spec.addCleanupInterceptor(this::featureFixtureInterceptor);
    }

    @Override
    public void stop() {
        //do nothing at this point
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

        containers.get().get(iteration.getFeature().getSpec())
                .ifPresent(containerUuid -> getLifecycle().updateTestContainer(
                        containerUuid,
                        container -> container.getChildren().add(uuid)
                ));
    }

    private String getQualifiedName(final IterationInfo iteration) {
        return iteration.getFeature().getSpec().getReflection().getName() + "." + iteration.getName();
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
        final String containerUuid = containers.get().getOrCreate(spec);
        final TestResultContainer container = new TestResultContainer()
                .setUuid(containerUuid);

        getLifecycle().startTestContainer(container);
    }

    @Override
    public void afterSpec(final SpecInfo spec) {
        containers.get().get(spec).ifPresent(containerUuid -> {
            getLifecycle().stopTestContainer(containerUuid);
            getLifecycle().writeTestContainer(containerUuid);
        });
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
     * Creates container and fixture result for feature fixtures. Such fixtures (setup/cleanup) are
     * executed within iteration, so we can access current test uuid
     * from Allure context. But then we need to not forget to restore it to context, because
     * thread context is cleared on fixture start event.
     */
    private void featureFixtureInterceptor(final IMethodInvocation invocation) {
        final String uuid = getLifecycle().getCurrentTestCase().orElse(null);
        if (Objects.isNull(uuid)) {
            return;
        }

        final String containerUuid = UUID.randomUUID().toString();
        final TestResultContainer container = new TestResultContainer()
                .setUuid(containerUuid);

        container.getChildren().add(uuid);

        getLifecycle().startTestContainer(container);

        try {
            processFixture(invocation, containerUuid);
        } finally {
            getLifecycle().stopTestContainer(containerUuid);
            getLifecycle().writeTestContainer(containerUuid);
            getLifecycle().setCurrentTestCase(uuid);
        }
    }

    /**
     * For spec fixtures we re-use container, that we create per spec
     * in {@link #beforeSpec(SpecInfo)} method (and stop it in {@link #afterSpec(SpecInfo)}).
     * So we assume that we only have one container, container uuid is stored as attachment.
     * The rest of the flow are actually the same - simply create fixture around invocation.
     */
    private void specFixtureInterceptor(final IMethodInvocation invocation) {
        final String containerUuid = containers.get().get(invocation.getSpec())
                .orElse(null);

        if (Objects.isNull(containerUuid)) {
            return;
        }

        processFixture(invocation, containerUuid);
    }

    private void processFixture(final IMethodInvocation invocation,
                                final String containerUuid) {
        final String fixtureUuid = UUID.randomUUID().toString();

        final MethodKind kind = invocation.getMethod().getKind();
        final FixtureResult fixtureResult = new FixtureResult()
                .setName(kind.name());

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

    private static class Uuids {

        private final Map<SpecInfo, String> storage = new ConcurrentHashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        public Optional<String> get(final SpecInfo specInfo) {
            try {
                lock.readLock().lock();
                return Optional.ofNullable(storage.get(specInfo));
            } finally {
                lock.readLock().unlock();
            }
        }

        private String getOrCreate(final SpecInfo specInfo) {
            try {
                lock.writeLock().lock();
                return storage.computeIfAbsent(specInfo, ti -> UUID.randomUUID().toString());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

}
