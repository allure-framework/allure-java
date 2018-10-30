description = "Allure Javadoc Descriptions"

dependencies {
    compile(project(":allure-java-commons"))
    compile("commons-io:commons-io")

    testCompile("com.google.testing.compile:compile-testing")
    testCompile("org.slf4j:slf4j-simple")
    testCompile("org.testng:testng")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.description"
        ))
    }
}
