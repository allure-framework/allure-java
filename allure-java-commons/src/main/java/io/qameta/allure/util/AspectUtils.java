package io.qameta.allure.util;

import io.qameta.allure.model.Parameter;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.qameta.allure.util.NamingUtils.processNameTemplate;

/**
 * @author charlie (Dmitry Baev).
 */
public final class AspectUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AspectUtils.class);

    private AspectUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static String getName(final String nameTemplate,
                                 final MethodSignature methodSignature,
                                 final Object... args) {
        return Optional.of(nameTemplate)
                .filter(v -> !v.isEmpty())
                .map(value -> processNameTemplate(value, getParametersMap(methodSignature, args)))
                .orElseGet(methodSignature::getName);
    }

    public static Map<String, Object> getParametersMap(final MethodSignature signature, final Object... args) {
        final String[] parameterNames = signature.getParameterNames();
        final Map<String, Object> params = new HashMap<>();
        params.put("method", signature.getName());
        for (int i = 0; i < Math.max(parameterNames.length, args.length); i++) {
            params.put(parameterNames[i], args[i]);
            params.put(Integer.toString(i), args[i]);
        }
        return params;
    }

    public static List<Parameter> getParameters(final MethodSignature signature, final Object... args) {
        return IntStream.range(0, args.length).mapToObj(index -> {
            final String name = signature.getParameterNames()[index];
            final String value = objectToString(args[index]);
            return new Parameter().setName(name).setValue(value);
        }).collect(Collectors.toList());
    }

    public static String objectToString(final Object object) {
        try {
            if (Objects.nonNull(object) && (object instanceof Object[])) {
                return Arrays.toString((Object[]) object);
            }
            return Objects.toString(object);
        } catch (Exception e) {
            LOGGER.error("Could not convert object to string", e);
            return "<NPE>";
        }
    }
}
