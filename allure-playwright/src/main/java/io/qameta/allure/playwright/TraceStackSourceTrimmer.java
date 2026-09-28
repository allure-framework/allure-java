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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Trims the AspectJ-weaving frames that {@code AllurePlaywrightAspect} inserts in front of every
 * advised Playwright call before a trace's {@code trace.stacks} entry is used by Trace Viewer.
 */
final class TraceStackSourceTrimmer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceStackSourceTrimmer.class);

    private static final String STACKS_ENTRY = "trace.stacks";

    // Playwright puts screenshots/snapshots/embedded-source blobs here - skip them completely.
    private static final String RESOURCES_PREFIX = "resources/";

    private static final Pattern SYNTHETIC_FRAME = Pattern.compile(
            "^org\\.aspectj\\.runtime\\.reflect\\.JoinPointImpl\\."
                    + "|^io\\.qameta\\.allure\\.playwright\\.AllurePlaywrightAspect\\."
                    + "|_aroundBody\\d+"
                    + "|\\$AjcClosure\\d+"
    );

    private TraceStackSourceTrimmer() {
    }

    /**
     * Rewrites {@code trace.stacks} inside the given trace archive in place, if present.
     *
     * <p>If the archive has no {@code trace.stacks} entry at all — which is what actually happens when
     * {@code PLAYWRIGHT_JAVA_SRC} was never configured for this session, since Playwright's Java client
     * doesn't collect a stack per call at all in that case — there's nothing to trim. Checking for the
     * entry up front (an O(1) central-directory lookup) skips the full unzip/rewrite of a potentially large
     * archive (screenshots, snapshots, network capture) in that, likely common, case.</p>
     *
     * @param trace path to a Playwright trace zip, as produced by {@code Tracing.stop()}.
     */
    static void trim(final Path trace) {
        Path rewritten = null;
        try {
            if (!hasStacksEntry(trace)) {
                return;
            }
            rewritten = Files.createTempFile("allure-playwright-trace-trimmed-", ".zip");
            if (rewrite(trace, rewritten)) {
                Files.move(rewritten, trace, StandardCopyOption.REPLACE_EXISTING);
                rewritten = null;
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not trim Playwright trace stacks, attaching the trace unchanged", e);
        } finally {
            deleteIfExists(rewritten);
        }
    }

    private static boolean hasStacksEntry(final Path trace) throws IOException {
        try (ZipFile zip = new ZipFile(trace.toFile())) {
            return zip.getEntry(STACKS_ENTRY) != null;
        }
    }

    private static boolean rewrite(final Path trace, final Path rewritten) throws IOException {
        boolean stacksFound = false;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(trace));
                ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(rewritten))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final String name = entry.getName();
                final byte[] content = readAll(zis);
                if (STACKS_ENTRY.equals(name)) {
                    stacksFound = true;
                    writeEntry(zos, name, trimStacksJson(content), false);
                } else {
                    writeEntry(zos, name, content, name.startsWith(RESOURCES_PREFIX));
                }
            }
        }
        return stacksFound;
    }

    private static void writeEntry(final ZipOutputStream zos, final String name, final byte[] content,
                                   final boolean stored)
            throws IOException {
        final ZipEntry outEntry = new ZipEntry(name);
        if (stored) {
            final CRC32 crc = new CRC32();
            crc.update(content);
            outEntry.setMethod(ZipEntry.STORED);
            outEntry.setSize(content.length);
            outEntry.setCompressedSize(content.length);
            outEntry.setCrc(crc.getValue());
        }
        zos.putNextEntry(outEntry);
        zos.write(content);
        zos.closeEntry();
    }

    /**
     * Pure JSON transform: trims each recorded stack down to its first real-looking frame.
     *
     * @param stacksJson the raw {@code trace.stacks} entry content, shaped as
     *         {@code {"files": [...], "stacks": [[callId, [[fileIndex, line, column, name], ...]], ...]}}.
     * @return the rewritten content, same shape, synthetic leading frames removed from each stack.
     */
    static byte[] trimStacksJson(final byte[] stacksJson) {
        final JsonObject root = JsonParser.parseString(new String(stacksJson, StandardCharsets.UTF_8))
                .getAsJsonObject();
        final JsonArray files = root.getAsJsonArray("files");
        final JsonArray stacks = root.getAsJsonArray("stacks");
        for (JsonElement stackElement : stacks) {
            final JsonArray stackEntry = stackElement.getAsJsonArray();
            final JsonArray frames = stackEntry.get(1).getAsJsonArray();
            final int cut = firstUsableFrameIndex(files, frames);
            if (cut > 0) {
                stackEntry.set(1, dropLeadingFrames(frames, cut));
            }
        }
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Finds where a stack should be cut, combining two signals.
     *
     * <p><b>Primary:</b> the first frame anywhere in the stack that's file-resolvable. This is authoritative
     * wherever it fires — once {@code PLAYWRIGHT_JAVA_SRC} is configured, only frames under the consumer's own
     * source root ever resolve, so the shallowest resolved frame is the real caller, no matter how many
     * library-internal frames sit between it and frame 0.</p>
     *
     * <p><b>Backstop:</b> if no frame anywhere resolves a file (e.g. {@code sources(true)} was set without
     * ever configuring {@code PLAYWRIGHT_JAVA_SRC}), fall back to trimming the leading run of frames
     * recognizable as synthetic AspectJ/Playwright-weaving glue. If that run reaches the end of the stack with
     * nothing left after it, leave the stack untouched instead of trimming it down to nothing.</p>
     */
    private static int firstUsableFrameIndex(final JsonArray files, final JsonArray frames) {
        int resolvedAt = -1;
        int syntheticRun = 0;
        boolean inLeadingRun = true;
        for (int i = 0; i < frames.size(); i++) {
            final JsonArray frame = frames.get(i).getAsJsonArray();
            if (resolvedAt < 0 && isFileResolved(files, frame)) {
                resolvedAt = i;
            }
            if (inLeadingRun) {
                if (isSyntheticFrame(frame)) {
                    syntheticRun = i + 1;
                } else {
                    inLeadingRun = false;
                }
            }
        }
        if (resolvedAt >= 0) {
            return resolvedAt;
        }
        return syntheticRun < frames.size() ? syntheticRun : 0;
    }

    private static boolean isFileResolved(final JsonArray files, final JsonArray frame) {
        final int fileIndex = frame.get(0).getAsInt();
        return fileIndex >= 0
                && fileIndex < files.size()
                && !files.get(fileIndex).getAsString().isEmpty();
    }

    private static boolean isSyntheticFrame(final JsonArray frame) {
        return SYNTHETIC_FRAME.matcher(frame.get(3).getAsString()).find();
    }

    private static JsonArray dropLeadingFrames(final JsonArray frames, final int cut) {
        final JsonArray trimmed = new JsonArray();
        for (int i = cut; i < frames.size(); i++) {
            trimmed.add(frames.get(i));
        }
        return trimmed;
    }

    private static byte[] readAll(final InputStream in) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static void deleteIfExists(final Path file) {
        if (Objects.isNull(file)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.warn("Could not delete temporary file {}", file, e);
        }
    }
}
