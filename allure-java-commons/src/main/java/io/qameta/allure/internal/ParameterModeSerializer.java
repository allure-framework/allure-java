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
package io.qameta.allure.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.qameta.allure.model.Parameter;

import java.io.IOException;
import java.util.Locale;

/**
 * @author charlie (Dmitry Baev).
 */
public class ParameterModeSerializer extends StdSerializer<Parameter.Mode> {
    protected ParameterModeSerializer() {
        super(Parameter.Mode.class);
    }

    @Override
    public void serialize(final Parameter.Mode value,
                          final JsonGenerator gen,
                          final SerializerProvider provider) throws IOException {
        gen.writeString(value.name().toLowerCase(Locale.ENGLISH));
    }
}
