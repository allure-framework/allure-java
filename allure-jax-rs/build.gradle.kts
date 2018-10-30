description = "Allure JAX-RS Filter Integration"

dependencies {
    compile(project(":allure-attachments"))
    compile("javax.ws.rs:javax.ws.rs-api")
    testCompile("junit:junit")
    testCompile("org.slf4j:slf4j-simple")
    testCompile("org.assertj:assertj-core")
    testCompile("org.mockito:mockito-core")
    testCompile("com.github.tomakehurst:wiremock")
    testCompile("org.jboss.resteasy:resteasy-client")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.jaxrs"
        ))
    }
}
