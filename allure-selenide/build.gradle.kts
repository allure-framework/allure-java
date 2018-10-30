description = "Allure Selenide Integration"

dependencies {
    compile(project(":allure-java-commons"))
    compile("com.codeborne:selenide")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.selenide"
        ))
    }
}
