package io.github.ynixt.anothertypescriptgenerator

import io.github.ynixt.anothertypescriptgenerator.generator.ClassesConverter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KClass


enum class MapDateOption {
    AS_DATE,
    AS_STRING,
    AS_MOMENT,
    AS_LUXON,
    AS_NUMBER
}

data class CustomType(
    val kotlin: KotlinType,
    val typescript: TypescriptType
)

abstract class KotlinType
abstract class KotlinTypeWithClass : KotlinType() {
    abstract val kClass: KClass<*>
}

abstract class KotlinTypeWithString : KotlinType() {
    abstract val qualifiedName: String
}

data class AbsoluteKotlinType(
    override val kClass: KClass<*>
) : KotlinTypeWithClass()

data class AbsoluteKotlinTypeString(
    override val qualifiedName: String
) : KotlinTypeWithString()

data class SubclassKotlinType(
    override val kClass: KClass<*>
) : KotlinTypeWithClass()

data class SubclassKotlinTypeString(
    override val qualifiedName: String
) : KotlinTypeWithString()

abstract class TypescriptType {
    abstract val import: String?
    abstract val ignoreGenerics: Boolean
}

data class AbsoluteTypescriptType(
    val name: String,
    override val import: String? = null,
    override val ignoreGenerics: Boolean = false,
) : TypescriptType()

data class AbsoluteArrayTypescriptType(
    val name: String,
    override val import: String? = null,
    override val ignoreGenerics: Boolean = false,
) : TypescriptType()

class GenericObjectTypescriptType(override val ignoreGenerics: Boolean = false,) : TypescriptType() {
    override val import: String? = null
}

open class GenerateTypescriptInterfacesTask : DefaultTask() {

    @Input
    val outputPath = project.objects.property(String::class.java)

    @Input
    val classPrefix = project.objects.property(String::class.java)

    @Input
    val classSuffix = project.objects.property(String::class.java)

    @Input
    val classPackages = project.objects.listProperty(String::class.java)

    @Input
    val excludeClassPackages = project.objects.listProperty(String::class.java)

    @Input
    val customTypes = project.objects.listProperty(CustomType::class.java)

    @Input
    val mapDate = project.objects.property(MapDateOption::class.java)

    @Input
    val deleteBefore = project.objects.property(Boolean::class.java)

    @Input
    val ignoredClasses = project.objects.setProperty(String::class.java)

    @Input
    val ignoredFieldsByClass = project.objects.mapProperty(String::class.java, Set::class.java as Class<Set<String>>)

    @TaskAction
    fun generate() {
        val outputDir = resolvePath(project.projectDir.absolutePath, outputPath.get())
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        println("Using ${outputDir.absolutePath} as outputDir.")

        if (deleteBefore.get()) {
            val files = outputDir.listFiles()
            if (files != null) {
                for (file in files) file.deleteRecursively()
            }
        }

        val urls: MutableSet<URL> = LinkedHashSet<URL>()
        for (task in project.tasks) {
            if (task.name.startsWith("compile") && !task.name.startsWith("compileTest")) {
                for (file in task.outputs.files) {
                    urls.add(file.toURI().toURL())
                }
            }
        }
        urls.addAll(getFilesFromConfiguration("compileClasspath"))

        var classLoader: URLClassLoader? = null

        try {
            classLoader = URLClassLoader(urls.toTypedArray(), Thread.currentThread().getContextClassLoader())
            Thread.currentThread().setContextClassLoader(classLoader)

            val classes = ClassFinder(
                classPackages = classPackages.get(),
                excludeClassPackages = excludeClassPackages.get(),
                classPathUrls = classLoader.urLs.toList(),
            ).findClasses()

            ClassesConverter(
                classes = classes.toSet(),
                outputDir = outputDir.absolutePath,
                userCustomTypes = loadClassOfStringCustomTypes(customTypes.get(), classes),
                mapDateOption = mapDate.get(),
                ignoredClasses = ignoredClasses.get(),
                ignoredFieldsByClass = ignoredFieldsByClass.get(),
            ).convert()
        } finally {
            classLoader?.close()
        }
    }

    private fun loadClassOfStringCustomTypes(customTypes: List<CustomType>, classes: List<KClass<*>>): List<CustomType> {
        val classByQualifiedName = classes.associateBy { it.qualifiedName }

        return customTypes.mapNotNull {
            val customKotlin: KotlinType? = if (it.kotlin is KotlinTypeWithString) {
                val ktClass = classByQualifiedName[it.kotlin.qualifiedName]

                if (ktClass != null) {
                    when (it.kotlin) {
                        is AbsoluteKotlinTypeString -> AbsoluteKotlinType(ktClass)
                        is SubclassKotlinTypeString -> SubclassKotlinType(ktClass)
                        else -> null
                    }
                } else {
                    null
                }
            } else {
                it.kotlin
            }

            if (customKotlin != null) {
                it.copy(
                    kotlin = customKotlin
                )
            } else {
                null
            }
        }
    }

    private fun getFilesFromConfiguration(configuration: String): List<URL> {
        try {
            val urls: MutableList<URL> = ArrayList()
            for (file in project.configurations.getAt(configuration).files) {
                urls.add(file.toURI().toURL())
            }
            return urls
        } catch (e: Exception) {
            println(String.format("Cannot get file names from configuration '%s': %s", configuration, e.message))
            return emptyList()
        }
    }

    private fun resolvePath(basePath: String, path: String): File {
        val file = File(path)
        return if (file.isAbsolute) {
            file
        } else {
            File(File(basePath), path).canonicalFile
        }
    }
}