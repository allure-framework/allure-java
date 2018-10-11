package io.qameta.allure.jsonunit;

import com.google.common.base.Charsets;
import org.apache.tika.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonPatchListenerTest {

    @Test
    void shouldSeeEmptyDiffNodes() {
        assertThat("{}", jsonEquals("{}"));
    }

    @Test
    void shouldSeeAddedNode() {
        assertThrows(AssertionError.class, () -> assertThat("{\"test\": \"1\"}", jsonEquals("{}")));

    }

    @Test
    void shouldSeeRemovedNode() {
        assertThrows(AssertionError.class, () -> assertThat("{}", jsonEquals("{\"test\": \"1\"}")));
    }

    @Test
    void shouldSeeEmptyForCheckAnyNode() {
        assertThat("{\"test\":\"1\"}", jsonEquals("{\"test\": \"${json-unit.ignore}\"}"));
    }

    @Test
    void shouldSeeEmptyForCheckAnyBooleanNode() {
        assertThat("{\"test\": true}", jsonEquals("{\"test\": \"${json-unit.any-boolean}\"}"));
    }

    @Test
    void shouldSeeEmptyForCheckAnyNumberNode() {
        assertThat("{\"test\": 11}", jsonEquals("{\"test\": \"${json-unit.any-number}\"}"));
    }

    @Test
    void shouldSeeEmptyForCheckAnyStringNode() {
        assertThat("{\"test\": \"1\"}", jsonEquals("{\"test\": \"${json-unit.any-string}\"}"));
    }


    @Test
    void shouldSeeChangedStringNode() {
        assertThrows(AssertionError.class, () -> assertThat("{\"test\": \"2\"}",
                jsonEquals("{\"test\": \"1\"}")));
    }

    @Test
    void shouldSeeChangedNumberNode() {
        assertThrows(AssertionError.class, () -> assertThat("{\"test\": 2 }",
                jsonEquals("{\"test\": 1}")));

    }

    @Test
    void shouldSeeChangedBooleanNode() {
        assertThrows(AssertionError.class, () -> assertThat("{\"test\": false}",
                jsonEquals("{\"test\": true}")));
    }

    @Test
    void shouldSeeChangedStructureNode() {
        assertThrows(AssertionError.class, () -> assertThat("{\"test\": false}",
                jsonEquals("{\"test\": \"1\"}")));
    }

    @Test
    void shouldSeeChangedArrayNode() {
        assertThrows(AssertionError.class, () -> assertThat("[1, 2]", jsonEquals("[1, 1]")));
    }

    @Test
    void shouldSeeRemovedArrayNode() {
        assertThrows(AssertionError.class, () -> assertThat("[1]", jsonEquals("[1, 2]")));
    }

    @Test
    void shouldSeeAddedArrayNode() {
        assertThrows(AssertionError.class, () -> assertThat("[1, 2]", jsonEquals("[1]")));
    }

    @Test
    void shouldSeeObjectDiffNodes() {
        assertThrows(AssertionError.class, () -> assertThat("{\"test\": { \"test1\": \"2\"} }",
                jsonEquals("{\"test\": { \"test1\": \"1\"}}")));
    }

    @Test
    void shouldSeeNullNode() {
        assertThat(null, jsonEquals(null));
    }

    @Test
    void shouldWorkWhenIgnoringArrayOrder() {
        assertThrows(AssertionError.class, () -> assertThat("{\"test\":[[4,2],[1,2]]}",
                jsonEquals("{\"test\": [[1,2],[2,3]]}")));
    }

    @Test
    void shouldSeeActualSource() {
        assertThrows(AssertionError.class, () -> assertThat("{}", jsonEquals("{\"test\": \"1\"}")));
    }

    @Test
    void shouldSeeExpectedSource() {
        assertThrows(AssertionError.class, () -> assertThat("{}", jsonEquals("{\"test\": \"1\"}")));
    }

    @Test
    void shouldSeeArrayChangeToNode() {
        assertThrows(AssertionError.class, () -> assertThat("{\"test\":\"1\"}",
                jsonEquals("[[1,2],[2,3],[1,1]]")));
    }

    @Test
    void shouldSeeChangeNodeToArray() {
        assertThrows(AssertionError.class, () -> assertThat("[[1,2],[2,3],[1,1]]",
                jsonEquals("{\"test\":\"1\"}")));
    }

    @Test
    void shouldSeeDifference() throws IOException {
        assertThrows(AssertionError.class, () -> assertThat(getResourceAsString("left.json"), jsonEquals(getResourceAsString("right.json"))));
    }

    private String getResourceAsString(String path) throws IOException {
        return IOUtils.toString(JsonPatchListenerTest.class.getClassLoader().getResourceAsStream(path),
                Charsets.UTF_8.name());
    }

}