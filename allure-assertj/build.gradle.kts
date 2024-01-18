description = "Allure AssertJ Integration"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("org.aspectj:aspectjrt")
    compileOnly("org.assertj:assertj-core")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.vintage:junit-vintage-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.assertj"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
