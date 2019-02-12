description = "Allure Selenide Integration"

val selenideVersion = "5.1.0"

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
