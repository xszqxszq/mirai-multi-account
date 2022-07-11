val VERSION_NAME: String by project
plugins {
    kotlin("multiplatform") version "1.6.10"
    `java-library`
    `maven-publish`
    signing
}

group = "xyz.xszq"
version = VERSION_NAME


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
