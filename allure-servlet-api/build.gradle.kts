description = "Allure Servlet API v3 Integration"

var servletApiVersion = "4.0.1"

dependencies {
    api(project(":allure-attachments"))
    implementation("javax.servlet:javax.servlet-api:$servletApiVersion")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.servletapi"
        ))
    }
}
