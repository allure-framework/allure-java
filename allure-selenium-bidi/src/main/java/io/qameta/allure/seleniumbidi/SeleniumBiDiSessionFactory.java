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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.module.LogInspector;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.remote.Augmenter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class SeleniumBiDiSessionFactory implements BiDiSessionFactory {

    @Override
    public RecordingSession start(final WebDriver driver,
                                  final BiDiConfiguration configuration,
                                  final Consumer<BiDiLogEvent> logConsumer,
                                  final Consumer<BiDiNetworkEvent> networkConsumer) {
        final WebDriver bidiDriver = toBiDiDriver(driver);
        if (bidiDriver == null) {
            return null;
        }

        final List<AutoCloseable> inspectors = new ArrayList<>();
        if (configuration.isLogsEnabled()) {
            final LogInspector logInspector = new LogInspector(bidiDriver);
            logInspector.onLog(entry -> BiDiLogEvent.from(entry).forEach(logConsumer));
            inspectors.add(logInspector);
        }
        if (configuration.isNetworkEnabled()) {
            final Network network = new Network(bidiDriver);
            network.onBeforeRequestSent(event -> networkConsumer.accept(BiDiNetworkEvent.beforeRequestSent(event)));
            network.onFetchError(event -> networkConsumer.accept(BiDiNetworkEvent.fetchError(event)));
            network.onResponseStarted(event -> networkConsumer.accept(BiDiNetworkEvent.responseStarted(event)));
            network.onResponseCompleted(event -> networkConsumer.accept(BiDiNetworkEvent.responseCompleted(event)));
            network.onAuthRequired(event -> networkConsumer.accept(BiDiNetworkEvent.authRequired(event)));
            inspectors.add(network);
        }
        return new SeleniumRecordingSession(inspectors);
    }

    private WebDriver toBiDiDriver(final WebDriver driver) {
        if (driver instanceof HasBiDi) {
            return driver;
        }

        final WebDriver augmented = new Augmenter().augment(driver);
        return augmented instanceof HasBiDi ? augmented : null;
    }

    /**
     * Recording session over Selenium BiDi inspectors; closing it closes every registered inspector.
     */
    private static final class SeleniumRecordingSession implements RecordingSession {

        private final List<AutoCloseable> inspectors;

        private SeleniumRecordingSession(final List<AutoCloseable> inspectors) {
            this.inspectors = inspectors;
        }

        @Override
        public void close() {
            inspectors.forEach(inspector -> {
                try {
                    inspector.close();
                } catch (Exception ignored) {
                    // ignore cleanup failures; reporting must not affect WebDriver teardown
                }
            });
        }
    }
}
