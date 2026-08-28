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
package io.qameta.allure.okhttp3;

import com.sun.net.httpserver.HttpServer;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.qameta.allure.test.RunUtils.runTests;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The interceptor must not consume streaming (text/event-stream) response bodies:
 * an SSE response only completes when the server closes the connection, so buffering
 * it starves the {@link EventSourceListener} of events.
 *
 * @see <a href="https://github.com/allure-framework/allure-java/issues/1036">#1036</a>
 */
@IsolatedLifecycle
class AllureOkHttp3SseTest {

    private static final List<String> EVENTS = List.of("first", "second");

    private static final String FINITE_EVENT = "context event";

    private HttpServer server;
    private final CountDownLatch connectionHold = new CountDownLatch(1);

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/sse", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                for (final String event : EVENTS) {
                    os.write(("data: " + event + "\n\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
                // hold the connection open: if the body completes, buffering passes too
                // and the test stops catching the regression
                try {
                    connectionHold.await(15, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        server.createContext("/finite-sse", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(("data: " + FINITE_EVENT + "\n\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        connectionHold.countDown();
        if (Objects.nonNull(server)) {
            server.stop(0);
        }
    }

    @Test
    void shouldDeliverSseEventsWhileStreamIsOpen() throws InterruptedException {
        final List<String> received = new CopyOnWriteArrayList<>();
        final CountDownLatch eventsArrived = new CountDownLatch(EVENTS.size());

        final OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AllureOkHttp3())
                .build();
        final Request request = new Request.Builder()
                .url("http://localhost:" + server.getAddress().getPort() + "/sse")
                .build();

        final EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onEvent(final EventSource eventSource, final String id,
                                final String type, final String data) {
                received.add(data);
                eventsArrived.countDown();
            }

            @Override
            public void onFailure(final EventSource eventSource, final Throwable t,
                                  final okhttp3.Response response) {
                // cancel() lands here on every run, so don't fail the test - just stop waiting
                while (eventsArrived.getCount() > 0) {
                    eventsArrived.countDown();
                }
            }
        };

        runWithinTestContext(() -> {
            final EventSource eventSource = EventSources.createFactory(client)
                    .newEventSource(request, listener);
            try {
                eventsArrived.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                eventSource.cancel();
            }
        });

        assertThat(received).containsExactlyElementsOf(EVENTS);
    }

    /**
     * Verifies that callbacks and HTTP exchanges from a reused OkHttp dispatcher thread report to the test that opens
     * each event source, even when the shared client, factory, request, and listeners were all created earlier.
     */
    @Description
    @Issue("1036")
    @Test
    void shouldCaptureContextWhenOpeningEventSourceWithSharedObjects() {
        final ExecutorService dispatcherExecutor = Executors.newSingleThreadExecutor();
        final OkHttpClient client = new OkHttpClient.Builder()
                .dispatcher(new Dispatcher(dispatcherExecutor))
                .addInterceptor(new AllureOkHttp3())
                .build();
        final EventSource.Factory eventSourceFactory = AllureEventSources.createFactory(client);
        final Request request = finiteSseRequest();
        final SseProbe firstProbe = new SseProbe("event received by first test");
        final SseProbe secondProbe = new SseProbe("event received by second test");

        try {
            final AllureResults results = runTests(lifecycle -> {
                final AllureExternalKey firstTestKey = scheduleTest(lifecycle, "first SSE test");
                final AllureExternalKey secondTestKey = scheduleTest(lifecycle, "second SSE test");

                lifecycle.startTest(firstTestKey);
                receiveEvent(eventSourceFactory, request, firstProbe);
                lifecycle.stopTest(firstTestKey);

                lifecycle.startTest(secondTestKey);
                receiveEvent(eventSourceFactory, request, secondProbe);
                lifecycle.stopTest(secondTestKey);

                lifecycle.writeTest(firstTestKey);
                lifecycle.writeTest(secondTestKey);
            });

            final TestResult firstTest = results.getTestResultByName("first SSE test");
            final TestResult secondTest = results.getTestResultByName("second SSE test");

            assertThat(firstTest.getSteps())
                    .extracting(StepResult::getName)
                    .filteredOn(AllureOkHttp3SseTest::isSseEvidenceStep)
                    .containsExactly("HTTP exchange", "event received by first test");
            assertThat(secondTest.getSteps())
                    .extracting(StepResult::getName)
                    .filteredOn(AllureOkHttp3SseTest::isSseEvidenceStep)
                    .containsExactly("HTTP exchange", "event received by second test");
        } finally {
            dispatcherExecutor.shutdownNow();
        }
    }

    /**
     * Verifies that an event source opened without a running Allure test or fixture cannot reuse context retained by
     * an OkHttp dispatcher thread from an earlier request.
     */
    @Description
    @Issue("1036")
    @Test
    void shouldSuppressStaleDispatcherContextForUnownedEventSource() {
        final ExecutorService dispatcherExecutor = Executors.newSingleThreadExecutor();
        final OkHttpClient client = new OkHttpClient.Builder()
                .dispatcher(new Dispatcher(dispatcherExecutor))
                .addInterceptor(new AllureOkHttp3())
                .build();
        final EventSource.Factory eventSourceFactory = AllureEventSources.createFactory(client);
        final Request request = finiteSseRequest();
        final SseProbe ownedProbe = new SseProbe("event received by owned test");
        final SseProbe unownedProbe = new SseProbe("event received without owner");

        try {
            final AllureResults results = runTests(lifecycle -> {
                final AllureExternalKey testKey = scheduleTest(lifecycle, "owning SSE test");

                lifecycle.startTest(testKey);
                receiveEvent(eventSourceFactory, request, ownedProbe);
                lifecycle.stopTest(testKey);

                receiveEvent(eventSourceFactory, request, unownedProbe);
                lifecycle.writeTest(testKey);
            });

            final TestResult owningTest = results.getTestResultByName("owning SSE test");

            assertThat(owningTest.getSteps())
                    .extracting(StepResult::getName)
                    .filteredOn(AllureOkHttp3SseTest::isSseEvidenceStep)
                    .containsExactly("HTTP exchange", "event received by owned test");
        } finally {
            dispatcherExecutor.shutdownNow();
        }
    }

    private static AllureExternalKey scheduleTest(final AllureLifecycle lifecycle, final String name) {
        final AllureExternalKey key = AllureExternalKey.random(AllureOkHttp3SseTest.class);
        lifecycle.scheduleTest(key, new TestResult().setUuid(UUID.randomUUID().toString()).setName(name));
        return key;
    }

    private Request finiteSseRequest() {
        return new Request.Builder()
                .url("http://localhost:" + server.getAddress().getPort() + "/finite-sse")
                .build();
    }

    private static void receiveEvent(final EventSource.Factory eventSourceFactory, final Request request,
                                     final SseProbe probe) {
        eventSourceFactory.newEventSource(request, probe);
        probe.await();
    }

    private static boolean isSseEvidenceStep(final String name) {
        return "HTTP exchange".equals(name) || name.startsWith("event received");
    }

    private static final class SseProbe extends EventSourceListener {

        private final String stepName;
        private final CountDownLatch eventReceived = new CountDownLatch(1);
        private final CountDownLatch eventSourceClosed = new CountDownLatch(1);
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        private SseProbe(final String stepName) {
            this.stepName = stepName;
        }

        @Override
        public void onEvent(final EventSource eventSource, final String id,
                            final String type, final String data) {
            Allure.step(stepName);
            eventReceived.countDown();
        }

        @Override
        public void onClosed(final EventSource eventSource) {
            eventSourceClosed.countDown();
        }

        @Override
        public void onFailure(final EventSource eventSource, final Throwable throwable,
                              final okhttp3.Response response) {
            failure.set(throwable);
            eventSourceClosed.countDown();
        }

        private void await() {
            try {
                assertThat(eventReceived.await(5, TimeUnit.SECONDS))
                        .as("SSE event received")
                        .isTrue();
                assertThat(eventSourceClosed.await(5, TimeUnit.SECONDS))
                        .as("SSE event source closed")
                        .isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for the SSE callback", e);
            }
            assertThat(failure.get())
                    .as("SSE callback failure")
                    .isNull();
        }
    }
}
