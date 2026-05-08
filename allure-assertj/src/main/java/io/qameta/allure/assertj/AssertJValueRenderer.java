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

import io.qameta.allure.model.Parameter;
import io.qameta.allure.util.ObjectUtils;
import org.assertj.core.description.Description;
import org.assertj.core.groups.Tuple;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.getLambdaName;

/**
 * Renders AssertJ subjects and arguments into semantic step names.
 */
@SuppressWarnings("all")
final class AssertJValueRenderer {

    private static final int STEP_NAME_LIMIT = 1000;

    private static final int INLINE_VALUE_LIMIT = 3;

    private static final String LAMBDA = "<lambda>";

    private static final String TRUNCATED = "...";

    String renderSubject(final Object value) {
        return truncateStepName(renderSubjectValue(value));
    }

    String renderOperation(final String methodName, final Object[] args) {
        return truncateStepName(renderOperationName(methodName, args));
    }

    List<Parameter> renderParameters(final String methodName, final Object[] args) {
        final Object[] values = parameterArguments(methodName, args);
        if (values.length == 0) {
            return Collections.emptyList();
        }

        final String renderedOperation = renderOperation(methodName, args);
        final List<Parameter> parameters = new ArrayList<>();
        for (int index = 0; index < values.length; index++) {
            final String value = renderParameterValue(values[index]);
            if (renderedOperation.contains(value)) {
                continue;
            }
            parameters.add(new Parameter()
                    .setName(parameterName(methodName, index))
                    .setValue(value)
                    .setMode(Parameter.Mode.DEFAULT));
        }
        return parameters;
    }

    static String truncateStepName(final String value) {
        if (value == null || value.length() <= STEP_NAME_LIMIT) {
            return value;
        }
        return value.substring(0, STEP_NAME_LIMIT - TRUNCATED.length()) + TRUNCATED;
    }

