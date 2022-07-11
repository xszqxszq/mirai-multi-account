val VERSION_NAME: String by project
plugins {
    kotlin("multiplatform") version "1.6.10"
    `java-library`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.20.0")
    }
}

group = "xyz.xszq"
version = VERSION_NAME

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api(platform("net.mamoe:mirai-bom:2.12.0"))
    api("net.mamoe:mirai-core-api")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

apply(plugin="com.vanniktech.maven.publish")