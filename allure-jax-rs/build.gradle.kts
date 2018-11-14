description = "Allure JAX-RS Filter Integration"

val agent by configurations.creating

val javaxWsRsApiVersion = "2.0.1"

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile(project(":allure-attachments"))
    compile("javax.ws.rs:javax.ws.rs-api:$javaxWsRsApiVersion")
    testCompile("com.github.tomakehurst:wiremock")
    testCompile("org.jboss.resteasy:resteasy-client")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.jaxrs"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
