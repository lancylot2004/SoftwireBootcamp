import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

repositories {
    mavenCentral()
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Dynamite | The Game... | ????
    implementation(":dynamite.interface")
    implementation(":dynamite.runner")

    compileOnly(libs.jetbrains.annotations)

    // JUnit 5 | Testing Framework | https://github.com/junit-team/junit-framework/ | EPL-2.0
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)

    // jqwik | Property-Based Testing | https://github.com/jqwik-team/jqwik | EPL-2.0
    testImplementation(libs.jqwik)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
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

tasks.test {
    useJUnitPlatform()
}
