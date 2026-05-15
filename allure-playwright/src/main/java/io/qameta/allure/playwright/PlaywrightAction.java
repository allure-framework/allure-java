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

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("PMD.GodClass")
final class PlaywrightAction {

    private static final String METHOD_NAVIGATE = "navigate";
    private static final String METHOD_CLICK = "click";
    private static final String METHOD_DBLCLICK = "dblclick";
    private static final String METHOD_CHECK = "check";
    private static final String METHOD_UNCHECK = "uncheck";
    private static final String METHOD_TAP = "tap";
    private static final String METHOD_FILL = "fill";
    private static final String METHOD_CLEAR = "clear";
    private static final String METHOD_PRESS = "press";
    private static final String METHOD_TYPE = "type";
    private static final String METHOD_HOVER = "hover";
    private static final String METHOD_DRAG_TO = "dragTo";
    private static final String METHOD_DRAG_AND_DROP = "dragAndDrop";
    private static final String METHOD_SET_INPUT_FILES = "setInputFiles";
    private static final String METHOD_SELECT_OPTION = "selectOption";
    private static final String METHOD_DISPATCH_EVENT = "dispatchEvent";
    private static final String DRAG_PREFIX = "Drag ";
    private static final String ON = " on ";
    private static final String ASSERTIONS_PACKAGE = ".assertions.";
    private static final String SELECTOR = "selector";
    private static final String LOCATOR = "locator";
    private static final String ELEMENT = "element";
    private static final String REDACTED = "<redacted>";
    private static final String SPACE = " ";

    private static final Set<String> ACTION_METHODS = Collections.unmodifiableSet(
            new HashSet<String>(
                    Arrays.asList(
                            METHOD_NAVIGATE,
                            METHOD_CLICK,
                            METHOD_DBLCLICK,
                            METHOD_CHECK,
                            METHOD_UNCHECK,
                            METHOD_TAP,
                            METHOD_FILL,
                            METHOD_CLEAR,
                            METHOD_PRESS,
                            METHOD_TYPE,
                            METHOD_HOVER,
                            METHOD_DRAG_TO,
                            METHOD_DRAG_AND_DROP,
                            METHOD_SET_INPUT_FILES,
                            METHOD_SELECT_OPTION,
                            METHOD_DISPATCH_EVENT,
                            "focus",
                            "blur",
                            "setChecked",
                            "setContent",
                            "reload",
                            "goBack",
                            "goForward",
                            "bringToFront",
                            "close",
                            "waitForCondition",
                            "waitForClose",
                            "waitForConsoleMessage",
                            "waitForDownload",
                            "waitForFileChooser",
                            "waitForFunction",
                            "waitForLoadState",
                            "waitForNavigation",
                            "waitForPopup",
                            "waitForRequest",
                            "waitForRequestFinished",
                            "waitForResponse",
                            "waitForSelector",
                            "waitForTimeout",
                            "waitForURL",
                            "waitForWebSocket",
                            "waitForWorker"
                    )
            )
    );

    private static final Map<String, String> ACTION_PREFIXES = createActionPrefixes();

    private static final Set<String> IGNORED_METHODS = Collections.unmodifiableSet(
            new HashSet<String>(
                    Arrays.asList(
                            "equals",
                            "hashCode",
                            "toString",
                            "not"
                    )
            )
    );

    private final boolean logged;
    private final String name;

    private PlaywrightAction(final boolean logged, final String name) {
        this.logged = logged;
        this.name = name;
    }

    boolean isLogged() {
        return logged;
    }

    String getName() {
        return name;
    }

