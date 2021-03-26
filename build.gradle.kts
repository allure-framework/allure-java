import com.diffplug.gradle.spotless.SpotlessExtension
import io.qameta.allure.gradle.AllureExtension
import io.qameta.allure.gradle.task.AllureReport
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.jvm.tasks.Jar
import ru.vyarus.gradle.plugin.quality.QualityExtension

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
        mavenLocal()
        jcenter()
    }

    dependencies {
        classpath("com.diffplug.spotless:spotless-plugin-gradle:5.11.0")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5")
        classpath("io.spring.gradle:dependency-management-plugin:1.0.10.RELEASE")
        classpath("ru.vyarus:gradle-quality-plugin:4.5.0")
    }
}

val linkHomepage by extra("https://qameta.io/allure")
val linkCi by extra("https://ci.qameta.in/job/allure-java_deploy/")
val linkScmUrl by extra("https://github.com/allure-framework/allure-java")
val linkScmConnection by extra("scm:git:git://github.com/allure-framework/allure-java.git")
val linkScmDevConnection by extra("scm:git:ssh://git@github.com:allure-framework/allure-java.git")

val gradleScriptDir by extra("${rootProject.projectDir}/gradle")
val qualityConfigsDir by extra("$gradleScriptDir/quality-configs")
val spotlessDtr by extra("$qualityConfigsDir/spotless")

tasks.withType(Wrapper::class) {
    gradleVersion = "6.7"
}

plugins {
    java
    `java-library`
    id("net.researchgate.release") version "2.8.1"
    id("io.qameta.allure") version "2.8.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

release {
    tagTemplate = "\${version}"
}

val afterReleaseBuild by tasks.existing

configure(listOf(rootProject)) {
    description = "Allure Java"
    group = "io.qameta.allure"
}

configure(subprojects) {
    val project = this
    group = "io.qameta.allure"
    version = version

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "ru.vyarus.quality")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "io.qameta.allure")
    apply(from = "$gradleScriptDir/bintray.gradle")
    apply(from = "$gradleScriptDir/maven-publish.gradle")

    configure<DependencyManagementExtension> {
        imports {
            mavenBom("com.fasterxml.jackson:jackson-bom:2.11.3")
            mavenBom("org.junit:junit-bom:5.7.0")
        }
        dependencies {
            dependency("com.github.tomakehurst:wiremock:2.27.2")
            dependency("com.google.inject:guice:4.2.3")
            dependency("com.google.testing.compile:compile-testing:0.19")
            dependency("com.squareup.retrofit2:retrofit:2.9.0")
            dependency("commons-io:commons-io:2.8.0")
            dependency("io.github.benas:random-beans:3.9.0")
            dependency("io.github.glytching:junit-extensions:2.4.0")
            dependency("org.apache.commons:commons-lang3:3.12.0")
            dependency("org.apache.httpcomponents:httpclient:4.5.13")
            dependency("org.apache.tika:tika-core:1.25")
            dependency("org.aspectj:aspectjrt:1.9.6")
            dependency("org.aspectj:aspectjweaver:1.9.6")
            dependency("org.assertj:assertj-core:3.17.2")
            dependency("org.codehaus.groovy:groovy-all:2.5.13")
            dependency("org.freemarker:freemarker:2.3.31")
            dependency("org.jboss.resteasy:resteasy-client:4.5.8.Final")
            dependency("org.jooq:joor-java-8:0.9.13")
            dependency("org.mock-server:mockserver-netty:5.11.2")
            dependency("org.mockito:mockito-core:3.8.0")
            dependencySet("org.slf4j:1.7.30") {
                entry("slf4j-api")
                entry("slf4j-nop")
                entry("slf4j-simple")
            }
        }
    }

    tasks.compileJava {
        options.encoding = "UTF-8"
    }

    tasks.compileTestJava {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.jar {
        manifest {
            attributes(mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version
            ))
        }
    }

    tasks.test {
        systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
        systemProperty("allure.model.indentOutput", "true")
        systemProperty("junit.jupiter.execution.parallel.enabled", true)
        systemProperty("junit.jupiter.execution.parallel.mode.default", true)
        testLogging {
            listOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
        }
        maxHeapSize = project.property("test.maxHeapSize").toString()
        maxParallelForks = Integer.parseInt(project.property("test.maxParallelForks") as String)
    }

    tasks.processTestResources {
        filesMatching("**/allure.properties") {
            filter {
                it.replace("#project.description#", project.description ?: project.name)
            }
        }
    }

    configure<QualityExtension> {
        configDir = qualityConfigsDir
        checkstyleVersion = "8.36.1"
        pmdVersion = "6.27.0"
        spotbugsVersion = "4.1.2"
        codenarcVersion = "1.6"
        enabled = !project.hasProperty("disableQuality")
        afterEvaluate {
            val spotbugs = configurations.findByName("spotbugs")
            if (spotbugs != null) {
                dependencies {
                    spotbugs("org.slf4j:slf4j-simple")
                    spotbugs("com.github.spotbugs:spotbugs:3.1.12")
                }
            }
        }
    }

    configure<SpotlessExtension> {
        java {
            target("src/**/*.java")
            removeUnusedImports()
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|import|open|module|//startfile)")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        scala {
            target("src/**/*.scala")
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|//startfile)")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        groovy {
            target("src/**/*.groovy")
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|//startfile) ")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        format("misc") {
            target(
                    "*.gradle",
                    "*.gitignore",
                    "README.md",
                    "CONTRIBUTING.md",
                    "config/**/*.xml",
                    "src/**/*.xml"
            )
            trimTrailingWhitespace()
            endWithNewline()
        }
        encoding("UTF-8")
    }

    configure<AllureExtension> {
        autoconfigure = false
        aspectjweaver = false
    }

    val sourceJar by tasks.creating(Jar::class) {
        from(sourceSets.getByName("main").allSource)
        archiveClassifier.set("sources")
    }

    val javadocJar by tasks.creating(Jar::class) {
        from(tasks.getByName("javadoc"))
        archiveClassifier.set("javadoc")
    }

    tasks.withType(Javadoc::class) {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    artifacts.add("archives", sourceJar)
    artifacts.add("archives", javadocJar)

    val bintrayUpload by tasks.existing
    afterReleaseBuild {
        dependsOn(bintrayUpload)
    }

    repositories {
        jcenter()
        mavenLocal()
    }
}

allure {
    version = "2.13.5"
    autoconfigure = false
    aspectjweaver = false
}

val aggregatedReport by tasks.creating(AllureReport::class) {
    clean = true
    resultsDirs = subprojects.map { file("${it.buildDir}/allure-results") }.filter { it.exists() }
}
