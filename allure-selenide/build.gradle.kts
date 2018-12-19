description = "Allure Selenide Integration"

val selenideVersion = "4.12.2"

dependencies {
    compile(project(":allure-java-commons"))
    compile("com.codeborne:selenide:$selenideVersion")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.selenide"
        ))
    }
}
