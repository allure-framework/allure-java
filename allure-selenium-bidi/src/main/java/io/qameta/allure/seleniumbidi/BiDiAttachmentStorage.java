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
package io.qameta.allure.seleniumbidi;

import io.qameta.allure.AllureLifecycle;
import org.openqa.selenium.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class BiDiAttachmentStorage {

    static final String LOG_ATTACHMENT_NAME = "WebDriver BiDi logs";
    static final String NETWORK_ATTACHMENT_NAME = "WebDriver BiDi network";

    private static final String JSON_TYPE = "application/json";
    private static final String JSON_EXTENSION = "json";
    private static final Json JSON = new Json();

    private final List<Map<String, Object>> logs = new ArrayList<>();
    private final List<Map<String, Object>> network = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    private long droppedLogs;
    private long droppedNetworkEvents;

    void addLog(final BiDiLogEvent event, final int maxEntries) {
        lock.lock();
        try {
            if (logs.size() < maxEntries) {
                logs.add(event.toMap());
            } else {
                droppedLogs++;
            }
        } finally {
            lock.unlock();
        }
    }

    void addNetwork(final BiDiNetworkEvent event,
                    final int maxEntries,
                    final HeaderRedactor redactor) {
        lock.lock();
        try {
            if (network.size() < maxEntries) {
                network.add(event.toMap(redactor));
            } else {
                droppedNetworkEvents++;
            }
        } finally {
            lock.unlock();
        }
    }

    void flush(final AllureLifecycle lifecycle) {
        try {
            if (!lifecycle.getCurrentTestCaseOrStep().isPresent()) {
                return;
            }

            logsAttachment()
                    .ifPresent(
                            body -> lifecycle.addAttachment(
                                    LOG_ATTACHMENT_NAME,
                                    JSON_TYPE,
                                    JSON_EXTENSION,
                                    body
                            )
                    );
            networkAttachment()
                    .ifPresent(
                            body -> lifecycle.addAttachment(
                                    NETWORK_ATTACHMENT_NAME,
                                    JSON_TYPE,
                                    JSON_EXTENSION,
                                    body
                            )
                    );
        } finally {
            clear();
        }
    }

    void clear() {
        lock.lock();
        try {
            logs.clear();
            network.clear();
            droppedLogs = 0;
            droppedNetworkEvents = 0;
        } finally {
            lock.unlock();
        }
    }

    Optional<byte[]> logsAttachment() {
        lock.lock();
        try {
            if (logs.isEmpty() && droppedLogs == 0) {
                return Optional.empty();
            }
            return Optional.of(toJson(new ArrayList<>(logs), droppedLogs));
        } finally {
            lock.unlock();
        }
    }

    Optional<byte[]> networkAttachment() {
        lock.lock();
        try {
            if (network.isEmpty() && droppedNetworkEvents == 0) {
                return Optional.empty();
            }
            return Optional.of(toJson(new ArrayList<>(network), droppedNetworkEvents));
        } finally {
            lock.unlock();
        }
    }

    private static byte[] toJson(final List<Map<String, Object>> entries, final long dropped) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entries", entries);
        payload.put("dropped", dropped);
        return JSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
    }
}
