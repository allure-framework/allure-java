description = "Allure Javadoc Descriptions"

val agent: Configuration by configurations.creating

dependencies {
    api("commons-io:commons-io")
    api(project(":allure-java-commons"))
    testImplementation("com.google.testing.compile:compile-testing")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.description"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
