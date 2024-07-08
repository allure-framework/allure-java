description = "Allure Selenide Integration"

val selenideVersion = "7.3.3"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("com.codeborne:selenide:$selenideVersion")
    testImplementation("com.codeborne:selenide:$selenideVersion")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
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
                    "Automatic-Module-Name" to "io.qameta.allure.selenide"
                )
            )
        }
    }
    test {
        useJUnitPlatform()
    }
}
