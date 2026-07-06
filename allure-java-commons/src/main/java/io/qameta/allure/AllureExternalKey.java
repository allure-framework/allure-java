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
package io.qameta.allure;

import io.qameta.allure.util.ResultsUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Adapter-owned lifecycle identity.
 *
 * <p>A key is a <em>recomputable</em> identity built from framework data: reconstruct the same key in the start and
 * stop hooks and equal keys resolve to the same internal Allure item, so neither the adapter nor the lifecycle needs a
 * {@code Map<frameworkId, uuid>}. The namespace is the integration class (provenance, and no ad-hoc constant strings);
 * the values are the framework data that identifies the entity.</p>
 *
 * <p>The key reduces its values to a digest at construction and retains no framework objects. Value contract: at
 * least one value (enforced by the signature), no {@code null} values, no array values, and every value must have a
 * stable, value-based serialized form ({@code String.valueOf}) for the lifetime of the run. The key must be unique
 * per live entity: a retry of the same test must not recompute an equal key while the previous entity is still
 * unwritten — include an attempt counter in the values when the framework can re-run the same id within one run.</p>
 *
 * <p>Keys are process-local and in-memory only; they are never serialized into result files.</p>
 */
public final class AllureExternalKey {

    private static final int DISPLAY_MAX_LENGTH = 160;

    private final byte[] digest;

    private final int hash;

    private final String display;

    private AllureExternalKey(final byte[] digest, final String display) {
        this.digest = digest;
        this.hash = Arrays.hashCode(digest);
        this.display = display;
    }

    /**
     * Creates a key with the given namespace class and at least one identifying value.
     *
     * @param namespace the integration class that owns the key
     * @param first     the first identifying value (required)
     * @param rest      the remaining identifying values
     * @return the key
     */
    public static AllureExternalKey of(final Class<?> namespace, final Object first, final Object... rest) {
        Objects.requireNonNull(namespace, "namespace");
        final MessageDigest digest = ResultsUtils.getMd5Digest();
        final StringBuilder display = new StringBuilder(namespace.getSimpleName()).append('[');
        update(digest, namespace.getName());
        appendValue(digest, display, first);
        for (final Object value : rest) {
            display.append(", ");
            appendValue(digest, display, value);
        }
        display.append(']');
        return new AllureExternalKey(digest.digest(), truncate(display));
    }

    /**
     * Creates a key with the given namespace class and a random UUIDv4 value, for frameworks that expose no stable id.
     *
     * @param namespace the integration class that owns the key
     * @return the key
     */
    public static AllureExternalKey random(final Class<?> namespace) {
        return of(namespace, UUID.randomUUID().toString());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AllureExternalKey)) {
            return false;
        }
        final AllureExternalKey that = (AllureExternalKey) o;
        return hash == that.hash && Arrays.equals(digest, that.digest);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return display;
    }

    private static void appendValue(final MessageDigest digest, final StringBuilder display, final Object value) {
        final String serialized = serialize(value);
        update(digest, serialized);
        display.append(serialized);
    }

    private static String serialize(final Object value) {
        Objects.requireNonNull(value, "key value must not be null");
        if (value.getClass().isArray()) {
            throw new IllegalArgumentException(
                    "key value must not be an array (arrays have no stable value-based serialized form)"
            );
        }
        final String serialized = String.valueOf(value);
        if (serialized.isEmpty()) {
            throw new IllegalArgumentException("key value must not be empty");
        }
        return serialized;
    }

    private static void update(final MessageDigest digest, final String value) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static String truncate(final StringBuilder display) {
        return display.length() <= DISPLAY_MAX_LENGTH
                ? display.toString()
                : display.substring(0, DISPLAY_MAX_LENGTH) + "…";
    }
}
