description = "Allure JAX-RS Filter Integration"

val javaxWsRsApiVersion = "2.1.1"

dependencies {
    api(project(":allure-attachments"))
    implementation("javax.ws.rs:javax.ws.rs-api:$javaxWsRsApiVersion")
    testImplementation("com.github.tomakehurst:wiremock")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.jboss.resteasy:resteasy-client")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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
