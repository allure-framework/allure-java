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
package io.qameta.allure.playwright;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class AllurePlaywrightRegistry {

    private static final ThreadLocal<State> STATE = new ThreadLocal<State>() {
        @Override
        protected State initialValue() {
            return new State();
        }
    };

    private AllurePlaywrightRegistry() {
    }

    static void register(final Page page) {
        if (page == null) {
            return;
        }
        registerPage(page);
        try {
            register(page.context());
        } catch (RuntimeException ignored) {
            // The page may already be closed. Failure diagnostics must not alter test behavior.
        }
    }

    static void register(final BrowserContext context) {
        if (context != null) {
            STATE.get().contexts.add(context);
        }
    }

    static void register(final DefaultTraceSession traceSession) {
        STATE.get().traceSessions.add(traceSession);
    }

    static void registerPage(final Page page) {
        if (page != null) {
            STATE.get().pages.add(page);
        }
    }

    static Set<Page> getPages() {
        final State state = STATE.get();
        for (BrowserContext context : state.contexts) {
            try {
                state.pages.addAll(context.pages());
            } catch (RuntimeException ignored) {
                // The context may already be closed. Failure diagnostics must not alter test behavior.
            }
        }
        return state.pages;
    }

    static Set<DefaultTraceSession> getTraceSessions() {
        return STATE.get().traceSessions;
    }

    static List<DefaultTraceSession> getTraceSessions(final BrowserContext context) {
        final List<DefaultTraceSession> result = new ArrayList<>();
        for (DefaultTraceSession traceSession : STATE.get().traceSessions) {
            if (traceSession.isFor(context)) {
                result.add(traceSession);
            }
        }
        return result;
    }

    static boolean markFailureArtifactsAttached() {
        final State state = STATE.get();
        if (state.failureArtifactsAttached) {
            return false;
        }
        state.failureArtifactsAttached = true;
        return true;
    }

    static boolean markClosePageLogsAttached(final Page page) {
        return STATE.get().pagesWithCloseLogs.add(page);
    }

    static boolean markCloseVideoAttached(final Page page) {
        return STATE.get().pagesWithCloseVideo.add(page);
    }

    static void clear() {
        STATE.remove();
    }

    private static final class State {
        private final Set<Page> pages = identitySet();
        private final Set<BrowserContext> contexts = identitySet();
        private final Set<DefaultTraceSession> traceSessions = identitySet();
        private final Set<Page> pagesWithCloseLogs = identitySet();
        private final Set<Page> pagesWithCloseVideo = identitySet();
        private boolean failureArtifactsAttached;

        private static <T> Set<T> identitySet() {
            return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>());
        }
    }
}
