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
package io.qameta.allure.karate;

import io.qameta.allure.Allure;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import io.qameta.allure.test.IsolatedLifecycle;

@SuppressWarnings("MultipleStringLiterals")
@IsolatedLifecycle
public class TestRunner {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean serverRunning;

    @TempDir
    protected Path temp;

    AllureResults runApi(final String... featurePath) {
        startServer();
        return run(featurePath);
    }

    AllureResults run(final String... path) {
        return Allure.step("Run Karate features and collect Allure results", () -> RunUtils.runTests(lifecycle -> {
            final AllureKarate allureKarate = new AllureKarate(lifecycle);

            try {
                io.karatelabs.core.Runner.builder()
                        .path(path)
                        .listener(allureKarate)
                        .systemProperty("mock.server.url", getMockServerUrl())
                        .backupOutputDir(false)
                        .outputDir(temp.resolve("karate-reports"))
                        .outputJunitXml(false)
                        .outputCucumberJson(false)
                        .outputHtmlReport(false)
                        .parallel(1);
            } finally {
                stopServer();
            }
        }));
    }

    private String getMockServerUrl() {
        return serverSocket == null ? "" : "http://localhost:" + serverSocket.getLocalPort();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            serverRunning = true;
            serverThread = new Thread(this::serve, "allure-karate-test-server");
            serverThread.setDaemon(true);
            serverThread.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start local test HTTP server", e);
        }
    }

    private void serve() {
        while (serverRunning) {
            try (Socket socket = serverSocket.accept()) {
                handle(socket);
            } catch (SocketException e) {
                if (serverRunning) {
                    throw new UncheckedIOException(e);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void handle(final Socket socket) throws IOException {
        socket.setSoTimeout(5_000);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), US_ASCII));
        final String requestLine = reader.readLine();
        if (requestLine == null) {
            return;
        }
        String line;
        do {
            line = reader.readLine();
        } while (line != null && !line.isEmpty());

        final String[] request = requestLine.split(" ");
        final MockResponse response = request.length < 2
                ? new MockResponse(400, "Bad Request", "")
                : getResponse(request[0], request[1]);

        final byte[] body = response.body().getBytes(UTF_8);
        final OutputStream output = socket.getOutputStream();
        output.write(
                String.format(
                        "HTTP/1.1 %d %s\r\nContent-Length: %d\r\nContent-Type: application/json\r\nConnection: close\r\n\r\n",
                        response.status(),
                        response.reason(),
                        body.length
                ).getBytes(US_ASCII)
        );
        output.write(body);
        output.flush();
    }

    private MockResponse getResponse(final String method, final String path) {
        final String normalizedMethod = method.toUpperCase(Locale.ROOT);
        return switch (normalizedMethod + " " + path) {
            case "GET /", "GET /login" -> new MockResponse(200, "OK", "");
            case "POST /login" -> new MockResponse(401, "Unauthorized", "[{\"message\":\"No access\"}]");
            case "GET /user" -> new MockResponse(301, "Moved Permanently", "");
            case "GET /pages" -> new MockResponse(404, "Not Found", "");
            case "GET /users" -> new MockResponse(
                    200,
                    "OK",
                    "[{\"id\":\"1\",\"name\":\"Soul\"},{\"id\":\"2\",\"name\":\"Kate\"}]"
            );
            case "POST /users/login" -> new MockResponse(
                    200,
                    "OK",
                    "{\"message\":\"User logged in\",\"error\":null}"
            );
            default -> new MockResponse(500, "Internal Server Error", "");
        };
    }

    private void stopServer() {
        serverRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (serverThread != null) {
            try {
                serverThread.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private record MockResponse(int status, String reason, String body) {
    }
}
