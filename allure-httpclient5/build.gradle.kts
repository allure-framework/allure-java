description = "Allure Apache HttpClient5 Integration"

dependencies {
    api(project(":allure-attachments"))
    implementation("org.apache.httpcomponents.client5:httpclient5")
    testImplementation("com.github.tomakehurst:wiremock")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
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
                "Automatic-Module-Name" to "io.qameta.allure.httpclient5"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}