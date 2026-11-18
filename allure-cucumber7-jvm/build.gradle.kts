description = "Allure CucumberJVM 7.0"

val cucumberVersion = "7.34.8"
val minimumCucumberVersion = "7.3.0"

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

val minimumCucumberTestRuntimeClasspath = configurations.testRuntimeClasspath.get().copyRecursive().apply {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.cucumber" && requested.name == "cucumber-bom") {
            useVersion(minimumCucumberVersion)
            because("7.3.0 is the oldest Cucumber JVM runtime supported by the current adapter API")
        }
    }
}

val cucumber7MinimumVersionTest = tasks.register<Test>("cucumber7MinimumVersionTest") {
    description = "Runs the metadata-label tag regression against Cucumber JVM $minimumCucumberVersion"
    group = "verification"
    dependsOn(tasks.testClasses)
    mustRunAfter(tasks.test)

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.main.get().output + sourceSets.test.get().output + minimumCucumberTestRuntimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching(
            "io.qameta.allure.cucumber7jvm.AllureCucumber7JvmTest.shouldPreferMetadataTagHierarchyOverDefaults"
        )
    }

    filter {
        includeTestsMatching("io.qameta.allure.cucumber7jvm.AllureMutedExecutionTest")
    }

    val standardTest = tasks.test.get()
    systemProperties(standardTest.systemProperties)
    systemProperty("allure.test.cucumber.version", minimumCucumberVersion)
    systemProperty("allure.test.runtime.classpath", classpath.asPath)
    jvmArgs = standardTest.jvmArgs
    maxHeapSize = standardTest.maxHeapSize
    maxParallelForks = standardTest.maxParallelForks
}

tasks.check {
    dependsOn(cucumber7MinimumVersionTest)
}
