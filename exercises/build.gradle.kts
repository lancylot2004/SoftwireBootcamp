import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import proguard.gradle.ProGuardTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.shadow)
}

group = "dev.lancy.softwire"
version = "0.0.1"

buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.7.0")
    }
}

repositories {
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/ki/maven")
    maven("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")

    flatDir {
        dirs("$rootDir/libs")
    }
}

dependencies {
    // Dynamite
    implementation(":dynamite.interface")
    implementation(":dynamite.runner")

    compileOnly(libs.jetbrains.annotations)

    // JUnit 5 | Testing Framework | https://github.com/junit-team/junit-framework/ | EPL-2.0
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)

    // jqwik | Property-Based Testing | https://github.com/jqwik-team/jqwik | EPL-2.0
    testImplementation(libs.jqwik)

    // KInference | ONNX Runtime for Kotlin | https://github.com/JetBrains-Research/kinference | Apache-2.0
    api("io.kinference", "inference-core", "0.2.9")
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

tasks.register<ProGuardTask>("proguard") {
    dependsOn(tasks.shadowJar)
    notCompatibleWithConfigurationCache("ProGuardTask is not cache-safe")
    configuration(file("proguard.pro"))
    injars(tasks.shadowJar.flatMap { it.archiveFile })
    libraryjars("/Users/lanliu/Library/Java/JavaVirtualMachines/corretto-1.8.0_452/Contents/Home/lib/rt.jar")
    verbose()
    outjars(layout.buildDirectory.file("libs/dynamiteBot-min.jar"))
}

tasks.register<Jar>("fixManifest") {
    dependsOn("proguard")
    archiveFileName.set("dynamiteBot-fix.jar")
    destinationDirectory.set(file("${layout.buildDirectory}/libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        zipTree(file(layout.buildDirectory.file("libs/dynamiteBot-min.jar")))
    })

    manifest {
        attributes["Main-Class"] = "dev.lancy.softwire.dynamite.RunnerKt"
    }
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "dev.lancy.softwire.dynamite.RunnerKt"
    }

    archiveBaseName = "dynamiteBot"
    archiveVersion = ""
    archiveClassifier = ""

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("dev/lancy/softwire/dynamite/fizzbuzz")
    exclude("dev/lancy/softwire/dynamite/gildedrose")
}

tasks.register<UploadTask>("upload") {
    dependsOn("jar")
    botName = project.findProperty("botname")?.toString()
        ?: throw GradleException("[Missing Name] Please provide a bot name using -Pbotname=YourBotName")
}

tasks.test {
    useJUnitPlatform()
}
