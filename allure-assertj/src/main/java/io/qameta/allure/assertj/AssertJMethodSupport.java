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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps method-name decisions out of the aspect and recorder flow.
 */
final class AssertJMethodSupport {

    private static final String AS = "as";
    private static final String DESCRIBED_AS = "describedAs";

    private static final List<String> IGNORED_METHODS = Arrays.asList(
            "actual",
            "descriptionText",
            "equals",
            "getWritableAssertionInfo",
            "hashCode",
            "toString"
    );

    private static final Set<String> NAVIGATION_METHODS = new HashSet<>(Arrays.asList(
            "asBase64Decoded",
            "asBoolean",
            "asByte",
            "asDouble",
            "asFloat",
            "asInstanceOf",
            "asInt",
            "asList",
            "asLong",
            "asShort",
            "asString",
            "bytes",
            "decodedAsBase64",
            "element",
            "elements",
            "extracting",
            "extractingResultOf",
            "first",
            "flatExtracting",
            "flatMap",
            "last",
            "map",
            "rootCause",
            "singleElement",
            "size",
            "usingRecursiveAssertion",
            "usingRecursiveComparison"
    ));

    private AssertJMethodSupport() {
        throw new IllegalStateException("do not instantiate");
    }

    static boolean isIgnored(final String methodName) {
        return IGNORED_METHODS.contains(methodName);
    }

    static String normalize(final String methodName) {
        final int accessorIndex = methodName.indexOf("$accessor$");
        if (accessorIndex > 0) {
            return methodName.substring(0, accessorIndex);
        }
        if (DESCRIBED_AS.equals(methodName)) {
            return AS;
        }
        return methodName;
    }

    static boolean isDescription(final String methodName) {
        return AS.equals(methodName) || DESCRIBED_AS.equals(methodName);
    }

    static boolean isNavigation(final String methodName) {
        return NAVIGATION_METHODS.contains(methodName);
    }
}
