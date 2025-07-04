import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    jacoco
}

repositories {
    mavenCentral()
}

dependencies {
    // Ad-hoc jar files for running dynamite.
    implementation(
        fileTree("libs") {
            from("libs")
            include("*.jar")
        },
    )

    testImplementation(libs.junit.jupiter)
}

tasks.withType(KotlinJvmCompile::class).configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_23)
    }
}

jacoco {
    toolVersion = "0.8.13"
    reportsDirectory = layout.buildDirectory.dir("build/reports/jacoco")
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = false
    }
}

tasks.jar {
    manifest { attributes("Main-Class" to "dev.lancy.softwire.dynamite.Runner") }

    // Do not allow duplicate entries.
    duplicatesStrategy = DuplicatesStrategy.FAIL
}
