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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The collection of properties utils methods.
 */
public final class PropertiesUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtils.class);

    private static final String ALLURE_PROPERTIES_FILE = "allure.properties";

    private PropertiesUtils() {
    }

    public static Properties loadAllureProperties() {
        final Properties properties = new Properties();
        loadPropertiesFrom(ClassLoader.getSystemClassLoader(), properties);
        loadPropertiesFrom(Thread.currentThread().getContextClassLoader(), properties);
        properties.putAll(System.getProperties());
        return properties;
    }

    private static void loadPropertiesFrom(final ClassLoader classLoader, final Properties properties) {
        try (InputStream stream = classLoader.getResourceAsStream(ALLURE_PROPERTIES_FILE)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            LOGGER.error("Error while reading allure.properties file from classpath: {}", e.getMessage());
        }
    }
}
