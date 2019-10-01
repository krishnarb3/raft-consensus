plugins {
    id("org.jetbrains.kotlin.jvm").version("1.3.40")
}

repositories {
    jcenter()
}

subprojects {
    repositories {
        mavenCentral()
    }
}

dependencies {
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8")
    testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test")
    testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test-junit")
}