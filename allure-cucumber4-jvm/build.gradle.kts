description = "Allure CucumberJVM 4.0"

val cucumberVersion = "7.10.0"

dependencies {
    api(project(":allure-java-commons"))
    implementation("io.cucumber:cucumber-core:$cucumberVersion")
    implementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("commons-io:commons-io")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.cucumber4jvm"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
