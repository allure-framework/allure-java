description = "Allure Servlet API v3 Integration"

var servletApiVersion = "4.0.1"

dependencies {
    compile(project(":allure-attachments"))
    compile("javax.servlet:javax.servlet-api:$servletApiVersion")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.servletapi"
        ))
    }
}
