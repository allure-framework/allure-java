/*
 *  Copyright 2016-2024 Qameta Software Inc
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

import io.qameta.allure.testdata.DummyUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static io.qameta.allure.util.NamingUtils.processNameTemplate;
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
}
