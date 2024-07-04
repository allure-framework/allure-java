description = "Allure JsonUnit Integration"

val jsonUnitVersion = "3.4.0"

dependencies {
    api(project(":allure-attachments"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("net.javacrumbs.json-unit:json-unit:$jsonUnitVersion")
    implementation("org.apache.commons:commons-lang3")
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
                "Automatic-Module-Name" to "io.qameta.allure.jsonunit"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}

