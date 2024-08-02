description = "Allure OkHttp3 Integration"

val okhttpVersion = "4.12.0"

dependencies {
    api(project(":allure-attachments"))
    compileOnly("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("com.github.tomakehurst:wiremock")
    testImplementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.jboss.resteasy:resteasy-client")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.okhttp3"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}
