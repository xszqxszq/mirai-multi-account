plugins {
    kotlin("multiplatform") version "1.6.10"
    `java-library`
    `maven-publish`
}

group = "xyz.xszq"
version = "v1.0.0"

repositories {
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