    static PlaywrightAction from(final JoinPoint joinPoint, final boolean screenshot) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final String method = signature.getName();
        if (screenshot) {
            return new PlaywrightAction(true, "Take screenshot");
        }
        if (!shouldLog(joinPoint, method)) {
            return new PlaywrightAction(false, method);
        }
        return new PlaywrightAction(true, name(joinPoint, method));
    }

    private static boolean shouldLog(final JoinPoint joinPoint, final String method) {
        if (IGNORED_METHODS.contains(method)) {
            return false;
        }
        if (AllurePlaywrightConfig.isAllMode()) {
            return true;
        }
        return isAssertion(joinPoint) || ACTION_METHODS.contains(method);
    }

    private static String name(final JoinPoint joinPoint, final String method) {
        final String result;
        if (isAssertion(joinPoint)) {
            result = "Expect " + target(joinPoint) + SPACE + method;
        } else if (METHOD_NAVIGATE.equals(method)) {
            result = "Navigate to " + arg(joinPoint, 0, "url");
        } else if (METHOD_FILL.equals(method)) {
            result = "Fill " + target(joinPoint) + " with " + secret(joinPoint, 1);
        } else if (METHOD_PRESS.equals(method)) {
            result = "Press " + secret(joinPoint, 0) + ON + target(joinPoint);
        } else if (METHOD_TYPE.equals(method)) {
            result = "Type " + secret(joinPoint, 1) + " into " + target(joinPoint);
        } else if (METHOD_DISPATCH_EVENT.equals(method)) {
            result = "Dispatch " + arg(joinPoint, 1, "event") + ON + target(joinPoint);
        } else {
            result = prefix(method) + target(joinPoint);
        }
        return result;
    }

    private static String prefix(final String method) {
        final String prefix = ACTION_PREFIXES.get(method);
        return prefix == null ? humanize(method) + SPACE : prefix;
    }

    private static Map<String, String> createActionPrefixes() {
        final Map<String, String> prefixes = new HashMap<>();
        prefixes.put(METHOD_CLICK, "Click ");
        prefixes.put(METHOD_DBLCLICK, "Double click ");
        prefixes.put(METHOD_HOVER, "Hover ");
        prefixes.put(METHOD_DRAG_TO, DRAG_PREFIX);
        prefixes.put(METHOD_DRAG_AND_DROP, DRAG_PREFIX);
        prefixes.put(METHOD_SET_INPUT_FILES, "Set input files for ");
        prefixes.put(METHOD_SELECT_OPTION, "Select option in ");
        return Collections.unmodifiableMap(prefixes);
    }

    private static boolean isAssertion(final JoinPoint joinPoint) {
        final String name = joinPoint.getSignature().getDeclaringTypeName();
        final String targetName = joinPoint.getTarget() == null ? "" : joinPoint.getTarget().getClass().getName();
        return name.contains(ASSERTIONS_PACKAGE) || targetName.contains(ASSERTIONS_PACKAGE);
    }

    private static String target(final JoinPoint joinPoint) {
        final Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String) {
            return String.valueOf(args[0]);
        }
        final String locator = locatorDescription(joinPoint.getTarget());
        if (locator != null && !locator.isEmpty()) {
            return locator;
        }
        return targetKind(joinPoint);
    }

    private static String targetKind(final JoinPoint joinPoint) {
        final String simpleName = joinPoint.getSignature().getDeclaringType().getSimpleName();
        if (simpleName.contains("Locator")) {
            return LOCATOR;
        }
        if (simpleName.contains("Element")) {
            return ELEMENT;
        }
        return simpleName.toLowerCase(Locale.ROOT);
    }

    private static String arg(final JoinPoint joinPoint, final int index, final String fallback) {
        final Object[] args = joinPoint.getArgs();
        if (index >= args.length || args[index] == null) {
            return fallback;
        }
        return String.valueOf(args[index]);
    }

    private static String secret(final JoinPoint joinPoint, final int index) {
        if (AllurePlaywrightConfig.isParametersRedacted()) {
            return REDACTED;
        }
        return arg(joinPoint, index, REDACTED);
    }

    private static String humanize(final String method) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < method.length(); i++) {
            final char ch = method.charAt(i);
            if (Character.isUpperCase(ch)) {
                builder.append(' ').append(Character.toLowerCase(ch));
            } else {
                builder.append(ch);
            }
        }
        builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
        return builder.toString();
    }

    private static String locatorDescription(final Object target) {
        final String description = invokeDescription(target);
        if (description != null && !description.isEmpty()) {
            return description;
        }
        return readSelector(target);
    }

    private static String invokeDescription(final Object target) {
        if (target == null) {
            return null;
        }
        try {
            final Method method = target.getClass().getMethod("description");
            final Object value = method.invoke(target);
            return value == null ? null : String.valueOf(value);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static String readSelector(final Object target) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField(SELECTOR);
                field.setAccessible(true);
                final Object value = field.get(target);
                return value == null ? null : String.valueOf(value);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        return null;
    }
}
