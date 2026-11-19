description = "Allure CucumberJVM 8.0"

val cucumberVersion = "8.0.4"

dependencies {
    implementation("org.slf4j:slf4j-api")
    api(project(":allure-java-commons"))
    compileOnly(platform("io.cucumber:cucumber-bom:$cucumberVersion"))
    compileOnly("io.cucumber:cucumber-plugin")
    compileOnly("io.cucumber:cucumber-core")
    implementation("org.opentest4j:opentest4j:1.3.0")
    compileOnly("io.cucumber:gherkin")
    testImplementation("commons-io:commons-io")
    testImplementation(platform("io.cucumber:cucumber-bom:$cucumberVersion"))
    testImplementation("io.cucumber:cucumber-core")
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:gherkin")
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("allure.test.cucumber.version", cucumberVersion)
    systemProperty("allure.test.runtime.classpath", sourceSets.test.get().runtimeClasspath.asPath)
}
