package io.qameta.allure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.joor.Reflect.on;

/**
 * @author charlie (Dmitry Baev).
 */
public final class NamingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamingUtils.class);

    private static final Collector<CharSequence, ?, String> JOINER = Collectors.joining(", ", "[", "]");

    private NamingUtils() {
        throw new IllegalStateException("Do not instance");
    }

    public static String processNameTemplate(final String template, final Map<String, Object> params) {
        final Matcher matcher = Pattern.compile("\\{([^}]*)}").matcher(template);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            final String pattern = matcher.group(1);
            final String replacement = processPattern(pattern, params).orElseGet(matcher::group);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @SuppressWarnings("ReturnCount")
    private static Optional<String> processPattern(final String pattern, final Map<String, Object> params) {
        if (pattern.isEmpty()) {
            LOGGER.error("Could not process empty pattern");
            return Optional.empty();
        }
        final String[] parts = pattern.split("\\.");
        final String parameterName = parts[0];
        if (!params.containsKey(parameterName)) {
            LOGGER.error("Could not find parameter " + parameterName);
            return Optional.empty();
        }
        final Object param = params.get(parameterName);
        return Optional.ofNullable(extractProperties(param, parts, 1));
    }

    @SuppressWarnings("ReturnCount")
    private static String extractProperties(final Object object, final String[] parts, final int index) {
        if (Objects.isNull(object)) {
            return "null";
        }
        if (index < parts.length) {
            if (object instanceof Object[]) {
                return Stream.of((Object[]) object)
                        .map(child -> extractProperties(child, parts, index))
                        .collect(JOINER);
            }
            if (object instanceof Iterable) {
                final Spliterator<?> iterator = ((Iterable) object).spliterator();
                return StreamSupport.stream(iterator, false)
                        .map(child -> extractProperties(child, parts, index))
                        .collect(JOINER);
            }
            final Object child;
            final String part = parts[index];
            if (part.endsWith("()")) {
                child = on(object).call(part.substring(0, part.length() - 2)).get();
            } else {
                child = on(object).get(part);
            }
            return extractProperties(child, parts, index + 1);
        }
        if (object instanceof Object[]) {
            return Arrays.toString((Object[]) object);
        }
        return String.valueOf(object);
    }
}
