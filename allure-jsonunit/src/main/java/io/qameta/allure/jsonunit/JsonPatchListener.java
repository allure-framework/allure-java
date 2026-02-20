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
package io.qameta.allure.jsonunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.core.listener.Difference;
import net.javacrumbs.jsonunit.core.listener.DifferenceContext;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JsonUnit listener, that keeps difference and
 * return formatted json to represent deltas
 * (i.e. the output of jsondiffpatch.diff).
 */
public class JsonPatchListener implements DifferenceListener {

    private static final String UNKNOWN_TYPE_ERROR = "Difference has unknown type";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final List<Difference> differences = new ArrayList<>();

    private DifferenceContext context;

    @Override
    public void diff(final Difference difference, final DifferenceContext differenceContext) {
        this.context = differenceContext;
        differences.add(difference);
    }

    public List<Difference> getDifferences() {
        return differences;
    }

    public DifferenceContext getContext() {
        return context;
    }

    private String getPath(final Difference difference) {
        switch (difference.getType()) {
            case DIFFERENT:
                return difference.getActualPath();

            case MISSING:
                return difference.getExpectedPath();

            case EXTRA:
                return difference.getActualPath();

            default:
                throw new IllegalArgumentException(UNKNOWN_TYPE_ERROR);
        }
    }

    @SuppressWarnings("ReturnCount")
    private List<Object> getPatch(final Difference difference) {
        final List<Object> result = new ArrayList<>();

        switch (difference.getType()) {
            case DIFFERENT:
                result.add(difference.getExpected());
                result.add(difference.getActual());
                return result;

            case MISSING:
                result.add(difference.getExpected());
                result.add(0);
                result.add(0);
                return result;

            case EXTRA:
                result.add(difference.getActual());
                return result;

            default:
                throw new IllegalArgumentException(UNKNOWN_TYPE_ERROR);
        }
    }

    public DiffModel getDiffModel() {
        return new DiffModel(
                writeAsString(context.getActualSource(), "actual"),
                writeAsString(context.getExpectedSource(), "expected"),
                getJsonPatch());
    }

    @SuppressWarnings({"all", "unchecked"})
    public String getJsonPatch() {
        final Map<String, Object> jsonDiffPatch = new HashMap<>();
        // take care of corner case when two jsons absolutelly different
        if (getDifferences().size() == 1) {
            final Difference difference = getDifferences().get(0);
            final String field = getPath(difference);
            if (field.isEmpty()) {
                final ObjectMapper mapper = new ObjectMapper();
                try {
                    return mapper.writeValueAsString(getPatch(difference));
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Could not process patch json", e);
                }
            }
        }
        getDifferences().forEach(difference -> {

            final String field = getPath(difference);
            Map<String, Object> currentMap = jsonDiffPatch;

            final String fieldWithDots = field.replace('[', '.');
            final int len = fieldWithDots.length();
            int left = 0;
            int right = 0;
            while (left < len) {
                right = fieldWithDots.indexOf('.', left);
                if (right == -1) {
                    right = len;
                }
                String fieldName = fieldWithDots.substring(left, right);
                fieldName = fieldName.replaceAll("]", "");

                if (right != len) {
                    if (!fieldName.isEmpty()) {
                        if (!currentMap.containsKey(fieldName)) {
                            currentMap.put(fieldName, new HashMap<>());
                        }
                        currentMap = (Map<String, Object>) currentMap.get(fieldName);
                    }

                    if (field.charAt(right) == '[') {
                        if (!currentMap.containsKey(fieldName)) {
                            currentMap.put("_t", "a");
                        }
                    }
                } else {
                    final List<?> actualExpectedValue = getPatch(difference);
                    currentMap.put(fieldName, actualExpectedValue);
                }
                left = right + 1;
            }
        });
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(jsonDiffPatch);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not process patch json", e);
        }
    }

    private static String writeAsString(final Object object, final String failDescription) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(String.format("Could not process %s json", failDescription), e);
        }
    }
}
