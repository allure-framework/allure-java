description = "Allure TestNG Integration"

val agent by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    compile("org.testng:testng")
    compile(project(":allure-descriptions-javadoc"))
    compile(project(":allure-java-commons"))
    testCompile("com.google.inject:guice")
    testCompile("org.assertj:assertj-core")
    testCompile("org.mockito:mockito-core")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-java-commons-test"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.testng"
        ))
    }
    from("src/main/services") {
        into("META-INF/services")
    }
}

tasks.named<Test>("test") {
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
    classifier = "sources"
}

val spiOff by configurations.creating {
    extendsFrom(configurations.getByName("compile"))
}

artifacts.add("archives", spiOffJar)
artifacts.add("spiOff", spiOffJar)
