description = "Allure Spring Boot Integration"

dependencies {
    compile("org.springframework.boot:spring-boot-autoconfigure")
    compile(project(":allure-attachments"))
    compile(project(":allure-spring4-webmvc"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.springboot"
        ))
    }
}
