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

import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class BiDiSessionState {

    private final BiDiConfiguration configuration;
    private final BiDiAttachmentStorage storage = new BiDiAttachmentStorage();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<RecordingSession> recordingSession = new AtomicReference<>();
    private final AtomicReference<AllureExternalKey> ownerKey = new AtomicReference<>();

    private BiDiSessionState(final BiDiConfiguration configuration) {
        this.configuration = configuration;
    }

    void setOwnerKey(final AllureExternalKey key) {
        ownerKey.set(key);
    }

    static BiDiSessionState start(final WebDriver driver,
                                  final BiDiConfiguration configuration,
                                  final BiDiSessionFactory sessionFactory) {
        final BiDiSessionState state = new BiDiSessionState(configuration);
        final RecordingSession session = sessionFactory.start(
                driver,
                configuration,
                state::recordLog,
                state::recordNetwork
        );
        if (session == null) {
            return null;
        }
        state.recordingSession.set(session);
        return state;
    }

    void recordLog(final BiDiLogEvent event) {
        if (closed.get() || !configuration.isLogsEnabled()) {
            return;
        }
        storage.addLog(
                event,
                configuration.getMaxLogEntries()
        );
    }

    void recordNetwork(final BiDiNetworkEvent event) {
        if (closed.get() || !configuration.isNetworkEnabled()) {
            return;
        }
        storage.addNetwork(
                event,
                configuration.getMaxNetworkEvents(),
                configuration.getHeaderRedactor()
        );
    }

    void flushAndClose(final AllureLifecycle lifecycle) {
        closed.set(true);
        try {
            closeRecordingSession();
        } finally {
            storage.flush(lifecycle, ownerKey.get());
        }
    }

    void close() {
        closed.set(true);
        try {
            closeRecordingSession();
        } finally {
            storage.clear();
        }
    }

    private void closeRecordingSession() {
        final RecordingSession session = recordingSession.getAndSet(null);
        if (session != null) {
            session.close();
        }
    }
}
