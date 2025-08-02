import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
}

application {
    mainClass.set("sp.service.sample.AppKt")
}

tasks.getByName<JavaCompile>("compileJava") {
    targetCompatibility = Version.jvmTarget
}

tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = Version.jvmTarget
}

dependencies {
    implementation(project(":lib"))
    implementation("com.github.kepocnhh:Bytes:0.3.0u-SNAPSHOT")
}
