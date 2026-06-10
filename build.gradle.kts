import com.github.spotbugs.snom.SpotBugsTask

val gradleScriptDir by extra("${rootProject.projectDir}/gradle")
val qualityConfigsDir by extra("$gradleScriptDir/quality-configs")
val spotlessDtr by extra("$qualityConfigsDir/spotless")

val libs = subprojects.filterNot { it.name in "allure-bom" }
val standardJavaLibs = libs.filterNot { it.name == "allure-scalatest" }
val javadocDescriptionProcessorExclusions = setOf(
    "allure-descriptions-javadoc",
    "allure-java-commons",
    "allure-model"
)

tasks.withType<Wrapper>().configureEach {
    gradleVersion = "9.5.1"
    distributionType = Wrapper.DistributionType.BIN
    distributionSha256Sum = "bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f"
}

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    checkstyle
    pmd
    id("com.github.spotbugs")
    id("com.diffplug.spotless")
    id("io.github.gradle-nexus.publish-plugin")
    id("io.qameta.allure")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

configure(listOf(rootProject)) {
    description = "Allure Java"
    group = "io.qameta.allure"
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

configure(subprojects) {
    group = "io.qameta.allure"
    version = version

    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    publishing {
        publications {
            withType<MavenPublication>().configureEach {
                suppressAllPomMetadataWarnings()
                versionMapping {
                    allVariants {
                        fromResolutionResult()
                    }
                }
                pom {
                    name.set(project.name)
                    description.set("Module ${project.name} of Allure Framework.")
                    url.set("https://allurereport.org/")
                    organization {
                        name.set("Qameta Software")
                        url.set("https://qameta.io/")
                    }
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("baev")
                            name.set("Dmitry Baev")
                            email.set("dmitry.baev@qameta.io")
                            url.set("https://github.com/baev")
                        }
                        developer {
                            id.set("eroshenkoam")
                            name.set("Artem Eroshenko")
                            email.set("artem.eroshenko@qameta.io")
                            url.set("https://github.com/eroshenkoam")
                        }
                    }
                    scm {
                        developerConnection.set("scm:git:git://github.com/allure-framework/allure-java")
                        connection.set("scm:git:git://github.com/allure-framework/allure-java")
                        url.set("https://github.com/allure-framework/allure-java")
                    }
                    issueManagement {
                        system.set("Github Issues")
                        url.set("https://github.com/allure-framework/allure-java/issues")
                    }
                    ciManagement {
                        system.set("Github Actions")
                        url.set("https://github.com/allure-framework/allure-java/actions")
                    }
                }
            }
            create<MavenPublication>("maven")
        }
    }

    signing {
        sign(publishing.publications)
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { !project.version.toString().endsWith("-SNAPSHOT") }
    }

    tasks.withType<GenerateModuleMetadata> {
        enabled = false
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

configure(libs) {
    val project = this
    apply(plugin = "checkstyle")
    apply(plugin = "pmd")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "io.qameta.allure")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "java")
    apply(plugin = "java-library")

    val orgSlf4jVersion = "2.0.17"
    val aspectJVersion = "1.9.25.1"
    val checkstyleVersion = "12.3.0"
    val pmdVersion = "7.15.0"
    val spotbugsVersion = "4.9.8"

    dependencies {
        if (project.name !in javadocDescriptionProcessorExclusions) {
            testAnnotationProcessor(rootProject.project(":allure-descriptions-javadoc"))
        }
    }

    dependencyManagement {
        imports {
            mavenBom("com.fasterxml.jackson:jackson-bom:2.21.1")
            mavenBom("org.junit:junit-bom:5.10.3")
        }
        dependencies {
            dependency("com.github.spotbugs:spotbugs:$spotbugsVersion")
            dependency("com.github.tomakehurst:wiremock:3.0.1")
            dependency("com.google.code.gson:gson:2.8.9")
            dependency("com.google.guava:guava:32.0.1-jre")
            dependency("com.google.inject:guice:7.0.0")
            dependency("com.google.testing.compile:compile-testing:0.23.0")
            dependency("com.puppycrawl.tools:checkstyle:$checkstyleVersion")
            dependency("com.squareup.retrofit2:retrofit:3.0.0")
            dependency("commons-io:commons-io:2.20.0")
            dependency("commons-beanutils:commons-beanutils:1.11.0")
            dependency("io.github.benas:random-beans:3.9.0")
            dependency("io.github.glytching:junit-extensions:2.6.0")
            dependency("jakarta.annotation:jakarta.annotation-api:3.0.0")
            dependency("net.sourceforge.pmd:pmd-java:$pmdVersion")
            dependency("org.apache.commons:commons-lang3:3.18.0")
            dependency("org.apache.commons:commons-text:1.10.0")
            dependency("org.aspectj:aspectjrt:$aspectJVersion")
            dependency("org.aspectj:aspectjweaver:$aspectJVersion")
            dependency("org.assertj:assertj-core:3.27.7")
            dependency("junit:junit:4.13.2")
            dependency("org.freemarker:freemarker:2.3.33")
            dependency("org.grpcmock:grpcmock-junit5:0.8.0")
            dependency("org.hamcrest:hamcrest:3.0")
            dependency("org.jboss.resteasy:resteasy-client:7.0.1.Final")
            dependency("org.mock-server:mockserver-netty:5.15.0")
            dependency("org.mockito:mockito-core:5.21.0")
            dependency("org.slf4j:slf4j-api:${orgSlf4jVersion}")
            dependency("org.slf4j:slf4j-nop:${orgSlf4jVersion}")
            dependency("org.slf4j:slf4j-simple:${orgSlf4jVersion}")
        }
        generatedPomCustomization {
            enabled(false)
        }
    }

    // Excluding shadowed jars from pom.xml https://github.com/gradle/gradle/issues/10861#issuecomment-576562961
    val internal by configurations.creating {
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = false
    }
    configurations.compileClasspath.get().extendsFrom(internal)
    configurations.runtimeClasspath.get().extendsFrom(internal)
    configurations.testCompileClasspath.get().extendsFrom(internal)
    configurations.testRuntimeClasspath.get().extendsFrom(internal)

    tasks {
        compileJava {
            options.compilerArgs.add("-Xlint:-options")
            options.release.set(17)
        }

        compileTestJava {
            options.compilerArgs.add("-parameters")
        }

        jar {
            manifest {
                attributes(
                    mapOf(
                        "Specification-Title" to project.name,
                        "Implementation-Title" to project.name,
                        "Implementation-Version" to project.version
                    )
                )
            }
        }

        test {
            systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
            systemProperty("allure.model.indentOutput", "true")
            systemProperty("junit.jupiter.execution.parallel.enabled", true)
            systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
            testLogging {
                listOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
            }
            maxHeapSize = project.property("test.maxHeapSize").toString()
            maxParallelForks = Integer.parseInt(project.property("test.maxParallelForks") as String)
            jvmArgs = listOf(
                "--add-opens",
                "java.base/java.lang=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.util=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.text=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens",
                "java.desktop/java.awt.font=ALL-UNNAMED"
            )
        }

        processTestResources {
            filesMatching("**/allure.properties") {
                filter {
                    it.replace("#project.description#", project.description ?: project.name)
                }
            }
        }
    }

    allure {
        adapter {
            // Many modules carry third-party test frameworks on the classpath as integration fixtures,
            // so never let the Gradle plugin auto-detect adapters from dependencies alone.
            autoconfigure.set(false)
            aspectjWeaver.set(true)
            aspectjVersion.set(aspectJVersion)

            // Every Gradle test task in this build runs on JUnit Platform now.
            // Avoid mentioning unused adapters here because allure-gradle adds mentioned
            // adapters to the test classpath even when we do not execute that framework.
            frameworks {
                junitPlatform {
                    enabled.set(true)
                    autoconfigureListeners.set(true)
                }
            }
        }
    }

    val enableQuality = true
    fun mainJavaSources(): FileTree = fileTree("src/main/java") {
        include("**/*.java")
    }

    fun checkstyleMainJavaSources(): FileTree = mainJavaSources().matching {
        exclude("cucumber/runtime/formatter/**")
    }

    checkstyle {
        toolVersion = checkstyleVersion
        configDirectory = rootProject.layout.projectDirectory.dir("gradle/quality-configs/checkstyle")
    }

    pmd {
        toolVersion = pmdVersion
        ruleSets = listOf()
        ruleSetFiles = rootProject.files("gradle/quality-configs/pmd/pmd.xml")
    }

    spotbugs {
        toolVersion = spotbugsVersion
        excludeFilter = rootProject.file("gradle/quality-configs/spotbugs/exclude.xml")

        afterEvaluate {
            val spotbugs = configurations.findByName("spotbugs")
            if (spotbugs != null) {
                dependencies {
                    spotbugs("org.slf4j:slf4j-simple")
                    spotbugs("com.github.spotbugs:spotbugs")
                }
            }
        }
    }

    tasks.checkstyleMain {
        source = checkstyleMainJavaSources()
        classpath = files()
        enabled = enableQuality
    }

    tasks.pmdMain {
        source = mainJavaSources()
        classpath = files()
        enabled = enableQuality
    }

    tasks.withType<SpotBugsTask>().configureEach {
        auxClassPaths.from(configurations.named("runtimeClasspath"))
        dependsOn(tasks.named("jar"))
        enabled = enableQuality
    }

    tasks.checkstyleTest {
        enabled = false
    }

    tasks.pmdTest {
        enabled = false
    }

    tasks.spotbugsTest {
        enabled = false
    }

    spotless {
        java {
            target("src/**/*.java")
            removeUnusedImports()
            importOrder("", "jakarta", "javax", "java", "\\#")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|import|open|module|//startfile)")
            eclipse().configFile("$spotlessDtr/eclipse-jdt.prefs")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        scala {
            target("src/**/*.scala")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|//startfile)")
            scalafmt("3.7.3").scalaMajorVersion("2.13").configFile("$spotlessDtr/scalafmt.conf")
            endWithNewline()
            replaceRegex("one blank line after package line", "(package .+;)\n+import", "$1\n\nimport")
            replaceRegex("one blank line after import lists", "(import .+;\n\n)\n+", "$1")
        }
        groovy {
            target("src/**/*.groovy")
            licenseHeaderFile("$spotlessDtr/header.java", "(package|//startfile) ")
            greclipse("4.29").configFile("$spotlessDtr/eclipse-jdt.prefs")
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

    java {
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType(Javadoc::class) {
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    val allDepsInsight by tasks.creating(DependencyInsightReportTask::class) {
        showingAllVariants.set(true)
    }
}

configure(standardJavaLibs) {
    publishing.publications.named<MavenPublication>("maven") {
        from(components["java"])
    }
}

allure {
    version.set("2.19.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}
