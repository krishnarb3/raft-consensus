import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "com.rbkrishna.distributed"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-runtime", version = "0.13.0")
    compile(group = "ch.qos.logback", name = "logback-classic", version = "1.2.3")
    testCompile(group = "org.jetbrains.kotlin", name = "kotlin-test", version = "1.3.50")
    testCompile(group = "org.junit.jupiter", name = "junit-jupiter", version = "5.4.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}