import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

group = "dev.lancy.softwire"
version = "0.0.1"
repositories {
    mavenCentral()
    flatDir {
        dirs("$rootDir/libs")
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

    // onnxruntime | ONNX Runtime for Java | https://github.com/microsoft/onnxruntime | MIT
    implementation(libs.onnx.runtime)
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
    manifest {
        attributes["Main-Class"] = "dev.lancy.softwire.dynamite.RunnerKt"
    }
    archiveFileName.set("dynamiteBot.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) // include your compiled classes
    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        },
    )

    // Exclude unnecessary files (optional safety)
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.register<UploadTask>("upload") {
    dependsOn("jar")
    botName = project.findProperty("botname")?.toString()
        ?: throw GradleException("[Missing Name] Please provide a bot name using -Pbotname=YourBotName")
}

tasks.test {
    useJUnitPlatform()
}
