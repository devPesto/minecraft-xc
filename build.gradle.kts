/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

// plugin versioning
version = "0.0.0"

// jvm target
val JVM = 16 // 1.8 for 8, 11 for 11

// base of output jar name
val OUTPUT_JAR_NAME = "xc"

// target will be set to minecraft version by cli input parameter
var target = ""

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    // maven() // no longer needed in gradle 7

    // Apply the application plugin to add support for building a CLI application.
    application
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    
    maven { // paper
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
    maven { // protocol lib
        url = uri("https://repo.dmulloy2.net/nexus/repository/public/")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JVM))
    }
}

configurations {
    create("resolvableImplementation") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }
}

dependencies {
    // Align versions of all Kotlin components
    compileOnly(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    // uncomment to shadow into jar
    // configurations["resolvableImplementation"]("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Kotlin reflect api
    compileOnly("org.jetbrains.kotlin:kotlin-reflect")
    // uncomment to shadow into jar
    // configurations["resolvableImplementation"]("org.jetbrains.kotlin:kotlin-reflect")
    
    // toml parsing library
    compileOnly("org.tomlj:tomlj:1.0.0")
    configurations["resolvableImplementation"]("org.tomlj:tomlj:1.0.0")

    // protocol lib (for packets)
    compileOnly("com.comphenix.protocol:ProtocolLib:4.5.0")

    api("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    if ( project.hasProperty("1.12") === true ) {
        compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
        target = "1.12"
    } else if ( project.hasProperty("1.16") === true ) {
        compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
        target = "1.16"
    } else if ( project.hasProperty("1.18") === true ) {
        compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")
        target = "1.18"
    }
}

application {
    // Define the main class for the application.
    mainClassName = "phonon.xc.XCPluginKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-result-return-type")
}

tasks {
    named<ShadowJar>("shadowJar") {
        // verify valid target minecraft version
        doFirst {
            val supportedMinecraftVersions = setOf("1.12", "1.16", "1.18")
            if ( !supportedMinecraftVersions.contains(target) ) {
                throw Exception("Invalid Minecraft version! Supported versions are: 1.12, 1.16, 1.18")
            }
        }

        classifier = ""
        configurations = mutableListOf(project.configurations.named("resolvableImplementation").get())
        relocate("com.google", "phonon.xc.shadow.gson")
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    
    test {
        testLogging.showStandardStreams = true
    }
}

gradle.taskGraph.whenReady {
    tasks {
        named<ShadowJar>("shadowJar") {
            if ( hasTask(":release") ) {
                baseName = "${OUTPUT_JAR_NAME}-${target}"
                minimize() // FOR PRODUCTION USE MINIMIZE
            }
            else {
                baseName = "${OUTPUT_JAR_NAME}-${target}-SNAPSHOT"
                // minimize() // FOR PRODUCTION USE MINIMIZE
            }
        }
    }
}