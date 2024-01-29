/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.karate;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.netty.MockServer;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SuppressWarnings("MultipleStringLiterals")
public class TestRunner {

    private MockServer server;
    private MockServerClient client;

    AllureResults runApi(final String... featurePath) {
        server = new MockServer(8081);
        client = new MockServerClient("localhost", server.getLocalPort());

        final HttpRequest interceptReq1 = request("/login").withMethod("GET");
        final HttpResponse mockResponse1 = response().withStatusCode(200);
        client.when(interceptReq1).respond(mockResponse1);

        final HttpRequest interceptReq2 = request("/login").withMethod("POST");
        final HttpResponse mockResponse2 = response().withStatusCode(401).withBody("[{\"message\": \"No access\"}]");
        client.when(interceptReq2).respond(mockResponse2);

        final HttpRequest interceptReq3 = request("/user").withMethod("GET");
        final HttpResponse mockResponse3 = response().withStatusCode(301);
        client.when(interceptReq3).respond(mockResponse3);

        final HttpRequest interceptReq4 = request("/pages").withMethod("GET");
        final HttpResponse mockResponse4 = response().withStatusCode(404);
        client.when(interceptReq4).respond(mockResponse4);

        return run(featurePath);
    }

    AllureResults run(final String... path) {
        final AllureResultsWriterStub writerStub = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writerStub);
        final AllureKarate allureKarate = new AllureKarate(lifecycle);

        final AllureLifecycle defaultLifecycle = Allure.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            StepsAspects.setLifecycle(lifecycle);
            AttachmentsAspects.setLifecycle(lifecycle);

            com.intuit.karate.Runner.builder()
                    .path(path)
                    .hook(allureKarate)
                    .backupReportDir(false)
                    .outputJunitXml(false)
                    .outputCucumberJson(false)
                    .outputHtmlReport(false)
                    .parallel(1);

            return writerStub;
        } finally {
            Allure.setLifecycle(defaultLifecycle);
            StepsAspects.setLifecycle(defaultLifecycle);
            AttachmentsAspects.setLifecycle(defaultLifecycle);
            if (server != null && server.isRunning()) {
                server.stop();
            }
            if (client != null && !client.hasStopped()) {
                client.stop();
            }
        }
    }
}
