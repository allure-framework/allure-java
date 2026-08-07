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

import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeError;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SuppressWarnings("PMD.AvoidSynchronizedStatement")
final class HttpExchangeCapture {

    private static final String NO_BODY = "No body present";

    private final HttpRequest originalRequest;
    private final HttpClient.Version defaultVersion;
    private final Consumer<HttpExchange.Builder> exchangeCustomizer;
    private final ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private final long start = System.currentTimeMillis();

    private final AtomicReference<HttpResponse.ResponseInfo> responseInfo = new AtomicReference<>();
    private final HttpRequest capturedRequest;

    HttpExchangeCapture(final HttpRequest request,
                        final HttpClient.Version defaultVersion,
                        final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.originalRequest = Objects.requireNonNull(request, "request must not be null");
        this.defaultVersion = Objects.requireNonNull(defaultVersion, "defaultVersion must not be null");
        this.exchangeCustomizer = Objects.requireNonNull(exchangeCustomizer, "exchangeCustomizer must not be null");
        this.capturedRequest = instrument(request);
    }

    HttpRequest request() {
        return capturedRequest;
    }

    <T> HttpResponse.BodyHandler<T> bodyHandler(final HttpResponse.BodyHandler<T> delegate) {
        Objects.requireNonNull(delegate, "responseBodyHandler must not be null");
        return info -> {
            responseInfo.set(info);
            return new CapturingBodySubscriber<>(delegate.apply(info), this::captureResponseBody);
        };
    }

    HttpExchange exchange(final HttpResponse<?> response, final Throwable throwable) {
        final HttpExchange.Builder builder = HttpExchange.builder(capturedRequest())
                .setStart(start)
                .setStop(System.currentTimeMillis());
        exchangeCustomizer.accept(builder);

        final HttpExchangeResponse capturedResponse = capturedResponse(response);
        if (capturedResponse != null) {
            builder.setResponse(capturedResponse);
        }
        if (throwable != null) {
            builder.setError(error(throwable));
        }
        return builder.build();
    }

    static HttpRequest unwrap(final HttpRequest request) {
        return request instanceof CapturingHttpRequest captured ? captured.delegate : request;
    }

    static boolean isInstrumented(final HttpRequest request) {
        return request instanceof CapturingHttpRequest;
    }

    private HttpRequest instrument(final HttpRequest request) {
        return request.bodyPublisher()
                .<HttpRequest>map(
                        publisher -> new CapturingHttpRequest(
                                request,
                                new CapturingBodyPublisher(publisher, this::captureRequestBody)
                        )
                )
                .orElse(request);
    }

    private HttpExchangeRequest capturedRequest() {
        final HttpExchangeRequest.Builder builder = HttpExchangeRequest
                .builder(originalRequest.method(), originalRequest.uri().toString())
                .setHttpVersion(version(originalRequest.version().orElse(defaultVersion)));
        addHeaders(builder, originalRequest.headers());

        final byte[] bytes = snapshot(requestBody);
        if (bytes.length > 0) {
            builder.setBody(body(originalRequest.headers(), bytes));
        }
        return builder.build();
    }

    private HttpExchangeResponse capturedResponse(final HttpResponse<?> response) {
        final HttpResponse.ResponseInfo info = responseInfo.get();
        if (response == null && info == null) {
            return null;
        }

        final HttpExchangeResponse.Builder builder = HttpExchangeResponse.builder();
        if (response != null) {
            builder.setStatus(response.statusCode())
                    .setHttpVersion(version(response.version()));
            addHeaders(builder, response.headers());
        } else {
            builder.setStatus(info.statusCode())
                    .setHttpVersion(version(info.version()));
            addHeaders(builder, info.headers());
        }

        final HttpHeaders headers = response == null ? info.headers() : response.headers();
        final byte[] bytes = snapshot(responseBody);
        builder.setBody(bytes.length == 0 ? HttpExchangeBody.utf8(NO_BODY) : body(headers, bytes));
        return builder.build();
    }

    private void captureRequestBody(final ByteBuffer buffer) {
        capture(requestBody, buffer);
    }

    private void captureResponseBody(final ByteBuffer buffer) {
        capture(responseBody, buffer);
    }

    private static void capture(final ByteArrayOutputStream destination, final ByteBuffer source) {
        final ByteBuffer copy = source.asReadOnlyBuffer();
        final byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        synchronized (destination) {
            destination.writeBytes(bytes);
        }
    }

    private static byte[] snapshot(final ByteArrayOutputStream source) {
        synchronized (source) {
            return source.toByteArray();
        }
    }

