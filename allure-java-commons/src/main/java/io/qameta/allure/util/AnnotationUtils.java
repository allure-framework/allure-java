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
package io.qameta.allure.util;

import io.qameta.allure.Flaky;
import io.qameta.allure.LabelAnnotation;
import io.qameta.allure.LinkAnnotation;
import io.qameta.allure.Muted;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.createLabel;
import static io.qameta.allure.util.ResultsUtils.createLink;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

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
     * Returns true if {@link io.qameta.allure.Flaky} annotation is present.
     *
     * @param annotatedElement the element to search annotations on.
     * @return true if {@link io.qameta.allure.Flaky} annotation is present, false otherwise.
     */
    public static boolean isFlaky(final AnnotatedElement annotatedElement) {
        return isAnnotationPresent(annotatedElement, Flaky.class);
    }

    /**
     * Returns true if {@link io.qameta.allure.Muted} annotation is present.
     *
     * @param annotatedElement the element to search annotations on.
     * @return true if {@link io.qameta.allure.Muted} annotation is present, false otherwise.
     */
    public static boolean isMuted(final AnnotatedElement annotatedElement) {
        return isAnnotationPresent(annotatedElement, Muted.class);
    }

    /**
     * Returns links created from Allure meta annotations specified on annotated element.
     *
     * @param annotatedElement the element to search annotations on.
     * @return discovered links.
     */
    public static Set<Link> getLinks(final AnnotatedElement annotatedElement) {
        return getLinks(getAnnotationsFrom(annotatedElement));
    }

    /**
     * Shortcut for {@link #getLinks(Collection)}.
     *
     * @param annotations annotations to analyse.
     * @return discovered links.
     */
    public static Set<Link> getLinks(final Annotation... annotations) {
        return getLinks(asList(annotations));
    }

    /**
     * Returns links from given annotations.
     *
     * @param annotations annotations to analyse.
     * @return discovered links.
     */
    public static Set<Link> getLinks(final Collection<Annotation> annotations) {
        return extractMetaAnnotations(LinkAnnotation.class, AnnotationUtils::extractLinks, annotations)
                .collect(Collectors.toSet());
    }

    /**
     * Returns labels created from Allure meta annotations specified on annotated element.
     * Shortcut for {@link #getLinks(Annotation...)}
     *
     * @param annotatedElement the element to search annotations on.
     * @return discovered labels.
     */
    public static Set<Label> getLabels(final AnnotatedElement annotatedElement) {
        return getLabels(getAnnotationsFrom(annotatedElement));
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
        return extractMetaAnnotations(LabelAnnotation.class, AnnotationUtils::extractLabels, annotations)
                .collect(Collectors.toSet());
    }

    private static <T extends Annotation> boolean isAnnotationPresent(final AnnotatedElement annotatedElement, Class<T> annotationClass) {
        boolean isPresent = annotatedElement.isAnnotationPresent(annotationClass);
        if (!isPresent && annotatedElement instanceof Class<?>) {
            Annotation[] packageAnnotations = PackageUtil.getPackageAnnotations((Class<?>) annotatedElement);
            return stream(packageAnnotations).anyMatch(a -> a.annotationType().equals(annotationClass));
        }

        return isPresent;
    }

    private static Annotation[] getAnnotationsFrom(AnnotatedElement annotatedElement) {
        Annotation[] result = annotatedElement.getAnnotations();
        if (annotatedElement instanceof Class<?>) {
            Annotation[] packageAnnotations = PackageUtil.getPackageAnnotations((Class<?>) annotatedElement);
            List<Annotation> annotationList = new ArrayList<>(asList(result));
            List<Annotation> packageAnnotationList = new ArrayList<>(asList(packageAnnotations));
            annotationList.addAll(packageAnnotationList);
            result = annotationList.toArray(new Annotation[]{});
        }

        return result;
    }

    private static <T, U extends Annotation> Stream<T> extractMetaAnnotations(
            final Class<U> annotationType,
            final BiFunction<U, Annotation, Stream<T>> mapper,
            final Collection<Annotation> candidates) {
        final Set<Annotation> visited = new HashSet<>();
        return candidates.stream()
                .flatMap(AnnotationUtils::extractRepeatable)
                .flatMap(candidate -> extractMetaAnnotations(annotationType, mapper, candidate, visited));
    }

    private static <T, U extends Annotation> Stream<T> extractMetaAnnotations(
            final Class<U> annotationType,
            final BiFunction<U, Annotation, Stream<T>> mapper,
            final Annotation candidate,
            final Set<Annotation> visited) {
        if (!isInJavaLangAnnotationPackage(candidate.annotationType()) && visited.add(candidate)) {
            final Stream<T> children = Stream.of(candidate.annotationType().getAnnotations())
                    .flatMap(AnnotationUtils::extractRepeatable)
                    .flatMap(annotation -> extractMetaAnnotations(
                            annotationType, mapper, annotation, visited));
            final Stream<T> current = Stream.of(candidate.annotationType().getAnnotationsByType(annotationType))
                    .flatMap(marker -> mapper.apply(marker, candidate));
            return Stream.concat(current, children);
        }
        return Stream.empty();
    }

    private static Stream<Label> extractLabels(final LabelAnnotation m, final Annotation annotation) {
        if (Objects.equals(m.value(), LabelAnnotation.DEFAULT_VALUE)) {
            return callValueMethod(annotation)
                    .map(value -> createLabel(m.name(), value));
        }
        return Stream.of(createLabel(m.name(), m.value()));
    }

    private static Stream<Link> extractLinks(final LinkAnnotation m, final Annotation annotation) {
        // this is required as Link annotation uses name attribute as value alias.
        if (annotation instanceof io.qameta.allure.Link) {
            return Stream.of(createLink((io.qameta.allure.Link) annotation));
        }

        if (Objects.equals(m.value(), LinkAnnotation.DEFAULT_VALUE)) {
            return callValueMethod(annotation)
                    .map(value -> createLink(value, null, m.url(), m.type()));
        }
        return Stream.of(createLink(m.value(), null, m.url(), m.type()));
    }

    private static Stream<String> callValueMethod(final Annotation annotation) {
        try {
            final Method method = annotation.annotationType().getMethod(VALUE_METHOD_NAME);
            final Object object = method.invoke(annotation);
            return objectToStringStream(object);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error(
                    "Invalid annotation {}: marker annotations without value should contains value() method",
                    annotation,
                    e
            );
            return Stream.empty();
        }
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

    private static boolean isInJavaLangAnnotationPackage(final Class<? extends Annotation> annotationType) {
        return annotationType != null && annotationType.getName().startsWith("java.lang.annotation");
    }

    /**
     * Extracts annotations from packages hierarchically by given classes
     *
     * @author TikhomirovSergey (Sergey Tikhomirov).
     */
    static class PackageUtil {

        static Annotation[] getPackageAnnotations(Class<?> clz) {
            Objects.requireNonNull(clz, "Class should not be a null value");
            return getPackageAnnotations(clz.getPackage().getName());
        }

        static Annotation[] getPackageAnnotations(String packageName) {
            Objects.requireNonNull(packageName, "Package name should not be a null value");

            //the code below would look better if allure supported Java from 9 and higher versions
            Class<?> packInfo;
            try {
                packInfo = Class.forName(packageName + ".package-info", false, PackageUtil.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                packInfo = null;
            }

            Annotation[] annotations = ofNullable(packInfo).map(clazz -> clazz.getPackage().getAnnotations()).orElse(new Annotation[]{});
            String[] pathElements = packageName.split("[.]");
            if (pathElements.length == 1) {
                return annotations;
            }

            Annotation[] upperPackageAnnotations = getPackageAnnotations(join(".", copyOfRange(pathElements, 0, pathElements.length - 1)));
            List<Annotation> annotationList = new ArrayList<>(asList(annotations));
            List<Annotation> result = new ArrayList<>(asList(upperPackageAnnotations));
            result.addAll(annotationList);
            return result.toArray(new Annotation[] {});
        }
    }

}
