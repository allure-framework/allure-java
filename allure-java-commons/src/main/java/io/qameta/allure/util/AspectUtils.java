package io.qameta.allure.util;

import io.qameta.allure.model.Parameter;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * @author charlie (Dmitry Baev).
 */
public final class AspectUtils {

    private AspectUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static Parameter[] getParameters(final MethodSignature signature, final Object... args) {
        return IntStream.range(0, args.length).mapToObj(index -> {
            final String name = signature.getParameterNames()[index];
            final String value = objectToString(args[index]);
            return new Parameter().withName(name).withValue(value);
        }).toArray(Parameter[]::new);
    }

    public static Map<String, Object> getParametersMap(final MethodSignature signature, final Object... args) {
        final String[] parameterNames = signature.getParameterNames();
        final Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < Math.max(parameterNames.length, args.length); i++) {
            params.put(parameterNames[i], args[i]);
        }
        return params;
    }

    public static String objectToString(final Object object) {
        if (Objects.nonNull(object) && (object instanceof Object[])) {
            return Arrays.toString((Object[]) object);
        }
        return Objects.toString(object);
    }
}
