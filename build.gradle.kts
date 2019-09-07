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
        classpath("com.diffplug.spotless:spotless-plugin-gradle:3.17.0")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
        classpath("gradle.plugin.com.github.spotbugs:spotbugs-gradle-plugin:1.6.9")
        classpath("io.spring.gradle:dependency-management-plugin:1.0.6.RELEASE")
        classpath("ru.vyarus:gradle-quality-plugin:3.3.0")
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
    gradleVersion = "5.6.1"
}

plugins {
    java
    `java-library`
    id("net.researchgate.release") version "2.7.0"
    id("io.qameta.allure") version "2.7.0"
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
    apply(plugin = "com.diffplug.gradle.spotless")
    apply(plugin = "io.qameta.allure")
    apply(from = "$gradleScriptDir/bintray.gradle")
    apply(from = "$gradleScriptDir/maven-publish.gradle")

    configure<DependencyManagementExtension> {
        imports {
            mavenBom("com.fasterxml.jackson:jackson-bom:2.9.8")
            mavenBom("org.junit:junit-bom:5.4.0")
        }
        dependencies {
            dependency("com.github.tomakehurst:wiremock:2.21.0")
            dependency("com.google.inject:guice:4.2.2")
            dependency("com.google.testing.compile:compile-testing:0.15")
            dependency("com.squareup.retrofit2:retrofit:2.5.0")
            dependency("commons-io:commons-io:2.6")
            dependency("io.github.benas:random-beans:3.8.0")
            dependency("io.github.glytching:junit-extensions:2.3.0")
            dependency("org.apache.commons:commons-lang3:3.8.1")
            dependency("org.apache.httpcomponents:httpclient:4.5.7")
            dependency("org.apache.tika:tika-core:1.20")
            dependency("org.aspectj:aspectjrt:1.9.4")
            dependency("org.aspectj:aspectjweaver:1.9.4")
            dependency("org.assertj:assertj-core:3.11.1")
            dependency("org.codehaus.groovy:groovy-all:2.5.6")
            dependency("org.freemarker:freemarker:2.3.28")
            dependency("org.jboss.resteasy:resteasy-client:3.6.2.Final")
            dependency("org.jooq:joor-java-8:0.9.10")
            dependency("org.mock-server:mockserver-netty:5.5.1")
            dependency("org.mockito:mockito-core:2.24.0")
            dependency("org.slf4j:slf4j-api:1.7.25")
            dependency("org.slf4j:slf4j-simple:1.7.25")
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
        testLogging {
            events("passed", "skipped", "failed")
        }
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
        checkstyleVersion = "8.22"
        pmdVersion = "6.16.0"
        spotbugsVersion = "3.1.11"
        codenarcVersion = "1.3"
        enabled = !project.hasProperty("disableQuality")
    }

    configure<SpotlessExtension> {
        java {
            target(fileTree(rootDir) {
                include("**/src/**/*.java")
                exclude("**/generated-sources/**/*.*")
            })
            removeUnusedImports()
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|import|open|module|//startfile)")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        scala {
            target(fileTree(rootDir) {
                include("**/src/**/*.scala")
            })
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|//startfile)")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        groovy {
            target(fileTree(rootDir) {
                include("**/src/**/*.groovy")
            })
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|//startfile) ")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        format("misc") {
            target(fileTree(rootDir) {
                include("**/*.gradle",
                        "**/*.gitignore",
                        "README.md",
                        "CONTRIBUTING.md",
                        "config/**/*.xml",
                        "src/**/*.xml")
            })
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
    version = "2.9.0"
    autoconfigure = false
    aspectjweaver = false
}

val aggregatedReport by tasks.creating(AllureReport::class) {
    clean = true
    resultsDirs = subprojects.map { file("${it.buildDir}/allure-results") }.filter { it.exists() }
}
