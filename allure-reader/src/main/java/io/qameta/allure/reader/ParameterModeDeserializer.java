/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.reader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import io.qameta.allure.model.Parameter;

/**
 * @author charlie (Dmitry Baev).
 * @deprecated in favor of {@link MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS}
 * and {@link DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL}
 */
@Deprecated
public class ParameterModeDeserializer extends AllureEnumDeserializer<Parameter.Mode> {
    public ParameterModeDeserializer() {
        super(Parameter.Mode.class);
    }
}
