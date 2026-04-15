description = "Allure Servlet API v3 Integration"

var servletApiVersion = "4.0.1"

dependencies {
    api(project(":allure-attachments"))
    compileOnly("javax.servlet:javax.servlet-api:$servletApiVersion")
    testImplementation("javax.servlet:javax.servlet-api:$servletApiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.servletapi"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
