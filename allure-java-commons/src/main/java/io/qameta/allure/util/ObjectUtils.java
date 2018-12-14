package io.qameta.allure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author charlie (Dmitry Baev).
 */
public final class ObjectUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectUtils.class);

    /**
     * Do not instance.
     */
    private ObjectUtils() {
        throw new IllegalStateException("do not instance");
    }

    /**
     * Returns string representation of given object. Pretty prints arrays.
     *
     * @param object the given object.
     * @return the string representation of given object.
     */
    @SuppressWarnings({
            "CyclomaticComplexity",
            "ReturnCount",
            "PMD.NcssCount",
            "PMD.CyclomaticComplexity"
    })
    public static String toString(final Object object) {
        try {
            if (Objects.nonNull(object) && object.getClass().isArray()) {
                if (object instanceof Object[]) {
                    return Arrays.toString((Object[]) object);
                } else if (object instanceof long[]) {
                    return Arrays.toString((long[]) object);
                } else if (object instanceof short[]) {
                    return Arrays.toString((short[]) object);
                } else if (object instanceof int[]) {
                    return Arrays.toString((int[]) object);
                } else if (object instanceof char[]) {
                    return Arrays.toString((char[]) object);
                } else if (object instanceof double[]) {
                    return Arrays.toString((double[]) object);
                } else if (object instanceof float[]) {
                    return Arrays.toString((float[]) object);
                } else if (object instanceof boolean[]) {
                    return Arrays.toString((boolean[]) object);
                } else if (object instanceof byte[]) {
                    return Arrays.toString((byte[]) object);
                }
            }
            return Objects.toString(object);
        } catch (Exception e) {
            LOGGER.error("Could not convert object to string", e);
            return "<NPE>";
        }
    }
}
