plugins {
    kotlin("jvm") version "2.0.0"
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "com.ynixt"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    implementation("io.github.classgraph:classgraph:4.8.174")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}

gradlePlugin {
    website.set("https://github.com/ynixt/another-typescript-generator")
    vcsUrl.set("https://github.com/ynixt/another-typescript-generator")

    plugins {
        create("anotherTypescriptGeneratorPlugin") {
            id = "com.ynixt.another-typescript-generator"
            implementationClass = "com.ynixt.anothertypescriptgenerator.AnotherTypescriptGeneratorPlugin"
            displayName = "Another Typescript Generator"
            description = "Another Typescript Generator is a Gradle plugin that generates Typescript interfaces from Kotlin/Java classes. Each class generates a new file to prevent collision."
            tags.set(listOf("kotlin", "typescript", "generator"))
        }
    }
}