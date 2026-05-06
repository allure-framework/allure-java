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
package io.qameta.allure.assertj;

import io.qameta.allure.util.ObjectUtils;
import org.assertj.core.description.Description;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.getLambdaName;

/**
 * Renders AssertJ subjects and arguments into semantic step names.
 */
@SuppressWarnings("all")
final class AssertJValueRenderer {

    private static final String LAMBDA = "<lambda>";

    String renderSubject(final Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence || isSimple(value)) {
            return renderSimple(value);
        }
        if (value instanceof Collection) {
            return "Collection(size=" + ((Collection<?>) value).size() + ")";
        }
        if (value instanceof Map) {
            return "Map(size=" + ((Map<?, ?>) value).size() + ")";
        }
        if (value.getClass().isArray()) {
            return value.getClass().getComponentType().getSimpleName()
                    + "[](length=" + Array.getLength(value) + ")";
        }
        if (value instanceof Iterable) {
            return "Iterable";
        }
        return simpleClassName(value);
    }

    String renderOperation(final String methodName, final Object[] args) {
        if (args.length == 0) {
            return methodName + "()";
        }
        return methodName + "(" + renderArguments(methodName, args) + ")";
    }

    private String renderArguments(final String methodName, final Object[] args) {
        if (isDescriptionWithEmptyValues(args)) {
            return renderArgument(args[0]);
        }
        if (isSingleVarargToUnwrap(methodName, args)) {
            return renderArgument(Array.get(args[0], 0));
        }
        if (isSingleArrayArgument(args)) {
            return renderArray(args[0]);
        }
        return renderEach(args);
    }

    private boolean isDescriptionWithEmptyValues(final Object[] args) {
        return args.length == 2
                && args[1] != null
                && args[1].getClass().isArray()
                && Array.getLength(args[1]) == 0;
    }

    private boolean isSingleVarargToUnwrap(final String methodName, final Object[] args) {
        return args.length == 1
                && args[0] != null
                && args[0].getClass().isArray()
                && Array.getLength(args[0]) == 1
                && shouldUnwrapSingleVararg(methodName);
    }

    private boolean isSingleArrayArgument(final Object[] args) {
        return args.length == 1
                && args[0] != null
                && args[0].getClass().isArray();
    }

    private boolean shouldUnwrapSingleVararg(final String methodName) {
        return !methodName.contains("Any")
                && !methodName.contains("Exactly")
                && !methodName.contains("Only")
                && !methodName.contains("Sequence")
                && !methodName.contains("Subsequence")
                && !methodName.endsWith("In");
    }

    private String renderEach(final Object[] args) {
        final List<String> values = new ArrayList<>();
        for (Object arg : args) {
            values.add(renderArgument(arg));
        }
        return values.stream().collect(Collectors.joining(", "));
    }

    private String renderArgument(final Object value) {
        if (value == null) {
            return "null";
        }
        if (isLambda(value)) {
            return renderLambda(value);
        }
        if (value instanceof Description) {
            return renderSimple(value.toString());
        }
        if (value instanceof CharSequence || isSimple(value)) {
            return renderSimple(value);
        }
        if (value instanceof Collection) {
            return "Collection(size=" + ((Collection<?>) value).size() + ")";
        }
        if (value instanceof Map) {
            return "Map(size=" + ((Map<?, ?>) value).size() + ")";
        }
        if (value.getClass().isArray()) {
            return renderArray(value);
        }
        return simpleClassName(value);
    }

    private String renderArray(final Object array) {
        if (array instanceof byte[]) {
            return ObjectUtils.toString(array);
        }

        final int length = Array.getLength(array);
        if (array.getClass().getComponentType().isPrimitive()) {
            return ObjectUtils.toString(array);
        }
        if (allLambdas(array, length)) {
            return length == 1 ? renderLambda(Array.get(array, 0)) : lambdaList(array, length);
        }
        if (allSimple(array, length) || !array.getClass().getComponentType().isPrimitive()) {
            return renderObjectArray(array, length);
        }
        return array.getClass().getComponentType().getSimpleName() + "[](length=" + length + ")";
    }

    private String renderObjectArray(final Object array, final int length) {
        final List<String> values = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            values.add(renderArgument(Array.get(array, i)));
        }
        return values.stream().collect(Collectors.joining(", ", "[", "]"));
    }

    private boolean allLambdas(final Object array, final int length) {
        if (length == 0) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!isLambda(Array.get(array, i))) {
                return false;
            }
        }
        return true;
    }

    private String lambdaList(final Object array, final int length) {
        final List<String> values = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            values.add(renderLambda(Array.get(array, i)));
        }
        return values.stream().collect(Collectors.joining(", ", "[", "]"));
    }

    private boolean allSimple(final Object array, final int length) {
        for (int i = 0; i < length; i++) {
            final Object item = Array.get(array, i);
            if (item != null && !isSimple(item) && !(item instanceof CharSequence)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSimple(final Object value) {
        return value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum
                || value instanceof Path
                || value instanceof URI
                || value instanceof URL
                || value instanceof TemporalAccessor;
    }

    private boolean isLambda(final Object value) {
        final Class<?> type = value.getClass();
        return type.isSynthetic() || type.getName().contains("$$Lambda$");
    }

    private String renderLambda(final Object value) {
        return getLambdaName(value)
                .orElse(LAMBDA);
    }

    private String renderSimple(final Object value) {
        if (value instanceof CharSequence || value instanceof Character) {
            return "\"" + ObjectUtils.toString(value) + "\"";
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        return ObjectUtils.toString(value);
    }

    private String simpleClassName(final Object value) {
        final Class<?> type = value.getClass();
        if (type.isAnonymousClass()) {
            return type.getSuperclass().getSimpleName();
        }
        return type.getSimpleName();
    }
}
