description = "Allure Rest-Assured Integration"

val agent by configurations.creating

val restAssuredVersion = "3.1.0"

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile(project(":allure-attachments"))
    compile("io.rest-assured:rest-assured:$restAssuredVersion")

    testCompile("com.github.tomakehurst:wiremock")
    testCompile("org.jboss.resteasy:resteasy-client")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.junit.jupiter:junit-jupiter-params")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.restassured"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
