package io.qameta.allure.jsonunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Diff;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class JsonPatchListenerTest {

    private final JsonPatchListener listener = new JsonPatchListener();

    @Test
    public void shouldSeeEmptyDiffNodes() {
        Diff diff = Diff.create("{}", "{}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getDifferences(), hasSize(0));
    }

    @Test
    public void shouldSeeRemovedNode() {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"test\":[\"1\",0,0]}"));
    }

    @Test
    public void shouldSeeAddedNode() {
        Diff diff = Diff.create("{}", "{\"test\": \"1\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"test\":[\"1\"]}"));
    }

    @Test
    public void shouldSeeEmptyForCheckAnyNode() {
        Diff diff = Diff.create("{\"test\": \"${json-unit.ignore}\"}", "{\"test\":\"1\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{}"));
    }

    @Test
    public void shouldSeeEmptyForCheckAnyBooleanNode() {
        Diff diff = Diff.create("{\"test\": \"${json-unit.any-boolean}\"}", "{\"test\": true}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{}"));
    }

    @Test
    public void shouldSeeEmptyForCheckAnyNumberNode() {
        Diff diff = Diff.create("{\"test\": \"${json-unit.any-number}\"}", "{\"test\": 11}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{}"));

    }

    @Test
    public void shouldSeeEmptyForCheckAnyStringNode() {
        Diff diff = Diff.create("{\"test\": \"${json-unit.any-string}\"}", "{\"test\": \"1\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{}"));
    }


    @Test
    public void shouldSeeChangedStringNode() {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{\"test\": \"2\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"test\":[\"1\",\"2\"]}"));
    }

    @Test
    public void shouldSeeChangedNumberNode() {
        Diff diff = Diff.create("{\"test\": 1}", "{\"test\": 2 }", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"test\":[1,2]}"));

    }

    @Test
    public void shouldSeeChangedBooleanNode() {
        Diff diff = Diff.create("{\"test\": true}", "{\"test\": false}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"test\":[true,false]}"));
    }

    @Test
    public void shouldSeeChangedStructureNode() {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{\"test\": false}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"test\":[\"1\",false]}"));
    }

    @Test
    public void shouldSeeChangedArrayNode() {
        Diff diff = Diff.create("[1, 1]", "[1, 2]", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"1\":[1,2],\"_t\":\"a\"}"));
    }

    @Test
    public void shouldSeeRemovedArrayNode() {
        Diff diff = Diff.create("[1, 2]", "[1]", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"1\":[2,0,0],\"_t\":\"a\"}"));
    }

    @Test
    public void shouldSeeAddedArrayNode() {
        Diff diff = Diff.create("[1]", "[1, 2]", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"1\":[2],\"_t\":\"a\"}"));
    }

    @Test
    public void shouldSeeObjectDiffNodes() {
        Diff diff = Diff.create("{\"test\": { \"test1\": \"1\"}}", "{\"test\": { \"test1\": \"2\"} }", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{\"test\":{\"test1\":[\"1\",\"2\"]}}"));
    }

    @Test
    public void shouldSeeNullNode() {
        Diff diff = Diff.create(null, null, "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("{}"));
    }

    @Test
    public void shouldWorkWhenIgnoringArrayOrder() {
        Diff diff = Diff.create("{\"test\": [[1,2],[2,3]]}", "{\"test\":[[4,2],[1,2]]}", "", "", commonConfig().when(Option.IGNORING_ARRAY_ORDER));
        diff.similar();
        assertThat(listener.getJsonPatch(),
                equalTo("{\"test\":{\"0\":{\"0\":[3,4],\"_t\":\"a\"},\"_t\":\"a\"}}"));
    }

    @Test
    public void shouldSeeActualSource() throws JsonProcessingException {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{}", "", "", commonConfig());
        diff.similar();
        assertThat(new ObjectMapper().writeValueAsString(listener.getContext().getActualSource()), equalTo("{}"));
    }

    @Test
    public void shouldSeeExpectedSource() throws JsonProcessingException {
        Diff diff = Diff.create("{\"test\": \"1\"}", "{}", "", "", commonConfig());
        diff.similar();
        assertThat(new ObjectMapper().writeValueAsString(listener.getContext().getExpectedSource()), equalTo("{\"test\":\"1\"}"));
    }

    @Test
    public void shouldSeeNodeChangeToArray() {
        Diff diff = Diff.create("{\"test\": \"1\"}", "[[1,2],[2,3],[1,1]]", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("[{\"test\":\"1\"},[[1,2],[2,3],[1,1]]]"));
    }

    @Test
    public void shouldArrayChangeToNode() {
        Diff diff = Diff.create("[[1,2],[2,3],[1,1]]", "{\"test\": \"1\"}", "", "", commonConfig());
        diff.similar();
        assertThat(listener.getJsonPatch(), equalTo("[[[1,2],[2,3],[1,1]],{\"test\":\"1\"}]"));
    }

    private Configuration commonConfig() {
        return Configuration.empty().withDifferenceListener(listener);
    }
}
