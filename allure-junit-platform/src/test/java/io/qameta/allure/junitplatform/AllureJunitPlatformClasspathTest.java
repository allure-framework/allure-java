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
package io.qameta.allure.junitplatform;

import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;

class AllureJunitPlatformClasspathTest {

    private static final String KARATE_ADAPTER = "io.qameta.allure.karate.AllureKarate";
    private static final String KARATE_RUN_LISTENER = "io.karatelabs.core.RunListener";
    private static final Set<String> CHILD_FIRST_CLASSES = Set.of(
            AllureJunitPlatform.class.getName(),
            KARATE_ADAPTER
    );

    /**
     * Keeps JUnit Platform reporting available when an optional adapter links to a missing framework version.
     */
    @Test
    @Description
    void shouldIgnoreMissingOptionalFrameworkDependencyDuringClasspathProbe() {
        final ClassLoader classLoader = new MissingOptionalDependencyClassLoader(
                AllureJunitPlatformClasspathTest.class.getClassLoader()
        );

        assertThatCode(() -> Class.forName(AllureJunitPlatform.class.getName(), true, classLoader))
                .doesNotThrowAnyException();
    }

    private static final class MissingOptionalDependencyClassLoader extends ClassLoader {

        MissingOptionalDependencyClassLoader(final ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (KARATE_RUN_LISTENER.equals(name)) {
                throw new ClassNotFoundException(name);
            }
            if (!CHILD_FIRST_CLASSES.contains(name)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                final Class<?> loadedClass = findLoadedClass(name);
                final Class<?> result = loadedClass == null ? defineClassFromParent(name) : loadedClass;
                if (resolve) {
                    resolveClass(result);
                }
                return result;
            }
        }

        private Class<?> defineClassFromParent(final String name) throws ClassNotFoundException {
            final String resourceName = name.replace('.', '/') + ".class";
            try (InputStream input = getParent().getResourceAsStream(resourceName)) {
                if (input == null) {
                    throw new ClassNotFoundException(name);
                }
                final byte[] bytes = input.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
