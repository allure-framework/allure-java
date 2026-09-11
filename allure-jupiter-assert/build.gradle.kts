description = "Allure Jupiter Assertions Integration"

// Compile the optional type used by the regression fixture without adding it to the test runtime.
val missingDependency by sourceSets.creating

dependencies {
    implementation("org.slf4j:slf4j-api")
    api(project(":allure-java-commons"))
    implementation(project(":allure-jupiter"))
    compileOnly("org.aspectj:aspectjrt")
    compileOnly("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.aspectj:aspectjweaver")
    testImplementation("org.assertj:assertj-core")
    testImplementation(project(":allure-assertj"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.slf4j:slf4j-simple")
    testCompileOnly(missingDependency.output)
    testImplementation(project(":allure-java-commons-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
