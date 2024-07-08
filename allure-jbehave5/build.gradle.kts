description = "Allure JBehave 5 Integration"

val jbehaveVersion = "5.1.1"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("org.jbehave:jbehave-core:$jbehaveVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.jbehave:jbehave-core:$jbehaveVersion")
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
                "Automatic-Module-Name" to "io.qameta.allure.jbehave5"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
