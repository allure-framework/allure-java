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
package io.qameta.allure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllureExternalKeyTest {

    @Test
    void shouldCompareByNamespaceAndValues() {
        final AllureExternalKey first = AllureExternalKey.of(AllureExternalKeyTest.class, "suite", "test");
        final AllureExternalKey second = AllureExternalKey.of(AllureExternalKeyTest.class, "suite", "test");
        final AllureExternalKey otherNamespace = AllureExternalKey.of(AllureExternalKey.class, "suite", "test");
        final AllureExternalKey otherValues = AllureExternalKey.of(AllureExternalKeyTest.class, "suite", "other");

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second)
                .isNotEqualTo(otherNamespace)
                .isNotEqualTo(otherValues);
    }

    @Test
    void shouldRecomputeEqualForValueTypedFrameworkData() {
        // same framework value object reconstructed in two hooks must compare equal
        final Object frameworkId = 42L;
        assertThat(AllureExternalKey.of(AllureExternalKeyTest.class, frameworkId))
                .isEqualTo(AllureExternalKey.of(AllureExternalKeyTest.class, 42L));
    }

    @Test
    void shouldKeepValueBoundariesDistinct() {
        // the digest frames each value, so concatenation across value boundaries must not collide
        assertThat(AllureExternalKey.of(AllureExternalKeyTest.class, "ab", "c"))
                .isNotEqualTo(AllureExternalKey.of(AllureExternalKeyTest.class, "a", "bc"));
    }

    @Test
    void shouldCreateDistinctRandomKeys() {
        assertThat(AllureExternalKey.random(AllureExternalKeyTest.class))
                .isNotEqualTo(AllureExternalKey.random(AllureExternalKeyTest.class));
    }

    @Test
    void shouldRejectInvalidValues() {
        assertThatThrownBy(() -> AllureExternalKey.of(AllureExternalKeyTest.class, (Object) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AllureExternalKey.of(AllureExternalKeyTest.class, "ok", (Object) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AllureExternalKey.of(AllureExternalKeyTest.class, new String[]{"a"}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AllureExternalKey.of(AllureExternalKeyTest.class, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullNamespace() {
        assertThatThrownBy(() -> AllureExternalKey.of((Class<?>) null, "value"))
                .isInstanceOf(NullPointerException.class);
    }
}
