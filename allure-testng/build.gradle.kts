description = "Allure TestNG Integration"

val agent: Configuration by configurations.creating

val testNgVersion = "6.14.3"
val latestTestNgVersion = "7.4.0"

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    implementation("org.testng:testng:$testNgVersion")
    implementation(project(":allure-test-filter"))
    testAnnotationProcessor("org.slf4j:slf4j-simple")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("com.google.inject:guice")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-java-commons-test"))
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.testng"
        ))
    }
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.test {
    useTestNG(closureOf<TestNGOptions> {
        suites("src/test/resources/testng.xml")
    })
    exclude("**/samples/*")
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}

val taskSuffix = latestTestNgVersion.replace('.', '_')

tasks.create("test_$taskSuffix", GradleBuild::class) {
    buildName = "allure-testng-$taskSuffix"
    startParameter = project.gradle.startParameter.newInstance()
    startParameter.projectProperties["testNgVersion"] = latestTestNgVersion
    tasks = listOf("test")
}

tasks.check {
    dependsOn("test_$taskSuffix")
}

val spiOffJar: Jar by tasks.creating(Jar::class) {
    from(sourceSets.getByName("main").output)
    archiveClassifier.set("spi-off")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifact(spiOffJar)
        }
    }
}
