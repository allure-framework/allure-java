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
package io.qameta.allure;

import java.util.Objects;

/**
 * @author charlie (Dmitry Baev).
 * @deprecated scheduled to remove in 3.0
 */
@Deprecated
public class ReadError {

    private final String message;

    private final Throwable exception;

    public ReadError(final Throwable exception, final String message, final Object... args) {
        this.message = Objects.nonNull(args) ? String.format(message, args) : message;
        this.exception = exception;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getException() {
        return exception;
    }
}
