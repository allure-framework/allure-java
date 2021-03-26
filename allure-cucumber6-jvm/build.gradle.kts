description = "Allure CucumberJVM 6.0"

val agent: Configuration by configurations.creating

val cucumberVersion = "6.10.2"
val cucumberGherkinVersion = "18.0.0"

dependencies {
    agent("org.aspectj:aspectjweaver")
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
                "Automatic-Module-Name" to "io.qameta.allure.cucumber6jvm"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
