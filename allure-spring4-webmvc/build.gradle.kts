description = "Allure Spring 4 Web MVC Integration"

dependencies {
    compile("org.springframework:spring-webmvc")
    compile(project(":allure-servlet-api"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.spring4webmvc"
        ))
    }
}
