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

import io.grpc.ClientInterceptor;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.CallOptions;
import io.grpc.Channel;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;

/**
 * <p>
 * Implementation for forwarding a call to a client inner proxy-call (CustomForwardingClientCall) with logic to attach
 * data to Allure.
 * </p>
 * <p>
 * How to use with pure grpc-api
 * </p>
 * <p>
 * The grpc api implementation currently requires creating a stub to create client.
 * In my case, it will be the GreetGrpc service, which is described in the proto file.
 *
 * <pre>{@code
 *      val stub = GreetGrpc.newBlockingStub(channel)
 *          .withInterceptors(new AllureGrpcClientInterceptor());
 *
 *      val response = stub.greetMe();
 * }
 * </pre>
 * </p>
 * <p>
 * How to use with https://github.com/yidongnan/grpc-spring-boot-starter with client autoconfiguration
 * </p>
 * <p>
 * According to the documentation (https://yidongnan.github.io/grpc-spring-boot-starter/en/client/configuration.html)
 * in the configuration section, there are different methods for connecting a client
 * interceptor. Still, I advise to add it to your spring-boot config class as:
 *
 * <pre>{@code
 *
 *      @Bean
 *      GlobalClientInterceptorConfigurer globalClientInterceptorConfigurer() {
 *          interceptors -> interceptors.add(new AllureGrpcClientInterceptor());
 *      }
 * }
 * </pre>
 * </p>
 *
 * @author a-simeshin (Simeshin Artem)
 * @see CustomForwardingClientCall
 */
@SuppressWarnings("All")
public class AllureGrpcClientInterceptor implements ClientInterceptor {

    private static InheritableThreadLocal<AllureLifecycle> lifecycle = new InheritableThreadLocal<AllureLifecycle>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    public static AllureLifecycle getLifecycle() {
        return lifecycle.get();
    }

    @Override
    public <Q, A> ClientCall<Q, A> interceptCall(final MethodDescriptor<Q, A> method,
                                                 final CallOptions callOptions, final Channel next) {
        return new CustomForwardingClientCall<Q, A>(next.newCall(method, callOptions))
                .setAllureLifeCycle(getLifecycle())
                .setMethodDescriptor(method);
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
