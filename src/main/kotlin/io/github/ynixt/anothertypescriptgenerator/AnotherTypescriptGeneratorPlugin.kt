package io.github.ynixt.anothertypescriptgenerator

import org.gradle.api.Plugin
import org.gradle.api.Project

class AnotherTypescriptGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("anotherTypescriptGenerator", TypescriptGeneratorExtension::class.java)

        project.tasks.create("generateTypescriptInterfaces", GenerateTypescriptInterfacesTask::class.java).run {
            outputPath.set(extension.outputPath)
            classPrefix.set(extension.classPrefix)
            classSuffix.set(extension.classSuffix)
            classPackages.set(extension.classPackages)
            excludeClassPackages.set(extension.excludeClassPackages)
            customTypes.set(extension.customTypes)
            mapDate.set(extension.mapDate)
            deleteBefore.set(extension.deleteBefore)
            ignoredClasses.set(extension.ignoreClasses)
            ignoredFieldsByClass.set(extension.ignoredFieldsByClass)
            generateEnumOptions.set(extension.generateEnumOptions)
            generateEnumObject.set(extension.generateEnumObject)
        }
    }
}

open class TypescriptGeneratorExtension {
    var outputPath: String? = null
    var classPrefix: String = ""
    var classSuffix: String = ""
    var classPackages: List<String> = listOf()
    var excludeClassPackages: List<String> = listOf()
    var customTypes: List<CustomType> = listOf()
    var mapDate: MapDateOption = MapDateOption.AS_STRING
    var deleteBefore: Boolean = true
    var ignoreClasses: Set<String> = setOf()
    var ignoredFieldsByClass: Map<String, Set<String>> = mutableMapOf()
    var generateEnumOptions: Boolean = true
    var generateEnumObject: Boolean = false
}
