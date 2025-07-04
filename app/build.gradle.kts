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
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
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
    manifest { attributes("Main-Class" to "dev.lancy.softwire.dynamite.RunnerKt") }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("dynamiteBot.jar")
}

tasks.register<UploadTask>("upload") {
    dependsOn("jar")
    botName = project.findProperty("botname")?.toString()
        ?: throw GradleException("[Missing Name] Please provide a bot name using -Pbotname=YourBotName")
}
