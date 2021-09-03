description = "Allure Hamcrest Assertions Integration"

val agent: Configuration by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    compileOnly("org.aspectj:aspectjrt")
    implementation("org.hamcrest:hamcrest")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.hamcrest"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}