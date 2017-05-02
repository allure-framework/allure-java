package io.qameta.allure.aspects;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.Arrays;

/**
 * Some utils that help process steps and attachments names and titles.
 */
final class Allure1AspectUtils {

    /**
     * Don't instance this class.
     */
    private Allure1AspectUtils() {
    }

    /**
     * Generate method in the following format: {methodName}[{param1}, {param2}, ...].
     */
    public static String getName(final String methodName, final Object... parameters) {
        return methodName + getParametersAsString(parameters);
    }

    /**
     * Convert array of given parameters to sting.
     */
    public static String getParametersAsString(final Object... parameters) {
        if (parameters == null || parameters.length == 0) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < parameters.length; i++) {
            builder.append(arrayToString(parameters[i]));
            if (i < parameters.length - 1) {
                builder.append(", ");
            }
        }
        return builder.append(']').toString();
    }

    /**
     * Generate title using name pattern. First step all "{method}" substrings will be replaced
     * with given method name. Then replace all "{i}" substrings with i-th parameter.
     */
    public static String getTitle(final String namePattern, final String methodName,
                                  final Object instance, final Object... parameters) {
        final String finalPattern = namePattern
                .replaceAll("\\{method\\}", methodName)
                .replaceAll("\\{this\\}", String.valueOf(instance));
        final int paramsCount = parameters == null ? 0 : parameters.length;
        final Object[] results = new Object[paramsCount];
        for (int i = 0; i < paramsCount; i++) {
            results[i] = arrayToString(parameters[i]);
        }

        return MessageFormat.format(finalPattern, results);
    }

    /**
     * {@link Arrays#toString(Object[])} with {@link Arrays#toString(Object[])} for array elements.
     */
    public static Object arrayToString(final Object obj) {
        if (obj != null && obj.getClass().isArray()) {
            final int len = Array.getLength(obj);
            final String[] strings = new String[len];
            for (int i = 0; i < len; i++) {
                strings[i] = String.valueOf(Array.get(obj, i));
            }
            return Arrays.toString(strings);
        } else {
            return obj;
        }
    }

}
