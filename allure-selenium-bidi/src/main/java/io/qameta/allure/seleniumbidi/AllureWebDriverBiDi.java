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

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Selenium WebDriver BiDi listener that captures browser log and network events
 * as aggregated Allure attachments.
 */
@SuppressWarnings("unused")
public class AllureWebDriverBiDi implements WebDriverListener, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureWebDriverBiDi.class);

    private final AllureLifecycle lifecycle;
    private final BiDiSessionFactory sessionFactory;
    private final BiDiConfiguration configuration = new BiDiConfiguration();
    private final Map<WebDriver, BiDiSessionState> sessions = new IdentityHashMap<>();
    private final Lock sessionsLock = new ReentrantLock();

    /**
     * Creates an Allure web driver bi di with default configuration.
     */
    public AllureWebDriverBiDi() {
        this(Allure.getLifecycle(), new SeleniumBiDiSessionFactory());
    }

    AllureWebDriverBiDi(final AllureLifecycle lifecycle,
                        final BiDiSessionFactory sessionFactory) {
        this.lifecycle = lifecycle;
        this.sessionFactory = sessionFactory;
    }

    /**
     * Decorates the supplied object with Allure reporting behavior.
     *
     * @param driver the WebDriver instance to decorate or observe
     * @return the decorate
     */
    public <T extends WebDriver> T decorate(final T driver) {
        return new EventFiringDecorator<T>(this).decorate(driver);
    }

    /**
     * Configures logs.
     *
     * @param enabled whether the option should be enabled
     * @return this instance for method chaining
     */
    public AllureWebDriverBiDi logs(final boolean enabled) {
        configuration.setLogsEnabled(enabled);
        return this;
    }

    /**
     * Configures network.
     *
     * @param enabled whether the option should be enabled
     * @return this instance for method chaining
     */
    public AllureWebDriverBiDi network(final boolean enabled) {
        configuration.setNetworkEnabled(enabled);
        return this;
    }

    /**
     * Configures max log entries.
     *
     * @param maxLogEntries the maximum number of log entries to keep
     * @return this instance for method chaining
     */
    public AllureWebDriverBiDi maxLogEntries(final int maxLogEntries) {
        configuration.setMaxLogEntries(maxLogEntries);
        return this;
    }

    /**
     * Configures max network events.
     *
     * @param maxNetworkEvents the maximum number of network events to keep
     * @return this instance for method chaining
     */
    public AllureWebDriverBiDi maxNetworkEvents(final int maxNetworkEvents) {
        configuration.setMaxNetworkEvents(maxNetworkEvents);
        return this;
    }

    /**
     * Configures redact headers.
     *
     * @param headerNames the header names whose values should be redacted
     * @return this instance for method chaining
     */
    public AllureWebDriverBiDi redactHeaders(final String... headerNames) {
        configuration.redactHeaders(headerNames);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeAnyWebDriverCall(final WebDriver driver,
                                       final Method method,
                                       final Object[] args) {
        captureActiveAllureContext(driver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterAnyWebDriverCall(final WebDriver driver,
                                      final Method method,
                                      final Object[] args,
                                      final Object result) {
        captureActiveAllureContext(driver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeQuit(final WebDriver driver) {
        closeSession(driver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterQuit(final WebDriver driver) {
        closeSession(driver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        final List<BiDiSessionState> activeSessions = new ArrayList<>();
        sessionsLock.lock();
        try {
            activeSessions.addAll(sessions.values());
            sessions.clear();
        } finally {
            sessionsLock.unlock();
        }
        activeSessions.forEach(this::safeFlushAndClose);
    }

    private void captureActiveAllureContext(final WebDriver driver) {
        if (!configuration.isAnyEnabled()) {
            return;
        }

        if (!lifecycle.getCurrentTestCaseOrStep().isPresent()) {
            return;
        }

        getOrCreateSession(driver);
    }

    private BiDiSessionState getOrCreateSession(final WebDriver driver) {
        sessionsLock.lock();
        try {
            final BiDiSessionState existing = sessions.get(driver);
            if (existing != null) {
                return existing;
            }

            try {
                final BiDiSessionState created = BiDiSessionState.start(driver, configuration, sessionFactory);
                if (created != null) {
                    sessions.put(driver, created);
                }
                return created;
            } catch (RuntimeException e) {
                LOGGER.debug("Could not start WebDriver BiDi capture", e);
                return null;
            }
        } finally {
            sessionsLock.unlock();
        }
    }

    private void closeSession(final WebDriver driver) {
        final BiDiSessionState state;
        sessionsLock.lock();
        try {
            state = sessions.remove(driver);
        } finally {
            sessionsLock.unlock();
        }
        safeFlushAndClose(state);
    }

    private void safeFlushAndClose(final BiDiSessionState state) {
        if (state == null) {
            return;
        }
        try {
            state.flushAndClose(lifecycle);
        } catch (RuntimeException e) {
            LOGGER.debug("Could not flush WebDriver BiDi capture", e);
        }
    }
}
