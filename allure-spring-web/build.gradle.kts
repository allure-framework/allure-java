description = "Allure Spring Web Integration"

val springWebVersion = "7.0.8"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("org.springframework:spring-web:$springWebVersion")
    testImplementation("org.wiremock:wiremock")
    testImplementation("org.assertj:assertj-core")
    testImplementation(project(":allure-assertj"))
    testImplementation("org.jboss.resteasy:resteasy-client")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.springframework:spring-web:$springWebVersion")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.springweb"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    options.release.set(17)
}
