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
package io.qameta.allure.junit4;

import org.junit.runner.Description;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author charlie (Dmitry Baev).
 */
/* package-private */ final class AllureJunit4Utils {

    private static final Description CUCUMBER_CHECK_DESCRIPTION = Description
            .createSuiteDescription("check", new CucumberCheck());

    private static final String CUCUMBER_WORKING_DIR = Paths.get("").toUri().toString();
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    private AllureJunit4Utils() {
        throw new IllegalStateException("do not instance");
    }

    public static String getFullName(final Description description) {
        final Class<?> testClass = description.getTestClass();
        if (Objects.isNull(testClass)) {
            final UniqueIdExtract uniqueId = new UniqueIdExtract();
            //noinspection ResultOfMethodCallIgnored
            Description.createSuiteDescription("extract", uniqueId)
                    .equals(description);
            final String testCaseUri = uniqueId.id;
            if (Objects.nonNull(testCaseUri)) {
                if (testCaseUri.startsWith(CUCUMBER_WORKING_DIR)) {
                    return testCaseUri.substring(CUCUMBER_WORKING_DIR.length());
                }
                if (testCaseUri.startsWith(CLASSPATH_PREFIX)) {
                    return testCaseUri.substring(CLASSPATH_PREFIX.length());
                }
                if (testCaseUri.startsWith(FILE_PREFIX)) {
                    return testCaseUri.substring(FILE_PREFIX.length());
                }
                return testCaseUri;
            }
        }

        final String className = description.getClassName();
        final String methodName = description.getMethodName();
        return Objects.nonNull(methodName)
                ? String.format("%s.%s", className, methodName) : className;
    }

    public static boolean isCucumberTest(final Description description) {
        return CUCUMBER_CHECK_DESCRIPTION.equals(description);
    }

    /**
     * This is the Dummy class to check is {@link Description} object created
     * by Cucumber runner. We are using the fact that Cucumber runner creates
     * Descriptions with uniqueId of type <code>io.cucumber.junit.PickleRunners.PickleId</code>.
     */
    static class CucumberCheck implements Serializable {

        @Override
        public boolean equals(final Object obj) {
            if (Objects.isNull(obj)) {
                return false;
            }
            return "io.cucumber.junit.PickleRunners.PickleId"
                    .equals(obj.getClass().getCanonicalName());
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    /**
     * This is the Dummy class to extract <code>fUniqueId</code> field
     * from {@link Description} object.
     */
    @SuppressWarnings("VisibilityModifier")
    static class UniqueIdExtract implements Serializable {

        String id;

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(final Object obj) {
            id = Objects.toString(obj);
            return true;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
