description = "Allure OpenFeign Integration"

dependencies {
    implementation("io.github.openfeign:feign-core:13.6")
    testImplementation("io.github.openfeign:feign-gson:13.6")
    api(project(":allure-attachments"))
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
                "Automatic-Module-Name" to "io.qameta.allure.openfeign"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
