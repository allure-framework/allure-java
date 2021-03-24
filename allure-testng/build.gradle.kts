description = "Allure TestNG Integration"

val agent: Configuration by configurations.creating

val testNgVersion = "7.4.0"

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

val spiOffJar by tasks.creating(Jar::class) {
    from(sourceSets.getByName("main").output)
    classifier = "spi-off"
}

val spiOff by configurations.creating {
    extendsFrom(configurations.getByName("compile"))
}

artifacts.add("archives", spiOffJar)
artifacts.add("spiOff", spiOffJar)
