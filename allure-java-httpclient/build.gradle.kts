description = "Allure Java HTTP Client Integration"

dependencies {
    api(project(":allure-java-commons"))
    testImplementation("org.wiremock:wiremock")
    testImplementation("org.assertj:assertj-core")
    testImplementation(project(":allure-assertj"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.javahttpclient"
            )
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
