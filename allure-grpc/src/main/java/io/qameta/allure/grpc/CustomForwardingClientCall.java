/*
 *  Copyright 2021 Qameta Software OÜ
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
package io.qameta.allure.grpc;

import com.google.protobuf.Message;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.util.ObjectUtils;
import org.awaitility.Awaitility;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation for a call to intercept requests, responses, metadata and grpc integration status.
 * Designed for use in a custom client interceptor.
 * <p>
 * Attaching to a report synchronously
 * <li>Step with method in the name</li>
 * <li>Request</li>
 * <p>
 * Attaching to a report asynchronously
 * </p>
 * <li>Response</li>
 * <li>Headers</li>
 * <li>Status</li>
 * </p>
 *
 * @param <Q> request message
 * @param <A> response message
 * @author a-simeshin (Simeshin Artem)
 * @see io.grpc.ClientInterceptor
 */
@SuppressWarnings("All")
public class CustomForwardingClientCall<Q, A> extends ForwardingClientCall.SimpleForwardingClientCall<Q, A> {

    private static final String TEXT_PLAIN = "text/plain";
    private static InheritableThreadLocal<AllureLifecycle> lifecycle = new InheritableThreadLocal<AllureLifecycle>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    private final AtomicBoolean interactionIsDone = new AtomicBoolean(false);
    private final AtomicReference<List<Message>> responseContainer = new AtomicReference<>(new ArrayList<Message>());
    private final AtomicReference<Metadata> headersContainer = new AtomicReference<>(null);
    private final AtomicReference<Status> statusContainer = new AtomicReference<>(null);
    private MethodDescriptor<Q, A> methodDescriptor;

    protected CustomForwardingClientCall(final ClientCall<Q, A> delegate) {
        super(delegate);
    }

    public CustomForwardingClientCall<Q, A> setMethodDescriptor(final MethodDescriptor<Q, A> methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
        return this;
    }

    public static AllureLifecycle getLifecycle() {
        return lifecycle.get();
    }

    @Override
    public void sendMessage(final Q message) {
        super.sendMessage(message);
        final Message grpcRequest = (Message) message;
        final String jsonRequest = GrpcFormattingUtil.toJson(grpcRequest);

        Allure.setLifecycle(getLifecycle());
        Allure.step("gRPC interaction " + methodDescriptor.getFullMethodName(), () -> {
            Allure.addAttachment("gRPC request", jsonRequest);
            Allure.addByteAttachmentAsync("gRPC responses", TEXT_PLAIN, () -> {
                Awaitility.await().until(() -> interactionIsDone.get());
                final String jsonResponses = GrpcFormattingUtil.toJson(responseContainer.get());
                return jsonResponses.getBytes(StandardCharsets.UTF_8);
            });
            Allure.addByteAttachmentAsync("gRPC headers", TEXT_PLAIN, () -> {
                Awaitility.await().until(() -> headersContainer.get() != null);
                return ObjectUtils.toString(headersContainer.get()).getBytes(StandardCharsets.UTF_8);
            });
            Allure.addByteAttachmentAsync("gRPC status", TEXT_PLAIN, () -> {
                Awaitility.await().until(() -> statusContainer.get() != null);
                return ObjectUtils.toString(statusContainer.get()).getBytes(StandardCharsets.UTF_8);
            });
        });
    }

    @Override
    public void start(final Listener<A> responseListener, final Metadata headers) {
        super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<A>(responseListener) {
            @Override
            public void onMessage(final A message) {
                responseContainer.get().add((Message) message);
                super.onMessage(message);
            }

            @Override
            public void onHeaders(final Metadata headers) {
                headersContainer.set(headers);
                super.onHeaders(headers);
            }

            @Override
            public void onClose(final Status status, final Metadata trailers) {
                statusContainer.set(status);
                interactionIsDone.set(true);
                super.onClose(status, trailers);
            }
        }, headers);
    }

    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set.
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        lifecycle.set(allure);
    }
}
