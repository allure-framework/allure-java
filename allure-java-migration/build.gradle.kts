description = "Allure Java Migration Utils"

val agent: Configuration by configurations.creating

val junitVersion = "4.13.2"
val testNgVersion = "7.4.0"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api("org.apache.commons:commons-lang3")
    api("org.aspectj:aspectjrt")
    api(project(":allure-java-commons"))
    implementation("junit:junit:$junitVersion")
    implementation("org.testng:testng:$testNgVersion")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.testng:testng:$testNgVersion")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.migration"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
