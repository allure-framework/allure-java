package io.qameta.allure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
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
        if (Objects.nonNull(ClassLoader.getSystemResource(ALLURE_PROPERTIES_FILE))) {
            try (InputStream stream = ClassLoader.getSystemResourceAsStream(ALLURE_PROPERTIES_FILE)) {
                properties.load(stream);
            } catch (IOException e) {
                LOGGER.error("Error while reading allure.properties file from classpath: %s", e.getMessage());
            }
        }
        properties.putAll(System.getProperties());
        return properties;
    }

}
