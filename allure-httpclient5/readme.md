## Allure-httpclient5
Extended logging for requests and responses with [httpclient5](https://mvnrepository.com/artifact/org.apache.httpcomponents.client5/httpclient5)
This library does not support `httpclient` due to package and API changes between `httpclient` and `httpclient5`.
To work with `httpclient`, it is recommended to use the `allure-httpclient` library.

## Wiki
https://hc.apache.org/httpcomponents-client-5.2.x/
https://hc.apache.org/httpcomponents-client-5.2.x/quickstart.html
https://hc.apache.org/httpcomponents-client-5.2.x/migration-guide/index.html
https://hc.apache.org/httpcomponents-client-5.2.x/examples.html

## Additional features
Implemented:
- The `httpclient5` library uses `gzip` compression by default. Interceptors attach message bodies in decompressed form
- `HttpEntityEnclosingRequest` is removed from `httpclient5`. Request interceptor works wo `HttpEntityEnclosingRequest`

Not tested:
- The httpclient5 library support Async interactions (Not tested)

## Examples

```java
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import io.qameta.allure.httpclient5.AllureHttpClient5Request;
import io.qameta.allure.httpclient5.AllureHttpClient5Response;

class Test {
    
    @Test
    void smokeGetShouldNotThrowThenReturnCorrectResponseMessage() throws IOException {
        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addRequestInterceptorLast(new AllureHttpClient5Request())
                .addResponseInterceptorFirst(new AllureHttpClient5Response());

        try (CloseableHttpClient httpClient = builder.build()) {
            final HttpGet httpGet = new HttpGet("/hello");
            httpClient.execute(httpGet, response -> {
                assertThat(EntityUtils.toString(response.getEntity())).isEqualTo(BODY_STRING);
                return response;
            });
        }
    }
}
```

In addition to using standard templates for formatting, you can use your custom `ftl` templates along the path 
`/resources/tpl/...`. For examples, you can use templates from the `allure-attachments` module.

```java
        final HttpClientBuilder builder = HttpClientBuilder.create()
                .addRequestInterceptorLast(new AllureHttpClient5Request("your-request-template-attachment.ftl"))
                .addResponseInterceptorFirst(new AllureHttpClient5Response"your-response-template-attachment.ftl"());
```