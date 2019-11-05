import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.johnrengelman.shadow") version("5.0.0")
}

group = "com.rbkrishna.distributed"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

val mainVerticle = "com.rbkrishna.distributed.NodeService"
val mainClassName = "io.vertx.core.Launcher"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compile(project(":raft-consensus-api"))

    implementation(group = "io.vertx", name = "vertx-core", version = "3.7.1")
    implementation(group = "io.vertx", name = "vertx-ignite", version = "3.7.1")
    implementation(group = "io.vertx", name = "vertx-lang-kotlin", version = "3.7.1")
    implementation(group = "io.vertx", name = "vertx-lang-kotlin-coroutines", version = "3.7.1")

    testCompile(group = "org.jetbrains.kotlin", name = "kotlin-test", version = "1.3.40")
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

tasks.withType<ShadowJar> {
    classifier = "fat"
    manifest {
        attributes(mapOf(
            "Main-Verticle" to mainVerticle,
            "Main-Class" to mainClassName
        ))
    }
    mergeServiceFiles {
        include("META-INF/services/io.vertx.core.spi.VerticleFactory")
        include("META-INF/spring.*")
    }
}