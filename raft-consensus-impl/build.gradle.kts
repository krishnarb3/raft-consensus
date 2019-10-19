import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.spring") version("1.3.50")
    id("org.springframework.boot") version("2.1.6.RELEASE")
    id("io.spring.dependency-management") version("1.0.8.RELEASE")
}

group = "com.rbkrishna.distributed"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compile(project(":raft-consensus-api"))
    compile("org.springframework.boot:spring-boot-starter-web")
    testCompile("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

springBoot {
    mainClassName = "com.rbkrishna.distributed.impl.RaftApplication"
}