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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

final class BiDiConfiguration {

    static final int DEFAULT_MAX_LOG_ENTRIES = 1_000;
    static final int DEFAULT_MAX_NETWORK_EVENTS = 2_000;

    private final AtomicBoolean logsEnabled = new AtomicBoolean(true);
    private final AtomicBoolean networkEnabled = new AtomicBoolean(true);
    private final AtomicInteger maxLogEntries = new AtomicInteger(DEFAULT_MAX_LOG_ENTRIES);
    private final AtomicInteger maxNetworkEvents = new AtomicInteger(DEFAULT_MAX_NETWORK_EVENTS);
    private final AtomicReference<HeaderRedactor> headerRedactor = new AtomicReference<>(HeaderRedactor.defaults());

    boolean isLogsEnabled() {
        return logsEnabled.get();
    }

    void setLogsEnabled(final boolean logsEnabled) {
        this.logsEnabled.set(logsEnabled);
    }

    boolean isNetworkEnabled() {
        return networkEnabled.get();
    }

    void setNetworkEnabled(final boolean networkEnabled) {
        this.networkEnabled.set(networkEnabled);
    }

    boolean isAnyEnabled() {
        return logsEnabled.get() || networkEnabled.get();
    }

    int getMaxLogEntries() {
        return maxLogEntries.get();
    }

    void setMaxLogEntries(final int maxLogEntries) {
        this.maxLogEntries.set(requireNonNegative(maxLogEntries, "maxLogEntries"));
    }

    int getMaxNetworkEvents() {
        return maxNetworkEvents.get();
    }

    void setMaxNetworkEvents(final int maxNetworkEvents) {
        this.maxNetworkEvents.set(requireNonNegative(maxNetworkEvents, "maxNetworkEvents"));
    }

    HeaderRedactor getHeaderRedactor() {
        return headerRedactor.get();
    }

    void redactHeaders(final String... headerNames) {
        this.headerRedactor.set(HeaderRedactor.defaults().withAdditionalHeaders(headerNames));
    }

    private static int requireNonNegative(final int value, final String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be greater than or equal to zero");
        }
        return value;
    }
}
