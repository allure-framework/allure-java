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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Trims the AspectJ-weaving frames that {@code AllurePlaywrightAspect} inserts in front of every
 * advised Playwright call before a trace's {@code trace.stacks} entry is used by Trace Viewer.
 */
final class TraceStackSourceTrimmer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceStackSourceTrimmer.class);

    private static final String STACKS_ENTRY = "trace.stacks";

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
     * <p>Only {@code trace.stacks} is read into memory and replaced; all other entries (screenshots, snapshots,
     * network bodies, sources) are copied as-is, keeping their original compressed payloads. If the archive has
     * no {@code trace.stacks} entry (Playwright doesn't collect stacks without {@code PLAYWRIGHT_JAVA_SRC}), the
     * file is left untouched. Fails open: on any error the trace is left unchanged.</p>
     *
     * @param trace path to a Playwright trace zip, as produced by {@code Tracing.stop()}.
     */
    static void trim(final Path trace) {
        try (FileSystem zip = FileSystems.newFileSystem(trace)) {
            final Path stacks = zip.getPath(STACKS_ENTRY);
            if (Files.notExists(stacks)) {
                return;
            }
            Files.write(stacks, trimStacksJson(Files.readAllBytes(stacks)));
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not trim Playwright trace stacks, attaching the trace unchanged", e);
        }
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
}
