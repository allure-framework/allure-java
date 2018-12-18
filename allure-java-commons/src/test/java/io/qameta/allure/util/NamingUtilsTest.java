package io.qameta.allure.util;

import io.qameta.allure.testdata.DummyUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static io.qameta.allure.util.NamingUtils.processNameTemplate;
import static io.qameta.allure.util.NamingUtils.convertCamelCaseToSentence;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class NamingUtilsTest {


    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("", Collections.singletonMap("a", "b"), ""),
                Arguments.of("", Collections.singletonMap("a", "b"), ""),
                Arguments.of("Hello word", Collections.emptyMap(), "Hello word"),

                Arguments.of("Hello {0}", Collections.singletonMap("0", "world"), "Hello world"),
                Arguments.of("Hello {method}", Collections.singletonMap("method", "world"), "Hello world"),

                Arguments.of("{missing}", Collections.emptyMap(), "{missing}"),
                Arguments.of("Hello {user}!", Collections.singletonMap("user", "Ivan"), "Hello Ivan!"),
                Arguments.of("Hello {user}", Collections.singletonMap("user", null), "Hello null"),
                Arguments.of("Hello {users}", Collections.singletonMap("users", Arrays.asList("Ivan", "Petr")), "Hello [Ivan, Petr]"),
                Arguments.of("Hello {users}", Collections.singletonMap("users", new String[]{"Ivan", "Petr"}), "Hello [Ivan, Petr]"),
                Arguments.of("Hello {users}", Collections.singletonMap("users", Collections.singletonMap("a", "b")), "Hello {a=b}"),
                Arguments.of("Password: {user.password}", Collections.singletonMap("user", new DummyUser(null, "123", null)), "Password: 123"),
                Arguments.of("Passwords: {users.password}", Collections.singletonMap("users", new DummyUser[]{new DummyUser(null, "123", null)}), "Passwords: [123]"),
                Arguments.of("Passwords: {users.password}", Collections.singletonMap("users", new DummyUser[]{null, new DummyUser(null, "123", null)}), "Passwords: [null, 123]"),
                Arguments.of("Passwords: {users.password}", Collections.singletonMap("users", new DummyUser[][]{null, {null, new DummyUser(null, "123", null)}}), "Passwords: [null, [null, 123]]")
        );
    }

    private static Stream<Arguments> camelCaseTestData() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of(" ", " "),
                Arguments.of(null, null),
                Arguments.of("someCamelCaseString", "Some camel case string"),
                Arguments.of("string01WithNumbers02", "String 01 with numbers 02"),
                Arguments.of("single", "Single"),
                Arguments.of("Capitalized", "Capitalized"),
                Arguments.of("testIBMAbbreviation", "Test IBM abbreviation"),
                Arguments.of("SObject", "S object"),
                Arguments.of("ObjectS", "Object S"),
                Arguments.of("TimeIs10PM", "Time is 10 PM"));

    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldProcessTemplate(final String template,
                                      final Map<String, Object> parameters,
                                      final String expected) {
        final String actual = processNameTemplate(template, parameters);

        assertThat(actual)
                .describedAs("Should process template \"%s\" as \"%s\"", template, expected)
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("camelCaseTestData")
    public void shouldConvertCamelCase(final String forConversion, final String expected) {
        final String actual = convertCamelCaseToSentence(forConversion);

        assertThat(actual)
                .describedAs("Should convert \"%s\" to \"%s\"", forConversion, expected)
                .isEqualTo(expected);
    }
}