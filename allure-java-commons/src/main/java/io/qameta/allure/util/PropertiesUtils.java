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
        if (classLoader.getResource(ALLURE_PROPERTIES_FILE) != null) {
            try (InputStream stream = classLoader.getResourceAsStream(ALLURE_PROPERTIES_FILE)) {
                properties.load(stream);
            } catch (IOException e) {
                LOGGER.error("Error while reading allure.properties file from classpath: %s", e.getMessage());
            }
        }
    }

}
