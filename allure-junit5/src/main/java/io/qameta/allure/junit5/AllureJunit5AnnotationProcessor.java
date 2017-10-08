package io.qameta.allure.junit5;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.BeforeEachMethodAdapter;
import org.junit.jupiter.engine.extension.ExtensionRegistry;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Allure Junit5 annotation processor.
 */
public class AllureJunit5AnnotationProcessor implements BeforeEachMethodAdapter {

    private static AllureLifecycle lifecycle;

    @Override
    public void invokeBeforeEachMethod(ExtensionContext context, ExtensionRegistry registry) throws Throwable {
        context.getTestClass().map(this::getLabels).ifPresent(labels -> {
            getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
                getLifecycle().updateTestCase(uuid, testResult -> {
                    testResult.getLabels().addAll(labels);
                });
            });
        });
        context.getTestMethod().map(this::getLabels).ifPresent(labels -> {
            getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
                getLifecycle().updateTestCase(uuid, testResult -> {
                    testResult.getLabels().addAll(labels);
                });
            });
        });
    }

    private List<Label> getLabels(final AnnotatedElement element) {
        return Stream.of(
                getLabels(element, Epic.class, ResultsUtils::createLabel),
                getLabels(element, Feature.class, ResultsUtils::createLabel),
                getLabels(element, Story.class, ResultsUtils::createLabel),
                getLabels(element, Severity.class, ResultsUtils::createLabel),
                getLabels(element, Owner.class, ResultsUtils::createLabel)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private <T extends Annotation> Stream<Label> getLabels(final AnnotatedElement annotatedElement,
                                                           final Class<T> annotationClass,
                                                           final Function<T, Label> extractor) {
        final List<Label> labels = getAnnotations(annotatedElement, annotationClass).stream()
                .map(extractor)
                .collect(Collectors.toList());
        return labels.stream();
    }

    private <T extends Annotation> List<T> getAnnotations(final AnnotatedElement annotatedElement,
                                                          final Class<T> annotationClass) {
        final T annotation = annotatedElement.getAnnotation(annotationClass);
        return Stream.concat(
                extractRepeatable(annotatedElement, annotationClass).stream(),
                Objects.isNull(annotation) ? Stream.empty() : Stream.of(annotation)
        ).collect(Collectors.toList());
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
