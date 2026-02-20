/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for generating diffs between two strings and attaching them to Allure reports.
 * <p>
 * Provides a visual comparison of differences between original and revised text content,
 * highlighting additions, removals, and unchanged lines.
 * </p>
 */
public class AllureDiff {

    private String diffTemplatePath = "diff-template.ftl";

    /**
     * Set allure template file path. Default is 'diff-template.ftl' in 'resources/tpl/diff-template.ftl'.
     *
     * @param diffTemplatePath what the path to ftl template
     */
    public void setDiffTemplatePath(final String diffTemplatePath) {
        this.diffTemplatePath = diffTemplatePath;
    }

    /**
     * Computes the differences between two strings and generates an Allure report attachment.
     *
     * @param original The original text content
     * @param revised  The revised text content
     * @return Diff result containing both the patch and Allure attachment
     * @throws IllegalArgumentException if there's an error processing the diff
     */
    public Patch<String> diff(final String original, final String revised) {
        final List<String> originalLines = splitInputString(original);
        final List<String> revisedLines = splitInputString(revised);

        final Patch<String> patch = DiffUtils.diff(originalLines, revisedLines);
        final List<AbstractDelta<String>> deltas = patch.getDeltas();

        final List<AllureDiffLine> diffLines = new ArrayList<>();
        int origLine = 1;
        int revLine = 1;
        int deltaIndex = 0;

        while (origLine <= originalLines.size() || revLine <= revisedLines.size()) {
            final AbstractDelta<String> currentDelta = deltaIndex < deltas.size() ? deltas.get(deltaIndex) : null;

            if (currentDelta != null && isDeltaPosition(currentDelta, origLine, revLine)) {
                // Process deleted lines
                for (String line : currentDelta.getSource().getLines()) {
                    diffLines.add(new AllureDiffLine(line, AllureDiffType.REMOVED, origLine++, -1));
                }
                // Process inserted lines
                for (String line : currentDelta.getTarget().getLines()) {
                    diffLines.add(new AllureDiffLine(line, AllureDiffType.ADDED, -1, revLine++));
                }
                origLine += currentDelta.getSource().size();
                revLine += currentDelta.getTarget().size();
                deltaIndex++;
            } else {
                processUnchangedOrDivergedLines(originalLines, revisedLines, diffLines, origLine, revLine);
                origLine++;
                revLine++;
            }
        }

        final AttachmentProcessor<AttachmentData> processor = new DefaultAttachmentProcessor();
        processor.addAttachment(new AllureDiffModel(diffLines), new FreemarkerAttachmentRenderer(diffTemplatePath));

        return patch;
    }

    /**
     * Split the input string to lines.
     *
     * @param input any String
     * @return list of lines from input string
     */
    private static List<String> splitInputString(final String input) {
        return Arrays.asList((input == null ? "" : input).split("\\R"));
    }

    /**
     * Checks if the current position matches the delta's expected position.
     *
     * @param delta    The delta to check
     * @param origLine Current original line number
     * @param revLine  Current revised line number
     * @return true if the current position matches the delta's position
     */
    private static boolean isDeltaPosition(final AbstractDelta<String> delta,
                                           final int origLine,
                                           final int revLine) {
        return delta.getSource().getPosition() + 1 == origLine
                && delta.getTarget().getPosition() + 1 == revLine;
    }

    /**
     * Processes lines that are either unchanged or diverged (unexpected differences).
     *
     * @param originalLines Original text lines
     * @param revisedLines  Revised text lines
     * @param diffLines     List to accumulate diff lines
     * @param origLine      Current original line number
     * @param revLine       Current revised line number
     */
    private static void processUnchangedOrDivergedLines(final List<String> originalLines,
                                                        final List<String> revisedLines,
                                                        final List<AllureDiffLine> diffLines,
                                                        final int origLine,
                                                        final int revLine) {
        final String orig = getLine(originalLines, origLine);
        final String rev = getLine(revisedLines, revLine);
        if (orig != null && orig.equals(rev)) {
            diffLines.add(new AllureDiffLine(orig, AllureDiffType.UNCHANGED, origLine, revLine));
        } else {
            if (orig != null) {
                diffLines.add(new AllureDiffLine(orig, AllureDiffType.REMOVED, origLine, -1));
            }
            if (rev != null) {
                diffLines.add(new AllureDiffLine(rev, AllureDiffType.ADDED, -1, revLine));
            }
        }
    }

    /**
     * Safely retrieves a line from the text content.
     *
     * @param lines      List of text lines
     * @param lineNumber 1-based line number to retrieve
     * @return The line content or null if out of bounds
     */
    private static String getLine(final List<String> lines, final int lineNumber) {
        return lineNumber <= lines.size() ? lines.get(lineNumber - 1) : null;
    }

    /**
     * Represents the type of difference for a line in the comparison.
     */
    public enum AllureDiffType {
        /**
         * Line was added in the revised text.
         */
        ADDED,
        /**
         * Line was removed from the original text.
         */
        REMOVED,
        /**
         * Line is unchanged between versions.
         */
        UNCHANGED
    }

    /**
     * Represents a single line in the diff comparison.
     */
    public static class AllureDiffLine {
        private final String text;
        private final AllureDiff.AllureDiffType type;
        private final int originalLine;
        private final int revisedLine;

        /**
         * Represents a single line in the diff comparison.
         *
         * @param text         The text content of the line
         * @param type         The type of difference (ADDED/REMOVED/UNCHANGED)
         * @param originalLine Original line number (-1 if line was added)
         * @param revisedLine  Revised line number (-1 if line was removed)
         */
        public AllureDiffLine(final String text,
                              final AllureDiffType type,
                              final int originalLine,
                              final int revisedLine) {
            this.text = text;
            this.type = type;
            this.originalLine = originalLine;
            this.revisedLine = revisedLine;
        }

        /**
         * Get the text content of the line. Using only in FTL.
         *
         * @return what actually String contains
         */
        public String text() {
            return text;
        }

        /**
         * Get the type of difference (ADDED/REMOVED/UNCHANGED). Using only in FTL.
         *
         * @return what diff type String contatins
         */
        public AllureDiffType type() {
            return type;
        }

        /**
         * Get original line number (-1 if line was added). Using only in FTL.
         *
         * @return what the original line number
         */
        public int originalLine() {
            return originalLine;
        }

        /**
         * Get revised line number (-1 if line was removed). Using only in FTL.
         *
         * @return what the revised line number
         */
        public int revisedLine() {
            return revisedLine;
        }
    }

    /**
     * Data model for the Allure attachment containing diff results. Using only in FTL.
     */
    public static class AllureDiffModel implements AttachmentData {
        private final List<AllureDiffLine> rows;

        /**
         * Default constructor for data model for the Allure attachment containing diff results.
         *
         * @param rows List of diff lines to display in the report
         */
        public AllureDiffModel(final List<AllureDiffLine> rows) {
            this.rows = rows;
        }

        /**
         * Getting all rows with inner data model of diff representation. Using only in FTL.
         *
         * @return List of AllureDiffLine.
         */
        public List<AllureDiffLine> rows() {
            return rows;
        }

        /**
         * Get attachment name.
         *
         * @return attachment name
         */
        @Override
        public String getName() {
            return "Diff";
        }
    }
}
