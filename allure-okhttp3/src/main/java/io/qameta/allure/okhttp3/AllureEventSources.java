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

import io.qameta.allure.AllureThreadBinding;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.util.Objects;

/**
 * Allure-aware server-sent event source factories for OkHttp.
 */
public final class AllureEventSources {

    private AllureEventSources() {
        throw new IllegalStateException("Do not instance");
    }

    /**
     * Creates a reusable event source factory that captures the current Allure test or fixture when each event source
     * is opened. The captured context is restored for the HTTP interceptor and every listener callback.
     *
     * <p>The client, factory, request, and listener may be created before a test or fixture starts. Context is captured
     * only when {@link EventSource.Factory#newEventSource(Request, EventSourceListener)} is called.</p>
     *
     * @param client the OkHttp client
     * @return the context-aware event source factory
     */
    public static EventSource.Factory createFactory(final OkHttpClient client) {
        final EventSource.Factory delegate = EventSources.createFactory(Objects.requireNonNull(client, "client"));
        return (request, listener) -> {
            final AllureOkHttp3Context context = AllureOkHttp3Context.capture();
            final Request contextRequest = Objects.requireNonNull(request, "request")
                    .newBuilder()
                    .tag(AllureOkHttp3Context.class, context)
                    .build();
            final EventSourceListener contextListener = new ContextEventSourceListener(
                    Objects.requireNonNull(listener, "listener"),
                    context
            );
            return delegate.newEventSource(contextRequest, contextListener);
        };
    }

    private static final class ContextEventSourceListener extends EventSourceListener {

        private final EventSourceListener delegate;
        private final AllureOkHttp3Context context;

        private ContextEventSourceListener(final EventSourceListener delegate, final AllureOkHttp3Context context) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public void onOpen(final EventSource eventSource, final Response response) {
            runInContext(() -> delegate.onOpen(eventSource, response));
        }

        @Override
        public void onEvent(final EventSource eventSource, final String id,
                            final String type, final String data) {
            runInContext(() -> delegate.onEvent(eventSource, id, type, data));
        }

        @Override
        public void onClosed(final EventSource eventSource) {
            runInContext(() -> delegate.onClosed(eventSource));
        }

        @Override
        public void onFailure(final EventSource eventSource, final Throwable throwable,
                              final Response response) {
            runInContext(() -> delegate.onFailure(eventSource, throwable, response));
        }

        private void runInContext(final Runnable callback) {
            try (AllureThreadBinding ignored = context.bind()) {
                callback.run();
            }
        }
    }
}
