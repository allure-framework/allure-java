description = "Allure Playwright Integration"

val agent: Configuration by configurations.creating

val playwrightVersion = "1.62.0"

dependencies {
    implementation("org.slf4j:slf4j-api")
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    compileOnly("com.microsoft.playwright:playwright:$playwrightVersion")
    compileOnly("org.aspectj:aspectjrt")
    // Gson, not a new dependency in practice: com.microsoft.playwright:playwright already declares it
    // (compile scope) in its own POM, so anyone with the real Playwright jar on their classpath already
    // has it. Used to rewrite trace.stacks in TraceSourceSanitizer.
    compileOnly("com.google.code.gson:gson")
    testImplementation("com.microsoft.playwright:playwright:$playwrightVersion")
    testImplementation("com.google.code.gson:gson")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
