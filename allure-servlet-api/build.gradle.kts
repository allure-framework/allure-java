description = "Allure Servlet API v3 Integration"

dependencies {
    compile(project(":allure-attachments"))
    compile("javax.servlet:javax.servlet-api")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.servletapi"
        ))
    }
}
