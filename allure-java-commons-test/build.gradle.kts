description = "Allure Java Commons Test Utils"

dependencies {
    compile("commons-io:commons-io")
    compile(project(":allure-java-commons"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.commonstest"
        ))
    }
}
