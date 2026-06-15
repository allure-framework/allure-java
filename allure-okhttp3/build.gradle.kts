description = "Allure OkHttp3 Integration"

val okhttpVersion = "5.4.0"

dependencies {
    api(project(":allure-java-commons"))
    compileOnly("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("org.wiremock:wiremock")
    testImplementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("org.assertj:assertj-core")
    testImplementation(project(":allure-assertj"))
    testImplementation("org.jboss.resteasy:resteasy-client")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
