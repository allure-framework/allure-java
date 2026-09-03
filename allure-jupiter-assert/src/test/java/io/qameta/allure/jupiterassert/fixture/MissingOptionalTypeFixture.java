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
package io.qameta.allure.jupiterassert.fixture;

import java.util.function.Function;

/**
 * Loads a bounded generic subclass whose type argument is deliberately absent at runtime.
 */
public final class MissingOptionalTypeFixture {

    public static final String MISSING_TYPE_NAME = "io.qameta.allure.jupiterassert.fixture.MissingOptionalType";

    private MissingOptionalTypeFixture() {
    }

    public static Class<?> loadClassWithMissingTypeSignature() {
        return MissingTypeProperties.class;
    }

    private interface TypeProperties<T extends Runnable> {

        Class<? extends T> getType();

        String get(T instance);

    }

    private static class MappedProperties<T extends Runnable> implements TypeProperties<T> {

        @Override
        public Class<? extends T> getType() {
            return null;
        }

        protected void add(final Function<T, String> getter) {
            //fixture method, should be empty
        }

        @Override
        public String get(final T instance) {
            return null;
        }

    }

    private static final class MissingTypeProperties extends MappedProperties<MissingOptionalType> {

        @Override
        public Class<? extends MissingOptionalType> getType() {
            return null;
        }

        private MissingTypeProperties() {
            add(MissingOptionalType::getValue);
        }

    }

}
