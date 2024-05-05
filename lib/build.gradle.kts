import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sp.gx.core.Badge
import sp.gx.core.GitHub
import sp.gx.core.Markdown
import sp.gx.core.Maven
import sp.gx.core.asFile
import sp.gx.core.assemble
import sp.gx.core.buildDir
import sp.gx.core.check
import sp.gx.core.create
import sp.gx.core.existing
import sp.gx.core.file
import sp.gx.core.filled
import sp.gx.core.resolve
import sp.gx.core.task

version = "0.3.1"

val maven = Maven.Artifact(
    group = "com.github.kepocnhh",
    id = rootProject.name,
)

val gh = GitHub.Repository(
    owner = "StanleyProjects",
    name = rootProject.name,
)

repositories.mavenCentral()

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.gradle.jacoco")
    id("org.jetbrains.dokka") version Version.dokka
}

val compileKotlinTask = tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions {
        jvmTarget = Version.jvmTarget
        freeCompilerArgs = freeCompilerArgs + setOf("-module-name", maven.moduleName())
    }
}

tasks.getByName<JavaCompile>("compileTestJava") {
    targetCompatibility = Version.jvmTarget
}

tasks.getByName<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = Version.jvmTarget
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Version.jupiter}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.jupiter}")
}

fun Test.getExecutionData(): File {
    return buildDir()
        .dir("jacoco")
        .asFile("$name.exec")
}

val taskUnitTest = task<Test>("checkUnitTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED") // https://github.com/gradle/gradle/issues/18647
    doLast {
        getExecutionData().existing().file().filled()
    }
}

jacoco.toolVersion = Version.jacoco

val taskCoverageReport = task<JacocoReport>("assembleCoverageReport") {
    dependsOn(taskUnitTest)
    reports {
        csv.required = false
        html.required = true
        xml.required = false
    }
    sourceDirectories.setFrom(file("src/main/kotlin"))
    classDirectories.setFrom(sourceSets.main.get().output.classesDirs)
    executionData(taskUnitTest.getExecutionData())
    doLast {
        val report = buildDir()
            .dir("reports/jacoco/$name/html")
            .file("index.html")
            .existing()
            .file()
            .filled()
        println("Coverage report: ${report.absolutePath}")
    }
}

task<JacocoCoverageVerification>("checkCoverage") {
    dependsOn(taskCoverageReport)
    violationRules {
        rule {
            limit {
                minimum = BigDecimal(0.95)
            }
        }
    }
    classDirectories.setFrom(taskCoverageReport.classDirectories)
    executionData(taskCoverageReport.executionData)
}

"unstable".also { variant ->
    val version = "${version}u-SNAPSHOT"
    tasks.create("check", variant, "Readme") {
        doLast {
            val badge = Markdown.image(
                text = "version",
                url = Badge.url(
                    label = "version",
                    message = version,
                    color = "2962ff",
                ),
            )
            val expected = setOf(
                badge,
                Markdown.link("Maven", Maven.Snapshot.url(maven, version)),
                "implementation(\"${maven.moduleName(version)}\")",
            )
            rootDir.resolve("README.md").check(
                expected = expected,
                report = buildDir()
                    .dir("reports/analysis/readme")
                    .asFile("index.html"),
            )
        }
    }
    tasks.create("assemble", variant, "MavenMetadata") {
        doLast {
            val file = buildDir()
                .dir("yml")
                .file("maven-metadata.yml")
                .assemble(
                    """
                        repository:
                         groupId: '${maven.group}'
                         artifactId: '${maven.id}'
                        version: '$version'
                    """.trimIndent(),
                )
            println("Metadata: ${file.absolutePath}")
        }
    }
    task<Jar>("assemble", variant, "Jar") {
        dependsOn(compileKotlinTask)
        archiveBaseName = maven.id
        archiveVersion = version
        from(compileKotlinTask.destinationDirectory.asFileTree)
    }
    task<Jar>("assemble", variant, "Source") {
        archiveBaseName = maven.id
        archiveVersion = version
        archiveClassifier = "sources"
        from(sourceSets.main.get().allSource)
    }
    tasks.create("assemble", variant, "Pom") {
        doLast {
            val file = buildDir()
                .dir("libs")
                .file("${maven.name(version)}.pom")
                .assemble(
                    maven.pom(
                        version = version,
                        packaging = "jar",
                    ),
                )
            println("POM: ${file.absolutePath}")
        }
    }
}

"snapshot".also { variant ->
    val version = "$version-SNAPSHOT"
    tasks.create("check", variant, "Readme") {
        doLast {
            val badge = Markdown.image(
                text = "version",
                url = Badge.url(
                    label = "version",
                    message = version,
                    color = "2962ff",
                ),
            )
            val expected = setOf(
                badge,
                Markdown.link("Maven", Maven.Snapshot.url(maven, version)),
                "implementation(\"${maven.moduleName(version)}\")",
            )
            rootDir.resolve("README.md").check(
                expected = expected,
                report = buildDir()
                    .dir("reports/analysis/readme")
                    .asFile("index.html"),
            )
        }
    }
    tasks.create("assemble", variant, "MavenMetadata") {
        doLast {
            val file = buildDir()
                .dir("yml")
                .file("maven-metadata.yml")
                .assemble(
                    """
                        repository:
                         groupId: '${maven.group}'
                         artifactId: '${maven.id}'
                        version: '$version'
                    """.trimIndent(),
                )
            println("Metadata: ${file.absolutePath}")
        }
    }
    task<Jar>("assemble", variant, "Jar") {
        dependsOn(compileKotlinTask)
        archiveBaseName = maven.id
        archiveVersion = version
        from(compileKotlinTask.destinationDirectory.asFileTree)
    }
    task<Jar>("assemble", variant, "Source") {
        archiveBaseName = maven.id
        archiveVersion = version
        archiveClassifier = "sources"
        from(sourceSets.main.get().allSource)
    }
    tasks.create("assemble", variant, "Pom") {
        doLast {
            val file = buildDir()
                .dir("libs")
                .file("${maven.name(version)}.pom")
                .assemble(
                    maven.pom(
                        version = version,
                        packaging = "jar",
                    ),
                )
            println("POM: ${file.absolutePath}")
        }
    }
    task<DokkaTask>("assemble", variant, "Documentation") {
        outputDirectory = layout.buildDirectory.dir("documentation/$variant")
        moduleName = gh.name
        moduleVersion = version
        dokkaSourceSets.getByName("main") {
            val path = "src/$name/kotlin"
            reportUndocumented = false
            sourceLink {
                localDirectory = file(path)
                remoteUrl = gh.url().resolve("tree", moduleVersion.get(), "lib", path)
            }
            jdkVersion = Version.jvmTarget.toInt()
        }
        doLast {
            val index = outputDirectory.get()
                .file("index.html")
                .existing()
                .file()
                .filled()
            println("Documentation: ${index.absolutePath}")
        }
    }
    tasks.create("assemble", variant, "Metadata") {
        doLast {
            val file = buildDir()
                .dir("yml")
                .file("metadata.yml")
                .assemble(
                    """
                        repository:
                         owner: '${gh.owner}'
                         name: '${gh.name}'
                        version: '$version'
                    """.trimIndent(),
                )
            println("Metadata: ${file.absolutePath}")
        }
    }
}
