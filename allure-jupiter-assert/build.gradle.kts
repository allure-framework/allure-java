description = "Allure Jupiter Assertions Integration"

dependencies {
    api(project(":allure-jupiter"))
    compileOnly("org.aspectj:aspectjrt")
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation(project(":allure-assertj"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.jupiterassert"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
