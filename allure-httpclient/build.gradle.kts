description = "Allure Apache HttpClient Integration"

dependencies {
    compile("org.apache.httpcomponents:httpclient")
    compile(project(":allure-attachments"))
    testCompile("com.github.tomakehurst:wiremock")
    testCompile("junit:junit")
    testCompile("org.assertj:assertj-core")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.httpclient"
        ))
    }
}
