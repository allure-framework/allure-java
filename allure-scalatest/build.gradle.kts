import org.gradle.jvm.tasks.Jar

description = "Allure ScalaTest Integration"

apply(plugin = "scala")

val availableScalaVersions = listOf("2.11", "2.12")
val defaultScala211Version = "2.11.12"
val defaultScala212Version = "2.12.8"

var selectedScalaVersion = defaultScala212Version

if (hasProperty("scalaVersion")) {
    val scalaVersion: String by project
    selectedScalaVersion = when (scalaVersion) {
        "2.11" -> defaultScala211Version
        "2.12" -> defaultScala212Version
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

val agent by configurations.creating

dependencies {
    compile("org.scalatest:scalatest_$baseScalaVersion:3.0.5")
    agent("org.aspectj:aspectjweaver")
    compile("org.junit.jupiter:junit-jupiter-api")
    compile("org.junit.platform:junit-platform-launcher")
    compile(project(":allure-java-commons"))
    testAnnotationProcessor(project(":allure-descriptions-javadoc"))
    testCompile("io.github.glytching:junit-extensions")
    testCompile("org.assertj:assertj-core")
    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.junit.jupiter:junit-jupiter-params")
    testCompile("org.slf4j:slf4j-simple")
    testCompile(project(":allure-assertj"))
    testCompile(project(":allure-java-commons-test"))
    testCompile(project(":allure-junit-platform"))
    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

val scaladocJar by tasks.creating(Jar::class) {
    from(tasks.getByName("scaladoc"))
    classifier = "scaladoc"
}

artifacts.add("archives", scaladocJar)

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Automatic-Module-Name" to "io.qameta.allure.scalatest"
        ))
    }
}

tasks.named<Test>("test") {
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    useJUnitPlatform()
    exclude("**/testdata/*")
    doFirst {
        jvmArgs("-javaagent:${agent.singleFile}")
    }
}
