description = "Allure Ktorm Integration"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
}
apply(plugin = "kotlin")

val agent: Configuration by configurations.creating

val ktormVersion = "3.3.0"
val assertK = "0.23.1"
val h2 = "1.4.197"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-attachments"))

    implementation("org.ktorm:ktorm-core:$ktormVersion")

    testImplementation("com.h2database:h2:$h2")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertK")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.ktorm"
            )
        )
    }
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
