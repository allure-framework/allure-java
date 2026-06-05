description = "Allure Selenium WebDriver BiDi Integration"

val agent: Configuration by configurations.creating

val seleniumVersion = "4.23.0"
val testcontainersVersion = "1.21.4"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    compileOnly("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
    testImplementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks {
    compileJava {
        options.release.set(17)
    }
    compileTestJava {
        options.release.set(17)
    }
    jar {
        manifest {
            attributes(
                mapOf(
                    "Automatic-Module-Name" to "io.qameta.allure.seleniumbidi"
                )
            )
        }
    }
    test {
        useJUnitPlatform()
        jvmArgs("-javaagent:${agent.singleFile}")
        systemProperty("allure.model.indentOutput", "true")
        systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
    }
}
