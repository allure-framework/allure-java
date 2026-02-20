/*
 *  Copyright 2016-2026 Qameta Software Inc
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
package io.qameta.allure.junit5.features;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

/**
 * @author charlie (Dmitry Baev).
 */
public class ParameterisedPrimitivesTests {

    @ParameterizedTest
    @ValueSource(bytes = {0, 1})
    void bytes(byte value) {
    }

    @ParameterizedTest
    @ValueSource(shorts = {1, 2, 3})
    void shorts(int value) {
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void ints(int value) {
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L})
    void longs(long value) {
    }

    @ParameterizedTest
    @ValueSource(floats = {0.1f, 0.01f})
    void floats(float value) {
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.1d, 0.01d})
    void doubles(double value) {
    }

    @ParameterizedTest
    @ValueSource(chars = {'a', 'b', 'c'})
    void chars(char value) {
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void booleans(boolean value) {
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void booleansMethodSource(final boolean a, final boolean b) {
    }

    @ParameterizedTest
    @MethodSource("nulls")
    void nullMethodSource(final String stringValue,
                          final Long longValue) {
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(false, false)
        );
    }

    static Stream<Arguments> nulls() {
        return Stream.of(
                Arguments.of(null, null)
        );
    }


}
