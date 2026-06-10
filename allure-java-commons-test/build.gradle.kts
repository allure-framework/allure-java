description = "Allure Java Commons Test Utils"

dependencies {
    api("commons-io:commons-io")
    api("io.github.benas:random-beans")
    api("org.apache.commons:commons-lang3")
    api(project(":allure-java-commons"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.commonstest"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
