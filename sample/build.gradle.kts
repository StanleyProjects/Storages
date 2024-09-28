import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
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
    implementation("com.github.kepocnhh:Bytes:0.2.1-SNAPSHOT")
}
