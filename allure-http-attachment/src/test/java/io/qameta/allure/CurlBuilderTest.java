package io.qameta.allure;

import io.qameta.allure.http_attachment.CurlBuilder;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by vicdev on 13.05.17.
 */
public class CurlBuilderTest {

    @Test
    public void shouldValidateNullHeaders() {
        CurlBuilder curlBuilder = new CurlBuilder("GET", "http://localhost:8080");
        assertThat(curlBuilder.toString(), equalTo("curl -v -X GET 'http://localhost:8080' "));
    }

    @Test
    public void shouldGenerateCurl() {
        CurlBuilder curlBuilder = new CurlBuilder("GET", "http://localhost:8080").body("{}")
                .cookie("cookie", "123").header("header", "blah");
        assertThat(curlBuilder.toString(), equalTo("curl -v -X GET 'http://localhost:8080' -d '{}' " +
                "-b 'cookie=123' -H 'header: blah' "));

    }
}
