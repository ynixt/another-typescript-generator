plugins {
    kotlin("jvm") version "2.0.0"
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.ynixt"
version = "1.0.0"

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
    plugins {
        create("anotherTypescriptGeneratorPlugin") {
            id = "com.ynixt.another-typescript-generator"
            implementationClass = "com.ynixt.anothertypescriptgenerator.AnotherTypescriptGeneratorPlugin"
        }
    }
}