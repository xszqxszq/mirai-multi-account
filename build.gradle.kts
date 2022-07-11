plugins {
    kotlin("multiplatform") version "1.6.10"
    `java-library`
    `maven-publish`
    signing
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

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "mirai-multi-account"
            from(components["java"])
            pom {
                name.set("mirai-multi-account")
                description.set("A library helping mirai plugins to filter duplicate events")
                url.set("https://github.com/xszqxszq/mirai-multi-account")

                licenses {
                    license {
                        name.set("GNU AGPLv3")
                        url.set("https://github.com/xszqxszq/mirai-multi-account/blob/main/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("xszq")
                        name.set("xszqxszq")
                        email.set("943551369@qq.com")
                    }
                }
            }
            artifact(tasks.sourcesJar.get())
            artifact(tasks.jar.get())
        }
    }
}