description = "Allure Model Reader Module"

dependencies {
    api(project(":allure-model"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.reader"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
