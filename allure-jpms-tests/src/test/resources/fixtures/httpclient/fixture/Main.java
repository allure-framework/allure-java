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
package fixture;

import com.sun.net.httpserver.HttpServer;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.javahttpclient.AllureHttpClient;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Main {

    public static void main(final String[] args) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/module", exchange -> {
            final byte[] body = "module response".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            final AllureExternalKey test = AllureExternalKey.of(Main.class, "http");
            Allure.getLifecycle().scheduleTest(List.of(), test, new TestResult().setName("modular HTTP"));
            Allure.getLifecycle().startTest(test);
            final HttpClient client = new AllureHttpClient(HttpClient.newHttpClient());
            final URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/module");
            final HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200 || !"module response".equals(response.body())) {
                throw new AssertionError("Unexpected HTTP response: " + response);
            }
            Allure.getLifecycle().updateTest(result -> result.setStatus(Status.PASSED));
            Allure.getLifecycle().stopTest(test);
            Allure.getLifecycle().writeTest(test);
        } finally {
            server.stop(0);
        }
    }
}
