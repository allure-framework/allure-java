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
package io.qameta.allure.javahttpclient;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.time.Duration;

final class HttpClientLifecycle {

    private static final String JAVA_21_REQUIRED = "HTTP client lifecycle methods require Java 21 or newer";
    private static final String ACCESS_ERROR = "Could not access HTTP client lifecycle method ";

    private HttpClientLifecycle() {
        throw new IllegalStateException("do not instantiate");
    }

    static void invoke(final HttpClient delegate, final String name) {
        try {
            method(name).invoke(delegate);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(JAVA_21_REQUIRED, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(ACCESS_ERROR + name, e);
        } catch (InvocationTargetException e) {
            throwUnchecked(e.getCause());
        }
    }

    static boolean invokeBoolean(final HttpClient delegate, final String name) {
        try {
            return (Boolean) method(name).invoke(delegate);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(JAVA_21_REQUIRED, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(ACCESS_ERROR + name, e);
        } catch (InvocationTargetException e) {
            return throwUnchecked(e.getCause());
        }
    }

    @SuppressWarnings("PMD.PreserveStackTrace") // InvocationTargetException wraps the checked delegate exception.
    static boolean awaitTermination(final HttpClient delegate, final Duration duration) throws InterruptedException {
        try {
            return (Boolean) method("awaitTermination", Duration.class).invoke(delegate, duration);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(JAVA_21_REQUIRED, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access HTTP client lifecycle method awaitTermination", e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof InterruptedException interrupted) {
                throw interrupted;
            }
            return throwUnchecked(e.getCause());
        }
    }

    private static Method method(final String name, final Class<?>... parameterTypes) throws NoSuchMethodException {
        return HttpClient.class.getMethod(name, parameterTypes);
    }

    private static <T> T throwUnchecked(final Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException("HTTP client lifecycle method failed", throwable);
    }
}
