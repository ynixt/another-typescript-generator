package io.github.ynixt.anothertypescriptgenerator

import io.github.classgraph.ClassGraph
import java.net.URL
import kotlin.reflect.KClass

class ClassFinder(
    private val classPackages: List<String> = emptyList(),
    private val excludeClassPackages: List<String> = emptyList(),
    private val classPathUrls: List<URL>,
) {
    fun findClasses(): List<KClass<*>> {
        val scanResult = ClassGraph()
            .overrideClasspath(classPathUrls)
            .verbose()
            .enableAllInfo()
            .acceptPackages(*classPackages.toTypedArray())
            .rejectPackages(*excludeClassPackages.toTypedArray())
            .ignoreClassVisibility()
            .scan()

        val allClasses = scanResult.allClasses.filter { !it.name.endsWith("\$Companion") }

        return allClasses.map { classInfo ->
            classInfo.loadClass().kotlin
        }
    }
}