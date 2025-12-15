description = "Allure Rest-Assured Integration"

val restAssuredVersion = "5.5.6"

dependencies {
    api(project(":allure-attachments"))
    compileOnly("io.rest-assured:rest-assured:$restAssuredVersion")
    testImplementation("com.github.tomakehurst:wiremock")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.jboss.resteasy:resteasy-client")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.restassured"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
