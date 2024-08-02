description = "Allure Apache HttpClient Integration"

val httpClient4Version = "4.5.14";

dependencies {
    api(project(":allure-attachments"))
    compileOnly("org.apache.httpcomponents:httpclient:$httpClient4Version")
    testImplementation("com.github.tomakehurst:wiremock")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.apache.httpcomponents:httpclient:$httpClient4Version")
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
                "Automatic-Module-Name" to "io.qameta.allure.httpclient"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
