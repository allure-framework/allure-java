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
import io.qameta.allure.test.IsolatedLifecycle;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
}
