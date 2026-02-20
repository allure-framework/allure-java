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
package io.qameta.allure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Internal service loader util.
 *
 * @see ServiceLoader
 * @since 2.0
 */
public final class ServiceLoaderUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLoaderUtils.class);

    private ServiceLoaderUtils() {
        throw new IllegalStateException("Do not instance");
    }

    /**
     * Load implementation by given type.
     *
     * @param <T>         type of implementation.
     * @param type        the type of implementation to load.
     * @param classLoader the class loader to search for implementations.
     * @return loaded implementations.
     */
    public static <T> List<T> load(final Class<T> type, final ClassLoader classLoader) {
        final List<T> loaded = new ArrayList<>();
        final Iterator<T> iterator = ServiceLoader.load(type, classLoader).iterator();
        while (hasNextSafely(iterator)) {
            try {
                final T next = iterator.next();
                loaded.add(next);
                LOGGER.debug("Found {}", type);
            } catch (Exception e) {
                LOGGER.error("Could not load {}: {}", type, e);
            }
        }
        return loaded;
    }

    /**
     * Safely check for <pre>iterator.hasNext()</pre>.
     *
     * @param iterator specified iterator to check he presence of next element
     * @return {@code true} if the iteration has more elements, false otherwise
     */
    private static boolean hasNextSafely(final Iterator iterator) {
        try {
            /* Throw a ServiceConfigurationError if a provider-configuration file violates the specified format,
            or if it names a provider class that cannot be found and instantiated, or if the result of
            instantiating the class is not assignable to the service type, or if any other kind of exception
            or error is thrown as the next provider is located and instantiated.
            @see http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html#iterator()
            */
            return iterator.hasNext();
        } catch (Exception e) {
            LOGGER.error("iterator.hasNext() failed", e);
            return false;
        }
    }
}
