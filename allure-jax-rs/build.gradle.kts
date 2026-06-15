description = "Allure JAX-RS Filter Integration"

val jakartaWsRsApiVersion = "4.0.0"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:$jakartaWsRsApiVersion")
    testImplementation("jakarta.ws.rs:jakarta.ws.rs-api:$jakartaWsRsApiVersion")
    testImplementation("org.wiremock:wiremock")
    testImplementation("org.assertj:assertj-core")
    testImplementation(project(":allure-assertj"))
    testImplementation("org.jboss.resteasy:resteasy-client")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.jaxrs"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
