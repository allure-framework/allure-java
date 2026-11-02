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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class RestoredHttpResponse<T> implements HttpResponse<T> {

    private final HttpResponse<T> delegate;
    private final HttpRequest request;

    private RestoredHttpResponse(final HttpResponse<T> delegate) {
        this.delegate = delegate;
        this.request = HttpExchangeCapture.unwrap(delegate.request());
    }

    static <T> HttpResponse<T> restore(final HttpResponse<T> response) {
        return HttpExchangeCapture.isInstrumented(response.request()) ? new RestoredHttpResponse<>(response) : response;
    }

    static <T> CompletableFuture<HttpResponse<T>> restore(
                                                          final CompletableFuture<HttpResponse<T>> response) {
        final ForwardingFuture<HttpResponse<T>> restored = new ForwardingFuture<>(response);
        response.whenComplete((value, throwable) -> {
            if (throwable == null) {
                restored.complete(restore(value));
            } else {
                restored.completeExceptionally(throwable);
            }
        });
        return restored;
    }

    @Override
    public int statusCode() {
        return delegate.statusCode();
    }

    @Override
    public HttpRequest request() {
        return request;
    }

    @Override
    public Optional<HttpResponse<T>> previousResponse() {
        return delegate.previousResponse().map(RestoredHttpResponse::restore);
    }

    @Override
    public HttpHeaders headers() {
        return delegate.headers();
    }

    @Override
    public T body() {
        return delegate.body();
    }

    @Override
    public Optional<javax.net.ssl.SSLSession> sslSession() {
        return delegate.sslSession();
    }

    @Override
    public URI uri() {
        return delegate.uri();
    }

    @Override
    public HttpClient.Version version() {
        return delegate.version();
    }

    private static final class ForwardingFuture<T> extends CompletableFuture<T> {
        private final CompletableFuture<?> delegate;

        private ForwardingFuture(final CompletableFuture<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            final boolean cancelled = super.cancel(mayInterruptIfRunning);
            delegate.cancel(mayInterruptIfRunning);
            return cancelled;
        }

        @Override
        public void obtrudeValue(final T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void obtrudeException(final Throwable throwable) {
            throw new UnsupportedOperationException();
        }
    }
}
