description = "Allure Selenide Integration"

val agent: Configuration by configurations.creating

val selenideVersion = "5.20.4"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    implementation("com.codeborne:selenide:$selenideVersion")
    testImplementation("org.mockito:mockito-core")
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
                "Automatic-Module-Name" to "io.qameta.allure.selenide"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
