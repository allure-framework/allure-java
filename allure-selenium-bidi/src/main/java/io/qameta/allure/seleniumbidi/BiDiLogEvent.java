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

import org.openqa.selenium.bidi.log.BaseLogEntry;
import org.openqa.selenium.bidi.log.ConsoleLogEntry;
import org.openqa.selenium.bidi.log.GenericLogEntry;
import org.openqa.selenium.bidi.log.JavascriptLogEntry;
import org.openqa.selenium.bidi.log.LogEntry;
import org.openqa.selenium.bidi.log.StackFrame;
import org.openqa.selenium.bidi.log.StackTrace;
import org.openqa.selenium.bidi.script.RemoteValue;
import org.openqa.selenium.bidi.script.Source;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
final class BiDiLogEvent {

    private final Map<String, Object> values;

    private BiDiLogEvent(final Map<String, Object> values) {
        this.values = values;
    }

    static List<BiDiLogEvent> from(final LogEntry entry) {
        final List<BiDiLogEvent> events = new ArrayList<>(3);
        entry.getConsoleLogEntry().ifPresent(console -> events.add(console(console)));
        entry.getJavascriptLogEntry().ifPresent(javascript -> events.add(javascript(javascript)));
        entry.getGenericLogEntry().ifPresent(generic -> events.add(generic(generic)));
        return events;
    }

    static BiDiLogEvent generic(final GenericLogEntry entry) {
        final Map<String, Object> values = base("generic", entry);
        putIfNotNull(values, BiDiJsonKeys.TYPE, entry.getType());
        return new BiDiLogEvent(values);
    }

    static BiDiLogEvent console(final ConsoleLogEntry entry) {
        final Map<String, Object> values = base("console", entry);
        putIfNotNull(values, BiDiJsonKeys.TYPE, entry.getType());
        putIfNotNull(values, "method", entry.getMethod());
        values.put("args", remoteValues(entry.getArgs()));
        return new BiDiLogEvent(values);
    }

    static BiDiLogEvent javascript(final JavascriptLogEntry entry) {
        final Map<String, Object> values = base("javascript", entry);
        putIfNotNull(values, BiDiJsonKeys.TYPE, entry.getType());
        return new BiDiLogEvent(values);
    }

    static BiDiLogEvent of(final String text) {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put(BiDiJsonKeys.EVENT, "log");
        values.put(BiDiJsonKeys.TEXT, text);
        return new BiDiLogEvent(values);
    }

    Map<String, Object> toMap() {
        return new LinkedHashMap<>(values);
    }

    private static Map<String, Object> base(final String event, final BaseLogEntry entry) {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put(BiDiJsonKeys.EVENT, event);
        putIfNotNull(values, "level", entry.getLevel());
        putIfNotNull(values, BiDiJsonKeys.TEXT, entry.getText());
        values.put("timestamp", entry.getTimestamp());
        putIfNotNull(values, "source", source(entry.getSource()));
        putIfNotNull(values, "stackTrace", stackTrace(entry.getStackTrace()));
        return values;
    }

    private static Map<String, Object> source(final Source source) {
        if (source == null) {
            return null;
        }
        final Map<String, Object> values = new LinkedHashMap<>();
        putIfNotNull(values, "realm", source.getRealm());
        source.getBrowsingContext().ifPresent(context -> values.put("browsingContextId", context));
        return values;
    }

    private static List<Map<String, Object>> remoteValues(final List<RemoteValue> remoteValues) {
        final List<Map<String, Object>> result = new ArrayList<>();
        if (remoteValues == null) {
            return result;
        }
        remoteValues.forEach(remoteValue -> {
            final Map<String, Object> value = new LinkedHashMap<>();
            putIfNotNull(value, BiDiJsonKeys.TYPE, remoteValue.getType());
            remoteValue.getValue().ifPresent(content -> value.put(BiDiJsonKeys.VALUE, content));
            remoteValue.getHandle().ifPresent(handle -> value.put("handle", handle));
            remoteValue.getInternalId().ifPresent(id -> value.put("internalId", id));
            remoteValue.getSharedId().ifPresent(id -> value.put("sharedId", id));
            result.add(value);
        });
        return result;
    }

    static Map<String, Object> stackTrace(final StackTrace stackTrace) {
        if (stackTrace == null) {
            return null;
        }
        final Map<String, Object> values = new LinkedHashMap<>();
        final List<Map<String, Object>> frames = new ArrayList<>();
        stackTrace.getCallFrames().forEach(frame -> frames.add(stackFrame(frame)));
        values.put("callFrames", frames);
        return values;
    }

    private static Map<String, Object> stackFrame(final StackFrame frame) {
        final Map<String, Object> values = new LinkedHashMap<>();
        putIfNotNull(values, "url", frame.getUrl());
        putIfNotNull(values, "functionName", frame.getFunctionName());
        values.put("lineNumber", frame.getLineNumber());
        values.put("columnNumber", frame.getColumnNumber());
        return values;
    }

    static void putIfNotNull(final Map<String, Object> values, final String name, final Object value) {
        if (value != null) {
            values.put(name, value);
        }
    }
}
