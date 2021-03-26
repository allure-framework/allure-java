description = "Allure CucumberJVM Integration"

val agent: Configuration by configurations.creating

val cucumberVersion = "1.2.6"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    implementation("info.cukes:cucumber-core:$cucumberVersion")
    implementation("info.cukes:cucumber-java:$cucumberVersion")
    implementation("info.cukes:gherkin:2.12.2")
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
                "Automatic-Module-Name" to "io.qameta.allure.cucumberjvm"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
