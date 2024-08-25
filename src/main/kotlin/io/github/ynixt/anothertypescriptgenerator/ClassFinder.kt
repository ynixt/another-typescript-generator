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
            .enableClassInfo()
            .acceptClasses(*classPackages.toTypedArray())
            .acceptPackages(*classPackages.toTypedArray())
            .rejectPackages(*excludeClassPackages.toTypedArray())
            .ignoreClassVisibility()
            .scan()

        val allClasses = scanResult.allClasses.filter { !it.name.endsWith("\$Companion") && !it.isSynthetic }

        return allClasses.mapNotNull { classInfo ->
            try {
                val kClass = classInfo.loadClass().kotlin

                if (kClass.qualifiedName != null) {
                    kClass
                } else {
                    println("$kClass doesn't have a qualified name.")
                    null
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }
    }
}