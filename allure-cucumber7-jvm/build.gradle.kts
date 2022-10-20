description = "Allure CucumberJVM 7.0"

val cucumberVersion = "7.8.1"
val cucumberGherkinVersion = "24.0.0"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("io.cucumber:cucumber-plugin:$cucumberVersion")
    implementation("io.cucumber:gherkin:$cucumberGherkinVersion")
    testImplementation("io.cucumber:gherkin:$cucumberGherkinVersion")
    testImplementation("io.cucumber:cucumber-core:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("commons-io:commons-io")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.cucumber7jvm"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
