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

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An Allure-instrumented wrapper for the Java built-in {@link HttpClient}.
 *
 * <p>The wrapper records requests, responses, and transport errors as structured Allure HTTP exchange attachments.
 * Calls made without a running Allure test or fixture are passed directly to the wrapped client.</p>
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class AllureHttpClient extends HttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureHttpClient.class);

    private static final String ATTACHMENT_NAME = "HTTP exchange";

    private final HttpClient delegate;
    private final AllureLifecycle lifecycle;

    private final AtomicReference<Consumer<HttpExchange.Builder>> exchangeCustomizer = new AtomicReference<>(
            builder -> {
            }
    );

    /**
     * Wraps an HTTP client with Allure exchange capture.
     *
     * @param delegate the client to wrap
     * @return the instrumented client
     */
    public static AllureHttpClient wrap(final HttpClient delegate) {
        return new AllureHttpClient(delegate);
    }

    /**
     * Creates an Allure-instrumented HTTP client using the current global lifecycle.
     *
     * @param delegate the client to wrap
     */
    public AllureHttpClient(final HttpClient delegate) {
        this(delegate, Allure.getLifecycle());
    }

    /**
     * Creates an Allure-instrumented HTTP client using a supplied lifecycle.
     *
     * @param delegate the client to wrap
     * @param lifecycle the lifecycle that owns captured attachments
     */
    public AllureHttpClient(final HttpClient delegate, final AllureLifecycle lifecycle) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
    }

    /**
     * Sets the shared HTTP exchange builder customizer.
     *
     * @param exchangeCustomizer the exchange builder customizer
     * @return this instance for method chaining
     */
    public AllureHttpClient configureHttpExchange(final Consumer<HttpExchange.Builder> exchangeCustomizer) {
        this.exchangeCustomizer.set(Objects.requireNonNull(exchangeCustomizer, "exchangeCustomizer must not be null"));
        return this;
    }

    /**
     * Returns the wrapped client.
     *
     * @return the wrapped client
     */
    public HttpClient getDelegate() {
        return delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CookieHandler> cookieHandler() {
        return delegate.cookieHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Duration> connectTimeout() {
        return delegate.connectTimeout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Redirect followRedirects() {
        return delegate.followRedirects();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ProxySelector> proxy() {
        return delegate.proxy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLContext sslContext() {
        return delegate.sslContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLParameters sslParameters() {
        return delegate.sslParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Authenticator> authenticator() {
        return delegate.authenticator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version version() {
        return delegate.version();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Executor> executor() {
        return delegate.executor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebSocket.Builder newWebSocketBuilder() {
        return delegate.newWebSocketBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> HttpResponse<T> send(final HttpRequest request,
                                    final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        final Optional<AllureExternalKey> parent = lifecycle.getCurrentExecutableKey();
        if (parent.isEmpty()) {
            return delegate.send(request, responseBodyHandler);
        }

        final HttpExchangeCapture capture = newCapture(request);
        try {
            final HttpResponse<T> response = delegate.send(
                    capture.request(),
                    capture.bodyHandler(responseBodyHandler)
            );
            attach(parent.get(), capture.exchange(response, null));
            return RestoredHttpResponse.restore(response);
        } catch (IOException | InterruptedException | RuntimeException | Error throwable) {
            attach(parent.get(), capture.exchange(null, throwable));
            throw throwable;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                            final HttpRequest request,
                                                            final HttpResponse.BodyHandler<T> responseBodyHandler) {
        final Optional<AllureExternalKey> parent = lifecycle.getCurrentExecutableKey();
        if (parent.isEmpty()) {
            return delegate.sendAsync(request, responseBodyHandler);
        }

        return captureAsync(
                parent.get(),
                request,
                capture -> delegate.sendAsync(capture.request(), capture.bodyHandler(responseBodyHandler))
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                            final HttpRequest request,
                                                            final HttpResponse.BodyHandler<T> responseBodyHandler,
                                                            final HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        final Optional<AllureExternalKey> parent = lifecycle.getCurrentExecutableKey();
        if (parent.isEmpty()) {
            return delegate.sendAsync(request, responseBodyHandler, pushPromiseHandler);
        }

        return captureAsync(
                parent.get(),
                request,
                capture -> delegate.sendAsync(
                        capture.request(),
                        capture.bodyHandler(responseBodyHandler),
                        capturingPushPromiseHandler(parent.get(), pushPromiseHandler)
                )
        );
    }

    /**
     * Delegates the Java 21 {@code HttpClient.shutdown()} lifecycle method when it is available.
     */
    public void shutdown() {
        HttpClientLifecycle.invoke(delegate, "shutdown");
    }

    /**
     * Delegates the Java 21 {@code HttpClient.awaitTermination(Duration)} lifecycle method when it is available.
     *
     * @param duration the maximum duration to wait
     * @return whether the client terminated before the duration elapsed
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean awaitTermination(final Duration duration) throws InterruptedException {
        return HttpClientLifecycle.awaitTermination(delegate, duration);
    }

    /**
     * Delegates the Java 21 {@code HttpClient.isTerminated()} lifecycle method when it is available.
     *
     * @return whether the wrapped client has terminated
     */
    public boolean isTerminated() {
        return HttpClientLifecycle.invokeBoolean(delegate, "isTerminated");
    }

    /**
     * Delegates the Java 21 {@code HttpClient.shutdownNow()} lifecycle method when it is available.
     */
    public void shutdownNow() {
        HttpClientLifecycle.invoke(delegate, "shutdownNow");
    }

    private HttpExchangeCapture newCapture(final HttpRequest request) {
        return new HttpExchangeCapture(request, version(), exchangeCustomizer.get());
    }

    private <T> CompletableFuture<HttpResponse<T>> captureAsync(
                                                                final AllureExternalKey parent,
                                                                final HttpRequest request,
                                                                final Function<HttpExchangeCapture, CompletableFuture<HttpResponse<T>>> action) {
        final HttpExchangeCapture capture = newCapture(request);
        final CompletableFuture<HttpResponse<T>> response;
        try {
            response = action.apply(capture);
        } catch (RuntimeException | Error throwable) {
            attach(parent, capture.exchange(null, throwable));
            throw throwable;
        }

        attachAsync(parent, capture, response);
        return RestoredHttpResponse.restore(response);
    }

    private <T> HttpResponse.PushPromiseHandler<T> capturingPushPromiseHandler(
                                                                               final AllureExternalKey parent,
                                                                               final HttpResponse.PushPromiseHandler<T> handler) {
        if (handler == null) {
            return null;
        }
        return (initiatingRequest, pushPromiseRequest, acceptor) -> handler.applyPushPromise(
                HttpExchangeCapture.unwrap(initiatingRequest),
                pushPromiseRequest,
                bodyHandler -> captureAsync(
                        parent, pushPromiseRequest, capture -> acceptor.apply(
                                capture.bodyHandler(bodyHandler)
                        )
                )
        );
    }

    private void attach(final AllureExternalKey parent, final HttpExchange exchange) {
        try {
            lifecycle.addAttachmentStep(
                    parent,
                    ATTACHMENT_NAME,
                    HttpExchange.CONTENT_TYPE,
                    serialized(exchange),
                    AttachmentOptions.empty()
            );
        } catch (RuntimeException e) {
            LOGGER.warn("Could not save Java HTTP Client exchange", e);
        }
    }

    private void attachAsync(final AllureExternalKey parent,
                             final HttpExchangeCapture capture,
                             final CompletableFuture<? extends HttpResponse<?>> response) {
        final CompletionStage<InputStream> content = response.handle(
                (value, throwable) -> serialized(capture.exchange(value, throwable))
        );
        try {
            lifecycle.addAttachmentStepAsync(
                    parent,
                    ATTACHMENT_NAME,
                    HttpExchange.CONTENT_TYPE,
                    content,
                    AttachmentOptions.empty()
            ).exceptionally(throwable -> {
                LOGGER.warn("Could not save asynchronous Java HTTP Client exchange", throwable);
                return null;
            });
        } catch (RuntimeException e) {
            LOGGER.warn("Could not schedule asynchronous Java HTTP Client exchange", e);
        }
    }

    private static InputStream serialized(final HttpExchange exchange) {
        return new ByteArrayInputStream(HttpExchangeSerializer.toJsonBytes(exchange));
    }
}
