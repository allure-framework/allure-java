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
package io.qameta.allure.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectionUtilsTest {

    @Test
    void shouldReadValueFromGetter() {
        final Object value = ReflectionUtils.getValue(new GetterSample(), "value");

        assertThat(value).isEqualTo("getter value");
    }

    @Test
    void shouldPreferGetterOverField() {
        final Object value = ReflectionUtils.getValue(new GetterAndFieldSample(), "value");

        assertThat(value).isEqualTo("getter value");
    }

    @Test
    void shouldReadValueFromRecordStyleMethod() {
        final Object value = ReflectionUtils.getValue(new RecordStyleSample(), "value");

        assertThat(value).isEqualTo("record value");
    }

    @Test
    void shouldReadValueFromField() {
        final Object value = ReflectionUtils.getValue(new FieldSample(), "value");

        assertThat(value).isEqualTo("field value");
    }

    @Test
    void shouldReadValueFromInheritedField() {
        final Object value = ReflectionUtils.getValue(new ChildFieldSample(), "value");

        assertThat(value).isEqualTo("parent field value");
    }

    @Test
    void shouldReturnNullForMissingValue() {
        final Object value = ReflectionUtils.getValue(new GetterSample(), "missing");

        assertThat(value).isNull();
    }

    @Test
    void shouldReadBooleanValueFromPrimitiveIsGetter() {
        final Boolean value = ReflectionUtils.getBooleanValue(new PrimitiveBooleanSample(), "value");

        assertThat(value).isFalse();
    }

    @Test
    void shouldPreferPrimitiveIsGetterForBooleanValue() {
        final Boolean value = ReflectionUtils.getBooleanValue(new PrimitiveIsAndGetSample(), "value");

        assertThat(value).isFalse();
    }

    @Test
    void shouldReadBooleanValueFromGetter() {
        final Boolean value = ReflectionUtils.getBooleanValue(new BooleanGetterSample(), "value");

        assertThat(value).isTrue();
    }

    @Test
    void shouldReadBooleanValueFromField() {
        final Boolean value = ReflectionUtils.getBooleanValue(new BooleanFieldSample(), "value");

        assertThat(value).isFalse();
    }

    @Test
    void shouldIgnoreBoxedBooleanIsGetter() {
        final Boolean value = ReflectionUtils.getBooleanValue(new BoxedBooleanIsSample(), "value");

        assertThat(value).isNull();
    }

    @Test
    void shouldReturnNullForNonBooleanValue() {
        final Boolean value = ReflectionUtils.getBooleanValue(new GetterSample(), "value");

        assertThat(value).isNull();
    }

    static class GetterSample {

        public String getValue() {
            return "getter value";
        }
    }

    static class GetterAndFieldSample {

        private final String value = "field value";

        public String getValue() {
            return "getter value";
        }
    }

    static class RecordStyleSample {

        public String value() {
            return "record value";
        }
    }

    static class FieldSample {

        private final String value = "field value";
    }

    static class ParentFieldSample {

        private final String value = "parent field value";
    }

    static class ChildFieldSample extends ParentFieldSample {
    }

    static class PrimitiveBooleanSample {

        public boolean isValue() {
            return false;
        }
    }

    static class PrimitiveIsAndGetSample {

        public boolean isValue() {
            return false;
        }

        public Boolean getValue() {
            return true;
        }
    }

    static class BooleanGetterSample {

        public Boolean getValue() {
            return true;
        }
    }

    static class BooleanFieldSample {

        private final Boolean value = false;
    }

    static class BoxedBooleanIsSample {

        public Boolean isValue() {
            return true;
        }
    }
}
