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

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.qameta.allure.javahttpclient.HttpExchangeTestSupport.attachmentContent;
import static io.qameta.allure.javahttpclient.HttpExchangeTestSupport.executeWithAllure;
import static io.qameta.allure.javahttpclient.HttpExchangeTestSupport.httpExchangeAttachment;
import static io.qameta.allure.javahttpclient.HttpExchangeTestSupport.httpExchangeAttachments;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IsolatedLifecycle
class AllureHttpClientTest {

    private static final String RESPONSE_BODY = "response-body";

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        server.stubFor(
                post(urlEqualTo("/items"))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader("Content-Type", "text/plain")
                                        .withHeader("X-Response-Id", "response-42")
                                        .withBody(RESPONSE_BODY)
                        )
        );
        server.stubFor(
                get(urlEqualTo("/async"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "text/plain")
                                        .withBody("async-body")
                        )
        );
    }

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop();
        }
    }

    /**
     * Verifies that a synchronous POST produces one structured exchange while preserving the response body and
     * original request identity for the caller.
     */
    @Test
    @Issue("957")
    @Description
    void shouldCaptureSynchronousRequestAndResponse() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpRequest request = HttpRequest.newBuilder(uri("/items"))
                    .header("Content-Type", "text/plain")
                    .header("X-Request-Id", "request-42")
                    .POST(HttpRequest.BodyPublishers.ofString("request-body"))
                    .build();
            final HttpClient client = AllureHttpClient.wrap(HttpClient.newHttpClient());

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.body()).isEqualTo(RESPONSE_BODY);
            assertThat(response.request()).isSameAs(request);
        });

        final Attachment attachment = httpExchangeAttachment(results);
        final String exchange = attachmentContent(results, attachment);

        assertThat(attachment.getName()).isEqualTo("HTTP exchange");
        assertThat(exchange)
                .contains("\"method\":\"POST\"")
                .contains("\"url\":\"" + uri("/items") + "\"")
                .contains("\"httpVersion\":\"HTTP/1.1\"")
                .contains("\"name\":\"X-Request-Id\",\"value\":\"request-42\"")
                .contains("\"value\":\"request-body\"")
                .contains("\"status\":201")
                .contains("\"name\":\"x-response-id\",\"value\":\"response-42\"")
                .contains("\"value\":\"response-body\"");
    }

    /**
     * Verifies that an asynchronous response completed on the HTTP client executor remains attached to the Allure
     * test that initiated it.
     */
    @Test
    @Issue("957")
    @Description
    void shouldCaptureAsynchronousExchangeOnTheInitiatingTest() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpRequest request = HttpRequest.newBuilder(uri("/async")).GET().build();
            final HttpClient client = AllureHttpClient.wrap(HttpClient.newHttpClient());

            final HttpResponse<String> response = client
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .join();

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("async-body");
        });

        assertThat(attachmentContent(results, httpExchangeAttachment(results)))
                .contains("\"url\":\"" + uri("/async") + "\"")
                .contains("\"status\":200")
                .contains("\"value\":\"async-body\"");
    }

    /**
     * Verifies that accepted HTTP/2 push promises produce their own exchange attachment without hiding the primary
     * response.
     */
    @Test
    @Issue("957")
    @Description
    void shouldCaptureAcceptedPushPromises() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpClient client = AllureHttpClient.wrap(new PushPromiseHttpClient());
            final HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.test/main")).GET().build();
            final List<CompletableFuture<HttpResponse<String>>> pushes = new ArrayList<>();

            final HttpResponse<String> response = client.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString(),
                    (initiating, push, acceptor) -> pushes.add(acceptor.apply(HttpResponse.BodyHandlers.ofString()))
            ).join();

            assertThat(response.body()).isEqualTo("main-body");
            assertThat(pushes).singleElement().satisfies(
                    push -> assertThat(push.join().body()).isEqualTo("push-body")
            );
        });

        final List<String> exchanges = httpExchangeAttachments(results).stream()
                .map(attachment -> attachmentContent(results, attachment))
                .toList();
        assertThat(exchanges)
                .hasSize(2)
                .anySatisfy(
                        exchange -> assertThat(exchange)
                                .contains("https://example.test/main")
                                .contains("main-body")
                )
                .anySatisfy(
                        exchange -> assertThat(exchange)
                                .contains("https://example.test/pushed")
                                .contains("push-body")
                );
    }

    /**
     * Verifies that synchronous transport failures remain visible to the caller and are recorded as failed
     * exchanges.
     */
    @Test
    @Issue("957")
    @Description
    void shouldCaptureSynchronousTransportErrors() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpClient client = AllureHttpClient.wrap(new FailingHttpClient());
            final HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.test/failure")).GET().build();

            assertThatThrownBy(() -> client.send(request, HttpResponse.BodyHandlers.ofString()))
                    .isInstanceOf(IOException.class)
                    .hasMessage("simulated transport failure");
        });

        assertThat(attachmentContent(results, httpExchangeAttachment(results)))
                .contains("\"url\":\"https://example.test/failure\"")
                .contains("\"name\":\"java.io.IOException\"")
                .contains("\"message\":\"simulated transport failure\"");
    }

    /**
     * Verifies that asynchronous transport failures remain visible through the returned future and are recorded as
     * failed exchanges owned by the initiating test.
     */
    @Test
    @Issue("957")
    @Description
    void shouldCaptureAsynchronousTransportErrors() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpClient client = AllureHttpClient.wrap(new FailingHttpClient());
            final HttpRequest request = HttpRequest.newBuilder(
                    URI.create("https://example.test/async-failure")
            ).GET().build();

            assertThatThrownBy(() -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join())
                    .isInstanceOf(CompletionException.class)
                    .hasCauseInstanceOf(IOException.class)
                    .hasRootCauseMessage("simulated transport failure");
        });

        assertThat(attachmentContent(results, httpExchangeAttachment(results)))
                .contains("\"url\":\"https://example.test/async-failure\"")
                .contains("\"name\":\"java.io.IOException\"")
                .contains("\"message\":\"simulated transport failure\"");
    }

    /**
     * Verifies that cancelling the future returned by the wrapper cancels the underlying HTTP exchange and records
     * the cancellation.
     */
    @Test
    @Issue("957")
    @Description
    void shouldPropagateAsynchronousCancellation() {
        final PendingHttpClient delegate = new PendingHttpClient();
        final AllureResults results = executeWithAllure(() -> {
            final HttpClient client = AllureHttpClient.wrap(delegate);
            final HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.test/pending")).GET().build();

            final CompletableFuture<HttpResponse<String>> response = client.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            assertThat(response.cancel(true)).isTrue();
            assertThat(delegate.response).isCancelled();
        });

        assertThat(attachmentContent(results, httpExchangeAttachment(results)))
                .contains("\"url\":\"https://example.test/pending\"")
                .contains("\"name\":\"java.util.concurrent.CancellationException\"");
    }

    /**
     * Verifies that shared HTTP exchange options redact configured headers and limit captured bodies.
     */
    @Test
    @Issue("957")
    @Description
    void shouldApplyHttpExchangeCustomization() {
        final AllureResults results = executeWithAllure(() -> {
            final HttpRequest request = HttpRequest.newBuilder(uri("/items"))
                    .header("Content-Type", "text/plain")
                    .header("X-Api-Key", "very-secret")
                    .POST(HttpRequest.BodyPublishers.ofString("request-body"))
                    .build();
            final HttpClient client = AllureHttpClient.wrap(HttpClient.newHttpClient())
                    .configureHttpExchange(
                            exchange -> exchange
                                    .redactHeader("X-Api-Key")
                                    .setMaxBodySize(5)
                    );

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.body()).isEqualTo(RESPONSE_BODY);
        });

        assertThat(attachmentContent(results, httpExchangeAttachment(results)))
                .contains("__ALLURE_REDACTED__")
                .doesNotContain("very-secret")
                .contains("\"value\":\"reque\"")
                .contains("\"value\":\"respo\"")
                .contains("\"truncated\":true");
    }

    /**
     * Verifies that the wrapper preserves exact delegate inputs and client configuration when no Allure executable
     * is running.
     */
    @Test
    @Issue("957")
    @Description
    void shouldPassThroughWithoutAnAllureContext() throws Exception {
        final RecordingHttpClient delegate = new RecordingHttpClient();
        final AllureHttpClient client = new AllureHttpClient(delegate, new AllureLifecycle());
        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.test/direct")).GET().build();
        final HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();

        assertThat(client.send(request, handler)).isNull();
        assertThat(delegate.request).isSameAs(request);
        assertThat(delegate.bodyHandler).isSameAs(handler);
        assertThat(client.getDelegate()).isSameAs(delegate);
        assertThat(client.version()).isEqualTo(delegate.version());
        assertThat(client.followRedirects()).isEqualTo(delegate.followRedirects());
        assertThat(client.newWebSocketBuilder()).isNotNull();
    }

    /**
     * Verifies lifecycle delegation on runtimes that expose the Java 21 HTTP client lifecycle API and the documented
     * unsupported result on older runtimes.
     */
    @Test
    @Issue("957")
    @Description
    void shouldDelegateRuntimeLifecycleMethods() throws Exception {
        final LifecycleHttpClient delegate = new LifecycleHttpClient();
        final AllureHttpClient client = AllureHttpClient.wrap(delegate);
        final Duration timeout = Duration.ofSeconds(1);

        if (Runtime.version().feature() < 21) {
            assertThatThrownBy(client::shutdown).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(client::isTerminated).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(client::shutdownNow).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> client.awaitTermination(timeout))
                    .isInstanceOf(UnsupportedOperationException.class);
            return;
        }

        client.shutdown();
        assertThat(delegate.shutdown).isTrue();
        assertThat(client.awaitTermination(timeout)).isTrue();
        assertThat(delegate.awaitTerminationTimeout).isEqualTo(timeout);
        assertThat(client.isTerminated()).isTrue();
        client.shutdownNow();
        assertThat(delegate.shutdownNow).isTrue();
    }

    private URI uri(final String path) {
        return URI.create(String.format("http://localhost:%d%s", server.port(), path));
    }

    private abstract static class StubHttpClient extends HttpClient {
        private final HttpClient configuration = HttpClient.newHttpClient();

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return configuration.cookieHandler();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return configuration.connectTimeout();
        }

        @Override
        public Redirect followRedirects() {
            return configuration.followRedirects();
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return configuration.proxy();
        }

        @Override
        public SSLContext sslContext() {
            return configuration.sslContext();
        }

        @Override
        public SSLParameters sslParameters() {
            return configuration.sslParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return configuration.authenticator();
        }

        @Override
        public Version version() {
            return configuration.version();
        }

        @Override
        public Optional<Executor> executor() {
            return configuration.executor();
        }

        @Override
        public WebSocket.Builder newWebSocketBuilder() {
            return configuration.newWebSocketBuilder();
        }
    }

    private static final class FailingHttpClient extends StubHttpClient {
        @Override
        public <T> HttpResponse<T> send(final HttpRequest request,
                                        final HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException {
            throw new IOException("simulated transport failure");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                                final HttpRequest request,
                                                                final HttpResponse.BodyHandler<T> responseBodyHandler) {
            return CompletableFuture.failedFuture(new IOException("simulated transport failure"));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                                final HttpRequest request,
                                                                final HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                final HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, responseBodyHandler);
        }
    }

    private static final class PendingHttpClient extends StubHttpClient {
        private final CompletableFuture<HttpResponse<String>> response = new CompletableFuture<>();

        @Override
        public <T> HttpResponse<T> send(final HttpRequest request,
                                        final HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                                final HttpRequest request,
                                                                final HttpResponse.BodyHandler<T> responseBodyHandler) {
            return (CompletableFuture<HttpResponse<T>>) (CompletableFuture<?>) response;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                                final HttpRequest request,
                                                                final HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                final HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, responseBodyHandler);
        }
    }

    private static class RecordingHttpClient extends StubHttpClient {
        private HttpRequest request;
        private HttpResponse.BodyHandler<?> bodyHandler;

        @Override
        public <T> HttpResponse<T> send(final HttpRequest request,
                                        final HttpResponse.BodyHandler<T> responseBodyHandler) {
            this.request = request;
            this.bodyHandler = responseBodyHandler;
            return null;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                                final HttpRequest request,
                                                                final HttpResponse.BodyHandler<T> responseBodyHandler) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                                final HttpRequest request,
                                                                final HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                final HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, responseBodyHandler);
        }
    }

    private static final class LifecycleHttpClient extends RecordingHttpClient {
        private boolean shutdown;
        private boolean shutdownNow;
        private Duration awaitTerminationTimeout;

        public void shutdown() {
            shutdown = true;
        }

        public boolean awaitTermination(final Duration duration) {
            awaitTerminationTimeout = duration;
            return true;
        }

        public boolean isTerminated() {
            return shutdown;
        }

        public void shutdownNow() {
            shutdownNow = true;
        }
    }

    private static final class PushPromiseHttpClient extends StubHttpClient {
        @Override
        public <T> HttpResponse<T> send(final HttpRequest request,
                                        final HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                                final HttpRequest request,
                                                                final HttpResponse.BodyHandler<T> responseBodyHandler) {
            return CompletableFuture.completedFuture(response(request, responseBodyHandler, "main-body"));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                                                                final HttpRequest request,
                                                                final HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                final HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            final HttpRequest pushRequest = HttpRequest.newBuilder(
                    URI.create("https://example.test/pushed")
            ).GET().build();
            if (pushPromiseHandler != null) {
                pushPromiseHandler.applyPushPromise(
                        request,
                        pushRequest,
                        handler -> CompletableFuture.completedFuture(response(pushRequest, handler, "push-body"))
                );
            }
            return sendAsync(request, responseBodyHandler);
        }

        private static <T> HttpResponse<T> response(final HttpRequest request,
                                                    final HttpResponse.BodyHandler<T> handler,
                                                    final String body) {
            final HttpHeaders headers = HttpHeaders.of(
                    Map.of("Content-Type", List.of("text/plain")),
                    (name, value) -> true
            );
            final HttpResponse.ResponseInfo info = new HttpResponse.ResponseInfo() {
                @Override
                public int statusCode() {
                    return 200;
                }

                @Override
                public HttpHeaders headers() {
                    return headers;
                }

                @Override
                public Version version() {
                    return Version.HTTP_2;
                }
            };
            final HttpResponse.BodySubscriber<T> subscriber = handler.apply(info);
            final CompletableFuture<T> result = subscriber.getBody().toCompletableFuture();
            subscriber.onSubscribe(new NoopSubscription());
            subscriber.onNext(List.of(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))));
            subscriber.onComplete();
            return new StubResponse<>(request, headers, result.join());
        }
    }

    private static final class NoopSubscription implements Flow.Subscription {
        @Override
        public void request(final long count) {
            // the fake response body is delivered synchronously
        }

        @Override
        public void cancel() {
            // the fake response body is already complete
        }
    }

    private static final class StubResponse<T> implements HttpResponse<T> {
        private final HttpRequest request;
        private final HttpHeaders headers;
        private final T body;

        private StubResponse(final HttpRequest request, final HttpHeaders headers, final T body) {
            this.request = request;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }
    }
}
