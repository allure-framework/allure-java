package io.qameta.allure;

import io.qameta.allure.client.ApiClient;
import io.qameta.allure.client.ApiInterface;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.net.UnknownHostException;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Created by vicdev on 13.05.17.
 */
public class RetrofitAttachmentTest {

    private static final int PORT = 1080;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, PORT);
    private MockServerClient mockServerClient = new MockServerClient("localhost", PORT);

    private HttpRequest expectedRequest() throws UnknownHostException {
        return request()
                .withMethod("GET")
                .withPath("/simple");

    }

    private HttpResponse expectedResponse() throws UnknownHostException {
        return response().withStatusCode(200).withBody("{\"status\": \"ok\"}");

    }

    @Test
    public void shouldGenerateAttachment() throws IOException {
        HttpRequest expected = expectedRequest();
        mockServerClient.when(expected).respond(expectedResponse());
        ApiClient.getClient().create(ApiInterface.class).simple().execute();
    }
}
