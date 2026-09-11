description = "Allure Java Commons Test Utils"

dependencies {
    implementation("commons-io:commons-io")
    api("io.github.benas:random-beans")
    implementation("org.apache.commons:commons-lang3")
    api(project(":allure-java-commons"))
    implementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
