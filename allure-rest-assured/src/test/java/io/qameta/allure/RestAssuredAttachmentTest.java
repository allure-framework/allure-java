package io.qameta.allure;

import io.qameta.allure.restassured.AllureLoggerFilter;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.UnknownHostException;

import static io.restassured.RestAssured.given;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Created by vicdev on 13.05.17.
 */
public class RestAssuredAttachmentTest {

    private static final int PORT = 1080;
    private static final String URI = "http://localhost:" + PORT;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, PORT);
    private MockServerClient mockServerClient = new MockServerClient("localhost", PORT);

    private HttpRequest expectedRequest() throws UnknownHostException {
        return request()
                .withMethod("GET")
                .withPath("/simple")
                .withBody("{\"blah\": \"blah\", \"blah\":\"1111\"}")
                .withQueryStringParameter("param1", "1")
                .withQueryStringParameter("param2", "обана")
                .withCookie("RequestCookie", "requestValue");

    }

    private HttpResponse expectedResponse() throws UnknownHostException {
        return response().withStatusCode(200).withBody("{\"status\": \"ok\"}")
                .withCookie("ResponseCookie", "responseValue");

    }

    @Test
    public void shouldGenerateDefaultAttachment() throws UnknownHostException {
        HttpRequest expected = expectedRequest();
        mockServerClient.when(expected).respond(expectedResponse());
        given().filter(new AllureLoggerFilter()).baseUri(URI)
                .log().all().body("{\"blah\": \"blah\", \"blah\":\"1111\"}").queryParam("param1", "1")
                .queryParam("param2", "обана").cookie("RequestCookie",
                "requestValue").get("/simple");
    }

    @Test
    public void shouldGenerateCustomAttachment() throws UnknownHostException {
        HttpRequest expected = expectedRequest();
        mockServerClient.when(expected).respond(expectedResponse());
        given().filter(new AllureLoggerFilter().withTemplate("/templates/custom_report.ftl"))
                .baseUri(URI)
                .log().all().get("/simple");
    }
}
