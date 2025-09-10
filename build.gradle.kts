plugins {
    kotlin("jvm") version "2.2.10"
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

group = "io.github.ynixt"
version = "1.2.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
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
            id = "io.github.ynixt.another-typescript-generator"
            implementationClass = "io.github.ynixt.anothertypescriptgenerator.AnotherTypescriptGeneratorPlugin"
            displayName = "Another Typescript Generator"
            description =
                "Another Typescript Generator is a Gradle plugin that generates Typescript interfaces from Kotlin/Java classes. Each class generates a new file to prevent collision."
            tags.set(listOf("kotlin", "typescript", "generator"))
        }
    }
}
