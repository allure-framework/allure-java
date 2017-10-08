package io.qameta.allure.junit5;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Allure Junit5 annotation processor.
 */
public class AllureJunit5AnnotationProcessor implements BeforeTestExecutionCallback {

    private static AllureLifecycle lifecycle;

    @Override
    public void beforeTestExecution(final ExtensionContext context) throws Exception {
        getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
            getLifecycle().updateTestCase(uuid, testResult -> {
                context.getTestClass().ifPresent(testClass -> {
                    testResult.getLabels().addAll(getLabels(testClass));
                    testResult.getLinks().addAll(getLinks(testClass));
                });
                context.getTestMethod().ifPresent(testMethod -> {
                    testResult.getLabels().addAll(getLabels(testMethod));
                    testResult.getLinks().addAll(getLinks(testMethod));
                });
            });
        });
    }

    private List<Label> getLabels(final AnnotatedElement annotatedElement) {
        return Stream.of(
                getAnnotations(annotatedElement, Epic.class).map(ResultsUtils::createLabel),
                getAnnotations(annotatedElement, Feature.class).map(ResultsUtils::createLabel),
                getAnnotations(annotatedElement, Story.class).map(ResultsUtils::createLabel),
                getAnnotations(annotatedElement, Severity.class).map(ResultsUtils::createLabel),
                getAnnotations(annotatedElement, Owner.class).map(ResultsUtils::createLabel)
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


    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set.
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        lifecycle = allure;
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }

}
