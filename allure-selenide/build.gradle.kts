description = "Allure Selenide Integration"

val selenideVersion = "5.2.3"

dependencies {
    api(project(":allure-java-commons"))
    implementation("com.codeborne:selenide:$selenideVersion")
}

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.selenide"
        ))
    }
}