    private String renderSubjectValue(final Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence || isSimple(value)) {
            return renderSimple(value);
        }
        if (value instanceof Collection) {
            if (isInlineCollection((Collection<?>) value)) {
                return renderCollectionValue((Collection<?>) value);
            }
            return renderCollectionSubject((Collection<?>) value);
        }
        if (value instanceof Map) {
            return "map with " + renderEntryCount(((Map<?, ?>) value).size());
        }
        if (value.getClass().isArray()) {
            return renderArraySubject(value);
        }
        if (value instanceof Iterable) {
            return "iterable";
        }
        return simpleClassName(value);
    }

    private Object[] parameterArguments(final String methodName, final Object[] args) {
        if (isDescriptionWithEmptyValues(args)) {
            return new Object[]{args[0]};
        }
        if (isSingleVarargToUnwrap(methodName, args)) {
            return new Object[]{Array.get(args[0], 0)};
        }
        return args;
    }

    private String parameterName(final String methodName, final int index) {
        if ("hasFieldOrPropertyWithValue".equals(methodName)) {
            return index == 0 ? "field or property" : "expected value";
        }

        if (index > 0) {
            return "argument " + (index + 1);
        }

        switch (methodName) {
            case "as":
                return "description";
            case "asInstanceOf":
            case "first":
            case "singleElement":
                return "factory";
            case "extracting":
            case "flatExtracting":
                return "extractor";
            case "hasSize":
                return "expected size";
            case "satisfies":
                return "condition";
            case "contains":
            case "containsExactly":
            case "containsExactlyInAnyOrder":
            case "endsWith":
            case "isEqualTo":
            case "startsWith":
                return "expected";
            default:
                return "argument 1";
        }
    }

    private String renderParameterValue(final Object value) {
        return renderArgument(value);
    }

    private String renderOperationName(final String methodName, final Object[] args) {
        if (args.length == 0) {
            return readableMethodName(methodName);
        }

        final String arguments = renderArguments(methodName, args);
        switch (methodName) {
            case "as":
                return "described as " + arguments;
            case "asInstanceOf":
                return "as instance of " + arguments;
            case "contains":
                return "contains " + arguments;
            case "containsExactly":
                return "contains exactly " + arguments;
            case "containsExactlyInAnyOrder":
                return "contains exactly in any order " + arguments;
            case "endsWith":
                return "ends with " + arguments;
            case "extracting":
                return "extracts " + arguments;
            case "flatExtracting":
                return "flat extracts " + arguments;
            case "first":
                return "first element as " + arguments;
            case "hasFieldOrPropertyWithValue":
                return renderHasFieldOrPropertyWithValue(args);
            case "hasSize":
                return "has size " + arguments;
            case "isEqualTo":
                return "is equal to " + arguments;
            case "singleElement":
                return "single element as " + arguments;
            case "startsWith":
                return "starts with " + arguments;
            case "satisfies":
                return "satisfies " + arguments;
            default:
                return readableMethodName(methodName) + " " + arguments;
        }
    }

    private String renderHasFieldOrPropertyWithValue(final Object[] args) {
        if (args.length != 2) {
            return "has field or property with value " + renderEach(args);
        }
        return "has field or property " + renderArgument(args[0]) + " with value " + renderArgument(args[1]);
    }

    private String readableMethodName(final String methodName) {
        if (methodName.startsWith("is") && methodName.length() > 2 && Character.isUpperCase(methodName.charAt(2))) {
            return "is " + splitCamelCase(methodName.substring(2));
        }
        if (methodName.startsWith("has") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3))) {
            return "has " + splitCamelCase(methodName.substring(3));
        }
        return splitCamelCase(methodName);
    }

    private String splitCamelCase(final String value) {
        return value
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .toLowerCase();
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
        if (value instanceof Tuple) {
            return renderTuple((Tuple) value);
        }
        if (value instanceof CharSequence || isSimple(value)) {
            return renderSimple(value);
        }
        if (value instanceof Collection) {
            if (isInlineCollection((Collection<?>) value)) {
                return renderCollectionValue((Collection<?>) value);
            }
            return renderCollectionSubject((Collection<?>) value);
        }
        if (value instanceof Map) {
            return "map with " + renderEntryCount(((Map<?, ?>) value).size());
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

    private String renderCollectionSubject(final Collection<?> value) {
        final int size = value.size();
        if (size == 0) {
            return "empty collection";
        }
        return commonElementType(value)
                .map(type -> renderElementCount(size, type))
                .orElseGet(() -> renderItemCount(size));
    }

    private String renderArraySubject(final Object array) {
        final int length = Array.getLength(array);
        if (isInlineArray(array, length)) {
            return renderArrayValue(array, length);
        }
        if (array instanceof byte[]) {
            return "byte array with " + renderByteCount(length);
        }
        return renderElementCount(length, array.getClass().getComponentType());
    }

    private boolean isInlineCollection(final Collection<?> value) {
        return value.size() <= INLINE_VALUE_LIMIT && allInlineValues(value);
    }

    private boolean allInlineValues(final Collection<?> value) {
        for (Object item : value) {
            if (!isInlineValue(item)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInlineValue(final Object value) {
        return value == null
                || isLambda(value)
                || value instanceof Description
                || isInlineTuple(value)
                || value instanceof CharSequence
                || isSimple(value);
    }

    private boolean isInlineTuple(final Object value) {
        if (!(value instanceof Tuple)) {
            return false;
        }
        final Object[] values = ((Tuple) value).toArray();
        return isInlineArray(values, values.length);
    }

    private String renderTuple(final Tuple tuple) {
        final Object[] values = tuple.toArray();
        return renderObjectArray(values, values.length)
                .replaceFirst("^\\[", "(")
                .replaceFirst("]$", ")");
    }

    private String renderCollectionValue(final Collection<?> value) {
        final List<String> values = new ArrayList<>();
        for (Object item : value) {
            values.add(renderArgument(item));
        }
        return values.stream().collect(Collectors.joining(", ", "[", "]"));
    }

    private boolean isInlineArray(final Object array, final int length) {
        if (length > INLINE_VALUE_LIMIT || array instanceof byte[]) {
            return false;
        }
        if (array.getClass().getComponentType().isPrimitive()) {
            return true;
        }
        for (int i = 0; i < length; i++) {
            if (!isInlineValue(Array.get(array, i))) {
                return false;
            }
        }
        return true;
    }

    private String renderArrayValue(final Object array, final int length) {
        if (array.getClass().getComponentType().isPrimitive()) {
            return ObjectUtils.toString(array);
        }
        return renderObjectArray(array, length);
    }

    private Optional<Class<?>> commonElementType(final Collection<?> value) {
        Class<?> result = null;
        for (Object item : value) {
            if (item == null) {
                continue;
            }
            final Class<?> itemType = elementTypeOf(item);
            if (result == null) {
                result = itemType;
            } else if (!result.equals(itemType)) {
                return java.util.Optional.empty();
            }
        }
        return java.util.Optional.ofNullable(result);
    }

    private Class<?> elementTypeOf(final Object item) {
        if (item instanceof Collection) {
            return Collection.class;
        }
        if (item instanceof Map) {
            return Map.class;
        }
        return item.getClass();
    }

    private String renderElementCount(final int size, final Class<?> type) {
        if (String.class.equals(type)) {
            return size + " " + pluralize("string", size);
        }
        if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            return size + " " + pluralize("boolean", size);
        }
        if (Character.class.equals(type) || Character.TYPE.equals(type)) {
            return size + " " + pluralize("character", size);
        }
        if (Number.class.isAssignableFrom(type) || type.isPrimitive() && !Boolean.TYPE.equals(type)
                && !Character.TYPE.equals(type)) {
            return size + " " + pluralize("number", size);
        }
        if (Collection.class.equals(type)) {
            return size + " " + pluralize("collection", size);
        }
        if (Map.class.equals(type)) {
            return size + " " + pluralize("map", size);
        }
        return size + " " + type.getSimpleName() + " " + pluralize("item", size);
    }

    private String renderItemCount(final int size) {
        return size + " " + pluralize("item", size);
    }

    private String renderEntryCount(final int size) {
        return size + " " + pluralize("entry", size);
    }

    private String renderByteCount(final int size) {
        return size + " " + pluralize("byte", size);
    }

    private String pluralize(final String word, final int count) {
        return count == 1 ? word : word + "s";
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
