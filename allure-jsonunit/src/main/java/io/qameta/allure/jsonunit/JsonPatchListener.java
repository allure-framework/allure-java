package io.qameta.allure.jsonunit;

import com.google.gson.GsonBuilder;
import net.javacrumbs.jsonunit.core.listener.Difference;
import net.javacrumbs.jsonunit.core.listener.DifferenceContext;
import net.javacrumbs.jsonunit.core.listener.DifferenceListener;
import org.apache.commons.lang3.StringUtils;

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

    @SuppressWarnings("ReturnCount")
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

    @SuppressWarnings({"all", "unchecked"})
    public String getJsonPatch() {
        final Map<String, Object> jsonDiffPatch = new HashMap<>();
        // take care of corner case when two jsons absolutelly different
        if (getDifferences().size() == 1) {
            final Difference difference = getDifferences().get(0);
            final String field = getPath(difference);
            if (field.isEmpty()) {
                return new GsonBuilder().create().toJson(getPatch(difference));
            }
        }
        getDifferences().forEach(difference -> {

            final String field = getPath(difference);
            Map<String, Object> currentMap = jsonDiffPatch;

            final String fieldWithDots = StringUtils.replace(field, "[", ".");
            final int len = fieldWithDots.length();
            int left = 0;
            int right = 0;
            while (left < len) {
                right = fieldWithDots.indexOf('.', left);
                if (right == -1) {
                    right = len;
                }
                String fieldName = fieldWithDots.substring(left, right);
                fieldName = StringUtils.remove(fieldName, "]");

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

        return new GsonBuilder().create().toJson(jsonDiffPatch);
    }
}
