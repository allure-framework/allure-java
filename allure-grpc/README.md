Description
----
GRPC client interceptor for attaching request, response, headers and integration status to the Allure report.

The example is based on the service, which is declared in the proto file as
----

```protobuf
syntax = "proto3";

package io.qameta.allure.grpc;

option java_multiple_files = true;
option java_package = "io.qameta.allure.grpc";

service Greet {
  rpc GreetMe (SimpleRequest) returns (SimpleResponse) {}
}

message SimpleRequest {
  string body = 1;
}

message SimpleResponse {
  string body = 1;
}
```


How to use with pure java grpc-api
----

Further, after generating the code from the protobuf file, you need to create a client using a stub. An additional
interceptor should be added here. After that, the interceptor will be active. This example does not consider the
creation and configuration of the messageChannel or the generation of code from the protobuf file.

```java
public class GrpcClientTest {

    val stub = io.qameta.allure.grpc.GreetGrpc.newBlockingStub(channel)
            .withInterceptors(new AllureGrpcClientInterceptor());

    @Test
    void test() {
        val simpleResponse = stub.greetMe(SimpleRequest.builder().body("Say hi").build());
    }
}
```

How to use with spring-boot-starter
----
https://github.com/yidongnan/grpc-spring-boot-starter

According to the documentation https://yidongnan.github.io/grpc-spring-boot-starter/en/client/configuration.html
in the configuration section, there are different methods for connecting a client interceptor. Still, I advise to
add it to your spring-boot config class as

```java
@Configuration
public class YourTestConfiguration {

    @Bean
    GlobalClientInterceptorConfigurer globalClientInterceptorConfigurer() {
        interceptors -> interceptors.add(new AllureGrpcClientInterceptor());
    }
}
```

After adding this configuration to @SpringBootTest or to the context in some other way, the interceptor will be active.
