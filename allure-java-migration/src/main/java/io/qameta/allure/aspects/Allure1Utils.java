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
package io.qameta.allure.aspects;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.util.ResultsUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Some utils that help process steps and attachments names and titles.
 */
final class Allure1Utils {

    private static final String SEVERITY_LABEL = "severity";

    private static final String FEATURE_LABEL = "feature";

    private static final String STORY_LABEL = "story";

    /**
     * Don't instance this class.
     */
    private Allure1Utils() {
    }

    /**
     * Generate method in the following format: {methodName}[{param1}, {param2}, ...].
     */
    public static String getName(final String methodName, final Object... parameters) {
        return methodName + getParametersAsString(parameters);
    }

    /**
     * Convert array of given parameters to sting.
     */
    public static String getParametersAsString(final Object... parameters) {
        if (parameters == null || parameters.length == 0) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < parameters.length; i++) {
            builder.append(arrayToString(parameters[i]));
            if (i < parameters.length - 1) {
                builder.append(", ");
            }
        }
        return builder.append(']').toString();
    }

    /**
     * Generate title using name pattern. First step all "{method}" substrings will be replaced
     * with given method name. Then replace all "{i}" substrings with i-th parameter.
     */
    public static String getTitle(final String namePattern, final String methodName,
                                  final Object instance, final Object... parameters) {
        final String finalPattern = namePattern
                .replaceAll("\\{method}", methodName)
                .replaceAll("\\{this}", String.valueOf(instance));
        final int paramsCount = parameters == null ? 0 : parameters.length;
        final Object[] results = new Object[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            results[i] = arrayToString(parameters[i]);
        }

        return MessageFormat.format(finalPattern, results);
    }

    /**
     * {@link Arrays#toString(Object[])} with {@link Arrays#toString(Object[])} for array elements.
     */
    public static Object arrayToString(final Object obj) {
        if (obj != null && obj.getClass().isArray()) {
            final int len = Array.getLength(obj);
            final String[] strings = new String[len];
            for (int i = 0; i < len; i++) {
                strings[i] = String.valueOf(Array.get(obj, i));
            }
            return Arrays.toString(strings);
        } else {
            return obj;
        }
    }

    public static <T extends Annotation> List<Label> getLabels(final Method method, final Class<T> annotation,
                                                               final Function<T, List<Label>> extractor) {
        final List<Label> labels = new ArrayList<>();
        labels.addAll(getLabels((AnnotatedElement) method, annotation, extractor));
        labels.addAll(getLabels(method.getDeclaringClass(), annotation, extractor));
        return labels;
    }

    private static <T extends Annotation> List<Label> getLabels(final AnnotatedElement element,
                                                                final Class<T> annotation,
                                                                final Function<T, List<Label>> extractor) {
        return element.isAnnotationPresent(annotation)
                ? extractor.apply(element.getAnnotation(annotation))
                : Collections.emptyList();
    }

    public static <T extends Annotation> List<Link> getLinks(final Method method, final Class<T> annotation,
                                                             final Function<T, List<Link>> extractor) {
        final List<Link> labels = new ArrayList<>();
        labels.addAll(getLinks((AnnotatedElement) method, annotation, extractor));
        labels.addAll(getLinks(method.getDeclaringClass(), annotation, extractor));
        return labels;
    }

    private static <T extends Annotation> List<Link> getLinks(final AnnotatedElement element,
                                                              final Class<T> annotation,
                                                              final Function<T, List<Link>> extractor) {
        return element.isAnnotationPresent(annotation)
                ? extractor.apply(element.getAnnotation(annotation))
                : Collections.emptyList();
    }

    public static List<Label> createLabels(final Stories stories) {
        return Arrays.stream(stories.value())
                .map(value -> new Label().setName(STORY_LABEL).setValue(value))
                .collect(Collectors.toList());
    }

    public static List<Label> createLabels(final Features features) {
        return Arrays.stream(features.value())
                .map(value -> new Label().setName(FEATURE_LABEL).setValue(value))
                .collect(Collectors.toList());
    }

    public static List<Label> createLabels(final Severity severity) {
        return Collections.singletonList(new Label().setName(SEVERITY_LABEL).setValue(severity.value().value()));
    }

    public static List<Link> createLinks(final Issues issues) {
        return Arrays.stream(issues.value())
                .map(Allure1Utils::createLink)
                .collect(Collectors.toList());
    }

    public static List<Link> createLinks(final Issue issue) {
        return Collections.singletonList(createLink(issue));
    }

    public static List<Link> createLinks(final TestCaseId issue) {
        return Collections.singletonList(ResultsUtils.createTmsLink(issue.value()));
    }

    private static Link createLink(final Issue issue) {
        return ResultsUtils.createIssueLink(issue.value());
    }

}
