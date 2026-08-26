plugins {
    `java-platform`
}

description = "Allure Java (Bill of Materials)"

val scalaTestBinaryVersions = listOf("2.12", "2.13", "3")

dependencies {
    constraints {
        rootProject.subprojects.sorted().forEach {
            if (it.name == "allure-scalatest") {
                scalaTestBinaryVersions.forEach { scalaBinaryVersion ->
                    api("${it.group}:${it.name}_$scalaBinaryVersion:${it.version}")
                }
            } else {
                api("${it.group}:${it.name}:${it.version}")
            }
        }
    }
}

tasks.withType(Jar::class) {
    enabled = false
}

configurations.archives {
    artifacts.removeAll{ it.extension == "jar" }
}

publishing.publications.named<MavenPublication>("maven") {
    pom {
        from(components["javaPlatform"])
        description.set("This Bill of Materials POM can be used to ease dependency management " +
                "when referencing multiple Allure artifacts using Gradle or Maven.")
        packaging = "pom"
        withXml {
            val filteredContent = asString().replace("\\s*<scope>compile</scope>".toRegex(), "")
            asString().clear().append(filteredContent)
        }
    }
}
