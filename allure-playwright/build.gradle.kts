description = "Allure Playwright Integration"

val agent: Configuration by configurations.creating

val playwrightVersion = "1.59.0"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    compileOnly("com.microsoft.playwright:playwright:$playwrightVersion")
    compileOnly("org.aspectj:aspectjrt")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("com.microsoft.playwright:playwright:$playwrightVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.playwright"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
