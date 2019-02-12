description = "Allure Selenide Integration"

val selenideVersion = "4.12.2"

dependencies {
    api(project(":allure-java-commons"))
    implementation("com.codeborne:selenide:$selenideVersion")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.selenide"
        ))
    }
}
