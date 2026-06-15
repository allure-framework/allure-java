description = "Allure Jakarta Servlet API Integration"

val jakartaServletApiVersion = "6.1.0"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("jakarta.servlet:jakarta.servlet-api:$jakartaServletApiVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("jakarta.servlet:jakarta.servlet-api:$jakartaServletApiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
