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
package io.qameta.allure.jupiter;

import io.qameta.allure.model.Parameter;
import io.qameta.allure.util.ParameterUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.support.ParameterDeclaration;
import org.junit.jupiter.params.support.ParameterInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Reads the legacy JUnit Jupiter {@link ParameterInfo} API to report arguments with junit-jupiter-params 5.13. The
 * API is deprecated in JUnit 6 — every reference to it stays inside this class, which is loaded only after a
 * classpath check and only when the replacement API is unavailable.
 */
@SuppressWarnings({"deprecation", "removal"})
/* package-private */ final class AllureJupiterLegacyParameterInfoSupport {

    private AllureJupiterLegacyParameterInfoSupport() {
        throw new IllegalStateException("do not instance");
    }

    /**
     * Collects all arguments available for the current parameterized invocation as Allure parameters.
     *
     * @param extensionContext the extension context of the running test
     * @return the Allure parameters, empty when the test is not parameterized
     */
    static List<Parameter> getParameters(final ExtensionContext extensionContext) {
        return collectParameters(extensionContext, info -> true);
    }

    /**
     * Collects the arguments of every enclosing parameterized class invocation as Allure parameters, outermost
     * first. The test method's own arguments are excluded: they are reported from the invocation context.
     *
     * @param extensionContext the extension context of the running test
     * @param testMethod       the running test method
     * @return the class-level Allure parameters, empty when the test runs outside parameterized classes
     */
    static List<Parameter> getClassParameters(final ExtensionContext extensionContext,
                                              final Method testMethod) {
        return collectParameters(
                extensionContext,
                info -> !testMethod.equals(info.getDeclarations().getSourceElement())
        );
    }

    private static List<Parameter> collectParameters(final ExtensionContext extensionContext,
                                                     final Predicate<ParameterInfo> filter) {
        final List<Parameter> result = new ArrayList<>();
        final Set<ParameterInfo> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Optional<ExtensionContext> current = Optional.of(extensionContext);
        while (current.isPresent()) {
            final ParameterInfo info = ParameterInfo.get(current.get());
            if (Objects.isNull(info)) {
                break;
            }
            if (visited.add(info) && filter.test(info)) {
                // contexts are walked inner to outer: prepend so outer class arguments come first
                result.addAll(0, toParameters(info));
            }
            current = current.get().getParent();
        }
        return result;
    }

    private static List<Parameter> toParameters(final ParameterInfo info) {
        final ArgumentsAccessor arguments = info.getArguments();
        final List<Parameter> result = new ArrayList<>();
        for (final ParameterDeclaration declaration : info.getDeclarations().getAll()) {
            final int index = declaration.getParameterIndex();
            if (index < 0 || index >= arguments.size()) {
                continue;
            }
            result.add(
                    ParameterUtils.createParameter(
                            declaration.getAnnotatedElement(),
                            arguments.get(index),
                            declaration.getParameterName().orElse("arg" + index)
                    )
            );
        }
        return result;
    }
}
