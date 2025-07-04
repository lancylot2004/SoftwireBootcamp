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

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.junit.launcher)
    compileOnly(libs.jetbrains.annotations)
}

kotlin {
    jvmToolchain(24)
}

jacoco {
    toolVersion = "0.8.13"
    reportsDirectory = layout.buildDirectory.dir("build/reports/jacoco")
}

tasks.jar {
    manifest { attributes("Main-Class" to "dev.lancy.softwire.dynamite.Runner") }

    // Do not allow duplicate entries.
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = false
    }
}

tasks.test {
    useJUnitPlatform()
}
