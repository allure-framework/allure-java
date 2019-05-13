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
package io.qameta.allure.util;

import io.qameta.allure.LabelAnnotation;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * Collection of utils used by Allure integration to extract meta information from
 * test cases via reflection.
 *
 * @author charlie (Dmitry Baev).
 */
public final class AnnotationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationUtils.class);

    private static final String VALUE_METHOD_NAME = "value";

    private AnnotationUtils() {
        throw new IllegalStateException("Do not instance");
    }

    /**
     * Returns links created from Allure annotations specified on annotated element.
     *
     * @param annotatedElement the element to search annotations on.
     * @return discovered links.
     */
    public static List<Link> getLinks(final AnnotatedElement annotatedElement) {
        final List<Link> result = new ArrayList<>();
        result.addAll(extractLinks(annotatedElement, io.qameta.allure.Link.class, ResultsUtils::createLink));
        result.addAll(extractLinks(annotatedElement, io.qameta.allure.Issue.class, ResultsUtils::createLink));
        result.addAll(extractLinks(annotatedElement, io.qameta.allure.TmsLink.class, ResultsUtils::createLink));
        result.addAll(extractLinks(asList(annotatedElement.getDeclaredAnnotations()),
                asList(io.qameta.allure.Link.class,
                        io.qameta.allure.Issue.class,
                        io.qameta.allure.TmsLink.class)));
        return result;
    }

    /**
     * Shortcut for {@link #getLinks(Collection)}.
     *
     * @param annotations annotations to analyse.
     * @return discovered links.
     */
    public static List<Link> getLinks(final Annotation... annotations) {
        return getLinks(asList(annotations));
    }

    /**
     * Returns links from given annotations.
     *
     * @param annotations annotations to analyse.
     * @return discovered links.
     */
    public static List<Link> getLinks(final Collection<Annotation> annotations) {
        final List<Link> result = new ArrayList<>();
        result.addAll(extractLinks(annotations, io.qameta.allure.Link.class, ResultsUtils::createLink));
        result.addAll(extractLinks(annotations, io.qameta.allure.Issue.class, ResultsUtils::createLink));
        result.addAll(extractLinks(annotations, io.qameta.allure.TmsLink.class, ResultsUtils::createLink));
        result.addAll(extractLinks(annotations,
                asList(io.qameta.allure.Link.class, io.qameta.allure.Issue.class, io.qameta.allure.TmsLink.class)));
        return result;
    }

    /**
     * Returns labels created from Allure annotations specified on annotated element.
     * Shortcut for {@link #getLinks(Annotation...)}
     *
     * @param annotatedElement the element to search annotations on.
     * @return discovered labels.
     */
    public static Set<Label> getLabels(final AnnotatedElement annotatedElement) {
        return getLabels(annotatedElement.getDeclaredAnnotations());
    }

    /**
     * Shortcut for {@link #getLabels(Collection)}.
     *
     * @param annotations annotations to analyse.
     * @return discovered labels.
     */
    public static Set<Label> getLabels(final Annotation... annotations) {
        return getLabels(asList(annotations));
    }

    /**
     * Returns labels from given annotations.
     *
     * @param annotations annotations to analyse.
     * @return discovered labels.
     */
    public static Set<Label> getLabels(final Collection<Annotation> annotations) {
        return annotations.stream()
                .flatMap(AnnotationUtils::extractRepeatable)
                .map(AnnotationUtils::getMarks)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private static <T extends Annotation> Set<Link> extractLinks(final AnnotatedElement element,
                                                                 final Class<T> annotationType,
                                                                 final Function<T, Link> mapper) {

        return Stream.of(element.getAnnotationsByType(annotationType))
                .map(mapper)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> Set<Link> extractLinks(final Collection<Annotation> annotations,
                                                                 final Class<T> annotationType,
                                                                 final Function<T, Link> mapper) {
        return annotations.stream()
                .flatMap(AnnotationUtils::extractRepeatable)
                .filter(annotation -> annotationType.isAssignableFrom(annotation.annotationType()))
                .map(annotation -> (T) annotation)
                .map(mapper)
                .collect(Collectors.toSet());
    }

    private static Collection<? extends Link> extractLinks(final Collection<Annotation> annotations,
                                                           final List<Class<? extends Annotation>> ignore) {
        return annotations.stream()
                .flatMap(AnnotationUtils::extractRepeatable)
                .filter(annotation ->
                        Stream.of(annotation.annotationType().getAnnotationsByType(LabelAnnotation.class))
                                .anyMatch(it -> it.name().equals("link"))
                                && !ignore.contains(annotation.annotationType())
                )
                .flatMap(annotation -> AnnotationUtils.toLink(annotation).stream())
                .collect(Collectors.toSet());
    }

    private static Set<Link> toLink(final Annotation annotation) {
        try {
            final Method method = annotation.annotationType().getMethod(VALUE_METHOD_NAME);
            final Object object = method.invoke(annotation);
            return objectToStringStream(object)
                    .map(value -> ResultsUtils.createLink("", value, "", "custom"))
                    .collect(Collectors.toSet());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error(
                    "Invalid annotation {}: marker annotations should contains value() method",
                    annotation
            );
        }
        return Collections.emptySet();
    }

    private static Set<Label> getMarks(final Annotation annotation) {
        return Stream.of(annotation.annotationType().getAnnotationsByType(LabelAnnotation.class))
                .map(marker -> getLabel(annotation, marker))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private static Set<Label> getLabel(final Annotation annotation, final LabelAnnotation m) {
        if (Objects.equals(m.value(), LabelAnnotation.DEFAULT_VALUE)) {
            try {
                final Method method = annotation.annotationType().getMethod(VALUE_METHOD_NAME);
                final Object object = method.invoke(annotation);
                return objectToStringStream(object)
                        .map(value -> new Label().setName(m.name()).setValue(value))
                        .collect(Collectors.toSet());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error(
                        "Invalid annotation {}: marker annotations without value should contains value() method",
                        annotation
                );
                return Collections.emptySet();
            }
        }
        return Collections.singleton(new Label().setName(m.name()).setValue(m.value()));
    }

    @SuppressWarnings({
            "CyclomaticComplexity",
            "ReturnCount",
            "PMD.NcssCount",
            "PMD.CyclomaticComplexity"
    })
    private static Stream<String> objectToStringStream(final Object object) {
        if (Objects.nonNull(object) && object.getClass().isArray()) {
            if (object instanceof Object[]) {
                return Stream.of((Object[]) object).map(Objects::toString);
            } else if (object instanceof long[]) {
                return Stream.of((long[]) object).map(Objects::toString);
            } else if (object instanceof short[]) {
                return Stream.of((short[]) object).map(Objects::toString);
            } else if (object instanceof int[]) {
                return Stream.of((int[]) object).map(Objects::toString);
            } else if (object instanceof char[]) {
                return Stream.of((char[]) object).map(Objects::toString);
            } else if (object instanceof double[]) {
                return Stream.of((double[]) object).map(Objects::toString);
            } else if (object instanceof float[]) {
                return Stream.of((float[]) object).map(Objects::toString);
            } else if (object instanceof boolean[]) {
                return Stream.of((boolean[]) object).map(Objects::toString);
            } else if (object instanceof byte[]) {
                return Stream.of((byte[]) object).map(Objects::toString);
            }
        }
        return Stream.of(Objects.toString(object));
    }

    private static Stream<Annotation> extractRepeatable(final Annotation annotation) {
        if (isRepeatableWrapper(annotation)) {
            try {
                final Method method = annotation.annotationType().getMethod(VALUE_METHOD_NAME);
                return Stream.of((Annotation[]) method.invoke(annotation));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Could not extract repeatable annotation {}", annotation);
                return Stream.empty();
            }
        }
        return Stream.of(annotation);
    }

    private static boolean isRepeatableWrapper(final Annotation annotation) {
        return Stream.of(annotation.annotationType().getDeclaredMethods())
                .filter(method -> VALUE_METHOD_NAME.equalsIgnoreCase(method.getName()))
                .filter(method -> method.getReturnType().isArray())
                .anyMatch(method -> isRepeatable(method.getReturnType().getComponentType()));
    }

    private static boolean isRepeatable(final Class<?> annotationType) {
        return annotationType.isAnnotationPresent(Repeatable.class);
    }

}
