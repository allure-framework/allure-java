description = "Allure OkHttp3 Integration"

val agent by configurations.creating

val okhttpVersion = "3.13.1"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-attachments"))
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("com.github.tomakehurst:wiremock")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.jboss.resteasy:resteasy-client")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.okhttp3"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
