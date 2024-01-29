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
package io.qameta.allure.jsonunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Diff;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonPatchListenerTest {

    private final JsonPatchListener listener = new JsonPatchListener();

    @Test
    void shouldSeeEmptyDiffNodes() {
        Diff diff = Diff.create("{}", "{}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getDifferences())
                .isEmpty();
    }

    @Test
    void shouldSeeRemovedNode() {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch())
                .isEqualTo("{\"test\":[\"1\",0,0]}");
    }

    @Test
    void shouldSeeAddedNode() {
        Diff diff = Diff.create("{}", "{\"test\": \"1\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{\"test\":[\"1\"]}");
    }

    @Test
    void shouldSeeEmptyForCheckAnyNode() {
        Diff diff = Diff.create("{\"test\": \"${json-unit.ignore}\"}", "{\"test\":\"1\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{}");
    }

    @Test
    void shouldSeeEmptyForCheckAnyBooleanNode() {
        Diff diff = Diff.create("{\"test\": \"${json-unit.any-boolean}\"}", "{\"test\": true}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{}");
    }

    @Test
    void shouldSeeEmptyForCheckAnyNumberNode() {
        Diff diff = Diff.create("{\"test\": \"${json-unit.any-number}\"}", "{\"test\": 11}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{}");

    }

    @Test
    void shouldSeeEmptyForCheckAnyStringNode() {
        Diff diff = Diff.create("{\"test\": \"${json-unit.any-string}\"}", "{\"test\": \"1\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{}");
    }


    @Test
    void shouldSeeChangedStringNode() {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{\"test\": \"2\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{\"test\":[\"1\",\"2\"]}");
    }

    @Test
    void shouldSeeChangedNumberNode() {
        Diff diff = Diff.create("{\"test\": 1}", "{\"test\": 2 }", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{\"test\":[1,2]}");

    }

    @Test
    void shouldSeeChangedBooleanNode() {
        Diff diff = Diff.create("{\"test\": true}", "{\"test\": false}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{\"test\":[true,false]}");
    }

    @Test
    void shouldSeeChangedStructureNode() {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{\"test\": false}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{\"test\":[\"1\",false]}");
    }

    @Test
    void shouldSeeChangedArrayNode() {
        Diff diff = Diff.create("[1, 1]", "[1, 2]", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{\"1\":[1,2],\"_t\":\"a\"}");
    }

    @Test
    void shouldSeeRemovedArrayNode() {
        Diff diff = Diff.create("[1, 2]", "[1]", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{\"1\":[2,0,0],\"_t\":\"a\"}");
    }

    @Test
    void shouldSeeAddedArrayNode() {
        Diff diff = Diff.create("[1]", "[1, 2]", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{\"1\":[2],\"_t\":\"a\"}");
    }

    @Test
    void shouldSeeObjectDiffNodes() {
        Diff diff = Diff.create("{\"test\": { \"test1\": \"1\"}}", "{\"test\": { \"test1\": \"2\"} }", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{\"test\":{\"test1\":[\"1\",\"2\"]}}");
    }

    @Test
    void shouldSeeNullNode() {
        Diff diff = Diff.create(null, null, "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("{}");
    }

    @Test
    void shouldWorkWhenIgnoringArrayOrder() {
        Diff diff = Diff.create("{\"test\": [[1,2],[2,3]]}", "{\"test\":[[4,2],[1,2]]}", "", "", commonConfig().when(Option.IGNORING_ARRAY_ORDER));
        diff.similar();
        assertThat(listener.getJsonPatch()).
                isEqualTo("{\"test\":{\"0\":{\"0\":[3,4],\"_t\":\"a\"},\"_t\":\"a\"}}");
    }

    @Test
    void shouldSeeActualSource() throws JsonProcessingException {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{}", "", "", commonConfig());
        diff.similar();
        assertThat(new ObjectMapper().writeValueAsString(listener.getContext().getActualSource())).isEqualTo("{}");
    }

    @Test
    void shouldSeeExpectedSource() throws JsonProcessingException {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{}", "", "", commonConfig());
        diff.similar();
        assertThat(new ObjectMapper().writeValueAsString(listener.getContext().getExpectedSource())).isEqualTo("{\"test\":\"1\"}");
    }

    @Test
    void shouldSeeNodeChangeToArray() {
        Diff diff = Diff.create("{\"test\": \"1\"}", "[[1,2],[2,3],[1,1]]", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("[{\"test\":\"1\"},[[1,2],[2,3],[1,1]]]");
    }

    @Test
    void shouldArrayChangeToNode() {
        Diff diff = Diff.create("[[1,2],[2,3],[1,1]]", "{\"test\": \"1\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch()).isEqualTo("[[[1,2],[2,3],[1,1]],{\"test\":\"1\"}]");
    }

    @Test
    void shouldSeeDiffModel() {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{}", "", "", commonConfig());
        diff.similar();
        DiffModel model = listener.getDiffModel();
        assertThat(model.getExpected()).isEqualTo("{\"test\":\"1\"}");
        assertThat(model.getActual()).isEqualTo("{}");
        assertThat(model.getPatch()).isEqualTo("{\"test\":[\"1\",0,0]}");
    }

    private Configuration commonConfig() {
        return Configuration.empty().withDifferenceListener(listener);
    }
}
