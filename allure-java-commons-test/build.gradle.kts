description = "Allure Java Commons Test Utils"

dependencies {
    api("commons-io:commons-io")
    api("io.github.benas:random-beans")
    api("org.apache.commons:commons-lang3")
    api(project(":allure-java-commons"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.commonstest"
        ))
    }
}
