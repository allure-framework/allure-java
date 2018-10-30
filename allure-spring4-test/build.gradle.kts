description = "Allure Spring 4 test Integration"

dependencies {
    compile("org.springframework:spring-test")
    compile(project(":allure-java-commons"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.spring4test"
        ))
    }
}
