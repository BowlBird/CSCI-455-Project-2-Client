import org.jetbrains.compose.desktop.application.dsl.TargetFormat

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "csci.project.client.App"
    }
}

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "csci.project.client.App"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}
kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.material3:material3-desktop:1.2.1")
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "csci.project.client.App"
            packageVersion = "1.0.0"
        }
    }
}