    private static HttpExchangeBody body(final HttpHeaders headers, final byte[] bytes) {
        return new HttpExchangeBody(
                headers.firstValue("Content-Type").orElse(null),
                "utf8",
                new String(bytes, StandardCharsets.UTF_8),
                (long) bytes.length,
                null,
                null,
                null,
                null
        );
    }

    private static HttpExchangeError error(final Throwable throwable) {
        final Throwable unwrapped = unwrapFailure(throwable);
        return new HttpExchangeError(unwrapped.getClass().getName(), unwrapped.getMessage(), null);
    }

    private static Throwable unwrapFailure(final Throwable throwable) {
        Throwable result = throwable;
        while ((result instanceof CompletionException || result instanceof ExecutionException)
                && result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    private static String version(final HttpClient.Version version) {
        return switch (version) {
            case HTTP_1_1 -> "HTTP/1.1";
            case HTTP_2 -> "HTTP/2";
        };
    }

    private static void addHeaders(final HttpExchangeRequest.Builder builder, final HttpHeaders headers) {
        headers.map().forEach(
                (name, values) -> values.forEach(value -> builder.addHeader(name, value))
        );
    }

    private static void addHeaders(final HttpExchangeResponse.Builder builder, final HttpHeaders headers) {
        headers.map().forEach(
                (name, values) -> values.forEach(value -> builder.addHeader(name, value))
        );
    }

    private static final class CapturingHttpRequest extends HttpRequest {
        private final HttpRequest delegate;
        private final BodyPublisher bodyPublisher;

        private CapturingHttpRequest(final HttpRequest delegate, final BodyPublisher bodyPublisher) {
            this.delegate = delegate;
            this.bodyPublisher = bodyPublisher;
        }

        @Override
        public Optional<BodyPublisher> bodyPublisher() {
            return Optional.of(bodyPublisher);
        }

        @Override
        public String method() {
            return delegate.method();
        }

        @Override
        public Optional<Duration> timeout() {
            return delegate.timeout();
        }

        @Override
        public boolean expectContinue() {
            return delegate.expectContinue();
        }

        @Override
        public URI uri() {
            return delegate.uri();
        }

        @Override
        public Optional<HttpClient.Version> version() {
            return delegate.version();
        }

        @Override
        public HttpHeaders headers() {
            return delegate.headers();
        }
    }

    private static final class CapturingBodyPublisher implements HttpRequest.BodyPublisher {
        private final HttpRequest.BodyPublisher delegate;
        private final Consumer<ByteBuffer> capture;
        private final AtomicBoolean firstSubscription = new AtomicBoolean(true);

        private CapturingBodyPublisher(final HttpRequest.BodyPublisher delegate,
                                       final Consumer<ByteBuffer> capture) {
            this.delegate = delegate;
            this.capture = capture;
        }

        @Override
        public long contentLength() {
            return delegate.contentLength();
        }

        @Override
        public void subscribe(final Flow.Subscriber<? super ByteBuffer> subscriber) {
            if (firstSubscription.compareAndSet(true, false)) {
                delegate.subscribe(new CapturingPublisherSubscriber(subscriber, capture));
            } else {
                delegate.subscribe(subscriber);
            }
        }
    }

    private static final class CapturingPublisherSubscriber implements Flow.Subscriber<ByteBuffer> {
        private final Flow.Subscriber<? super ByteBuffer> delegate;
        private final Consumer<ByteBuffer> capture;

        private CapturingPublisherSubscriber(final Flow.Subscriber<? super ByteBuffer> delegate,
                                             final Consumer<ByteBuffer> capture) {
            this.delegate = delegate;
            this.capture = capture;
        }

        @Override
        public void onSubscribe(final Flow.Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(final ByteBuffer item) {
            capture.accept(item);
            delegate.onNext(item);
        }

        @Override
        public void onError(final Throwable throwable) {
            delegate.onError(throwable);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }

    private static final class CapturingBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
        private final HttpResponse.BodySubscriber<T> delegate;
        private final Consumer<ByteBuffer> capture;

        private CapturingBodySubscriber(final HttpResponse.BodySubscriber<T> delegate,
                                        final Consumer<ByteBuffer> capture) {
            this.delegate = Objects.requireNonNull(delegate, "bodySubscriber must not be null");
            this.capture = capture;
        }

        @Override
        public CompletionStage<T> getBody() {
            return delegate.getBody();
        }

        @Override
        public void onSubscribe(final Flow.Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(final List<ByteBuffer> items) {
            items.forEach(capture);
            delegate.onNext(items);
        }

        @Override
        public void onError(final Throwable throwable) {
            delegate.onError(throwable);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }
}

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
