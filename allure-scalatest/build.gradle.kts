import org.gradle.jvm.tasks.Jar

description = "Allure ScalaTest Integration"

apply(plugin = "scala")

val availableScalaVersions = listOf("2.11", "2.12", "2.13")
val defaultScala211Version = "2.11.12"
val defaultScala212Version = "2.12.8"
val defaultScala213Version = "2.13.1"

var selectedScalaVersion = defaultScala213Version

if (hasProperty("scalaVersion")) {
    val scalaVersion: String by project
    selectedScalaVersion = when (scalaVersion) {
        "2.11" -> defaultScala211Version
        "2.12" -> defaultScala212Version
        "2.13" -> defaultScala213Version
        else -> scalaVersion
    }
}

val baseScalaVersion = selectedScalaVersion.substring(0, selectedScalaVersion.lastIndexOf("."))
project.base.archivesBaseName = "allure-scalatest_$baseScalaVersion"

for (sv in availableScalaVersions) {
    val taskSuffix = sv.replace('.', '_')

    tasks.create("jarScala_$taskSuffix", GradleBuild::class) {
        startParameter = project.gradle.startParameter.newInstance()
        startParameter.projectProperties["scalaVersion"] = sv
        tasks = listOf("jar")
    }

    tasks.create("testScala_$taskSuffix", GradleBuild::class) {
        startParameter = project.gradle.startParameter.newInstance()
        startParameter.projectProperties["scalaVersion"] = sv
        tasks = listOf("test")
    }

    tasks.create("sourceJarScala_$taskSuffix", GradleBuild::class) {
        startParameter = project.gradle.startParameter.newInstance()
        startParameter.projectProperties["scalaVersion"] = sv
        tasks = listOf("sourceJar")
    }

    tasks.create("scaladocJarScala_$taskSuffix", GradleBuild::class) {
        startParameter = project.gradle.startParameter.newInstance()
        startParameter.projectProperties["scalaVersion"] = sv
        tasks = listOf("scaladocJar")
    }

    tasks.create("installScala_$taskSuffix", GradleBuild::class) {
        startParameter = project.gradle.startParameter.newInstance()
        startParameter.projectProperties["scalaVersion"] = sv
        tasks = listOf("install")
    }
}

val jarAll by tasks.creating {
    dependsOn(availableScalaVersions.map { "jarScala_${it.replace('.', '_')}" })
}

val testAll by tasks.creating {
    dependsOn(availableScalaVersions.map { "testScala_${it.replace('.', '_')}" })
}

val sourceJarAll by tasks.creating {
    dependsOn(availableScalaVersions.map { "sourceJarScala_${it.replace('.', '_')}" })
}

val scaladocJarAll by tasks.creating {
    dependsOn(availableScalaVersions.map { "scaladocJarScala_${it.replace('.', '_')}" })
}

val installAll by tasks.creating {
    dependsOn(availableScalaVersions.map { "installScala_${it.replace('.', '_')}" })
}

val agent: Configuration by configurations.creating

dependencies {
    agent("org.aspectj:aspectjweaver")
    api(project(":allure-java-commons"))
    implementation("org.scalatest:scalatest_$baseScalaVersion:3.1.1")
    implementation("org.scala-lang.modules:scala-collection-compat_$baseScalaVersion:2.1.4")
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testImplementation("io.github.glytching:junit-extensions")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(project(":allure-assertj"))
    testImplementation(project(":allure-java-commons-test"))
    testImplementation(project(":allure-junit-platform"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

val scaladocJar by tasks.creating(Jar::class) {
    from(tasks.getByName("scaladoc"))
    archiveClassifier.set("scaladoc")
}

artifacts.add("archives", scaladocJar)

tasks.jar {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.scalatest"
        ))
    }
}

tasks.test {
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    useJUnitPlatform()
    exclude("**/testdata/*")
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
