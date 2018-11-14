description = "Allure Apache HttpClient Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")

    compile("org.apache.httpcomponents:httpclient")
    compile(project(":allure-attachments"))

    testCompile("com.github.tomakehurst:wiremock")
    testCompile("io.github.glytching:junit-extensions")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.httpclient"
        ))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
