package io.github.ynixt.anothertypescriptgenerator.generator

import io.github.ynixt.anothertypescriptgenerator.AbsoluteArrayTypescriptType
import io.github.ynixt.anothertypescriptgenerator.AbsoluteTypescriptType
import io.github.ynixt.anothertypescriptgenerator.CustomType
import io.github.ynixt.anothertypescriptgenerator.GenericObjectTypescriptType
import io.github.ynixt.anothertypescriptgenerator.SubclassKotlinType
import io.github.ynixt.anothertypescriptgenerator.TypescriptType
import io.github.ynixt.anothertypescriptgenerator.Utils.lineSeparator
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class TypescriptParsedClass(
    private val kotlinParsedClass: KotlinParsedClass,
    private val customTypes: List<CustomType>,
    private val customTypesAbsolute: Map<KClass<*>, CustomType>,
    private val customTypesSubclass: List<CustomType>,
) {
    val fileName = kotlinParsedClass.getFileName()
    val packagePath = kotlinParsedClass.getPackagePath()

    private val typeParameters = kotlinParsedClass.typeParameters?.map {
        TypescriptParsedTypeParameter(
            name = it.name,
            bounds = it.bounds?.map { bound ->
                getClassifierForProperty(bound, false)
            }
        )
    }

    private val supertypes = kotlinParsedClass.supertypes?.map {
        getParsedTypeForProperty(it, false)
    }

    private val properties = getProperties()

    private val importsBlock = generateImportsBlock()
    private val classBlock = generateClassBlock()
    private val propertiesBlock = generatePropertiesBlock()

    fun asCode(): String {
        val sb = StringBuilder()

        sb.appendLine("/* eslint-disable */")
        sb.appendLine("/* tslint-disable */")
        sb.appendLine("")

        if (kotlinParsedClass.originalClass.java.isEnum) {
            val options = kotlinParsedClass.originalClass.java.enumConstants
            val optionsCode = options.joinToString(" | ") { "'$it'" }
            sb.appendLine("export type ${kotlinParsedClass.className} = $optionsCode;")
        } else {
            if (importsBlock.isNotEmpty()) {
                sb.appendLine(importsBlock.joinToString(lineSeparator))
                sb.appendLine()
            }

            sb.append("export interface $classBlock {")

            if (!propertiesBlock.isNullOrEmpty()) {
                sb.appendLine()
                sb.appendLine(propertiesBlock.joinToString(lineSeparator) { "  $it" })
            }

            sb.appendLine("}")
        }

        return sb.toString()
    }

    private fun generateImportsBlock(): List<String> {
        val imports = mutableListOf<String>()

        typeParameters?.filter { it.bounds != null }?.flatMap { it.bounds!! }?.forEach {
            imports.addAll(importTypescriptClassifier(it))
        }

        supertypes?.forEach {
            imports.addAll(getImportsOfParsedType(it))
        }

        properties?.map { it.returnType }?.forEach {
            imports.addAll(getImportsOfParsedType(it, true))
        }

        imports.sort()

        return imports.distinct()
    }

    private fun generateClassBlock(): String {
        val sb = StringBuilder()

        sb.append(kotlinParsedClass.className)

        if (!typeParameters.isNullOrEmpty()) {
            sb.append("<")
            sb.append(typeParameters.joinToString(", ") { it.asCode() })//
            sb.append(">")
        }

        val superTypesWithoutAny = supertypes?.filter { it.classifier.name != "Any" }

        if (!superTypesWithoutAny.isNullOrEmpty()) {
            sb.append(" extends ")
            sb.append(superTypesWithoutAny.joinToString(", ") { it.asCode() })
        }

        return sb.toString()
    }

    private fun generatePropertiesBlock(): List<String>? {
        return properties?.map { it.asCode() }
    }

    private fun importTypescriptClassifier(classifier: TypescriptParsedClassifier): List<String> {
        val list = mutableListOf<String>()

        if (classifier.customType != null) {
            if (classifier.customType!!.import != null) {
                list.add(classifier.customType!!.import!!)
            }
        } else {
            if (classifier.parsedClassifier != null) {
                list.addAll(importTypescriptClassifier(classifier.parsedClassifier))
            }

            if (classifier.parsedClass?.shouldGenerateNewFile == true) {
                if (classifier.parsedClass.originalClass != kotlinParsedClass.originalClass) {
                    list.add(importClass(classifier.parsedClass))
                }
            }
        }

        return list
    }

    private fun getProperties(): List<TypescriptParsedProperty>? {
        return kotlinParsedClass.properties?.map {
            TypescriptParsedProperty(
                name = it.name,
                returnType = getParsedTypeForProperty(it.returnType)
            )
        }
    }

    private fun getClassifierForProperty(classifier: ParsedClassifier, checkCustomTypes: Boolean = true): TypescriptParsedClassifier {
        var childClassifier: TypescriptParsedClassifier? = null
        val parsedClass: KotlinParsedClass? = classifier.parsedClass
        var customType: TypescriptType? = null

        if (classifier.parsedClass != null) {
            if (checkCustomTypes) {
                if (customTypesAbsolute.containsKey(classifier.parsedClass.originalClass)) {
                    customType = customTypesAbsolute[classifier.parsedClass.originalClass]!!.typescript
                } else {
                    val newOriginalClass = customTypesSubclass.find { typeSubclass ->
                        if (
                            (classifier.parsedClass.originalClass).isSubclassOf((typeSubclass.kotlin as SubclassKotlinType).kClass) ||
                            classifier.parsedClass.originalClass.qualifiedName == typeSubclass.kotlin.kClass.qualifiedName
                        ) {
                            return@find true
                        }
                        false
                    }

                    if (newOriginalClass != null) {
                        customType = newOriginalClass.typescript
                    }
                }
            }
        }

        if (classifier.parsedClassifier != null) {
            childClassifier = getClassifierForProperty(classifier.parsedClassifier!!, checkCustomTypes)
        }

        return TypescriptParsedClassifier(
            name = classifier.name,
            parsedClass = parsedClass,
            parsedClassifier = childClassifier,
            customType = customType
        )
    }

    private fun getParsedTypeForProperty(parsedType: ParsedType, checkCustomTypes: Boolean = true): TypescriptParsedType {
        return TypescriptParsedType(
            nullable = parsedType.nullable,
            classifier = getClassifierForProperty(parsedType.classifier, checkCustomTypes),
            argumentsTypes = parsedType.argumentsTypes?.map { argumentType ->
                getParsedTypeForProperty(argumentType, checkCustomTypes)
            }
        )
    }

    private fun importClass(kotlinParsedClass: KotlinParsedClass): String {
        val fileName = getRelativePath(this.packagePath, kotlinParsedClass.getFileName())

        return "import { ${kotlinParsedClass.className} } from '$fileName';"
    }

    private fun getImportsOfParsedType(parsedType: TypescriptParsedType, onlyOneArgument: Boolean = false): List<String> {
        val imports = mutableListOf<String>()


        imports.addAll(importTypescriptClassifier(parsedType.classifier))

        if (parsedType.argumentsTypes != null) {
            if (onlyOneArgument && parsedType.argumentsTypes.size == 2) {
                imports.addAll(getImportsOfParsedType(parsedType.argumentsTypes[1]))
            } else {
                imports.addAll(parsedType.argumentsTypes.map { getImportsOfParsedType(it) }.flatten())
            }
        }

        return imports
    }

    private fun getRelativePath(currentPackage: String, classPathThatWillBeImported: String): String {
        val currentPackageParts = currentPackage.trim('/').split('/')
        val classPathParts = classPathThatWillBeImported.trim('/').split('/')

        // Find common prefix length
        var commonLength = 0
        while (commonLength < currentPackageParts.size && commonLength < classPathParts.size &&
            currentPackageParts[commonLength] == classPathParts[commonLength]
        ) {
            commonLength++
        }

        // Calculate the number of '..' needed
        val upMoves = currentPackageParts.size - commonLength
        val upPath = if (upMoves > 0) "../".repeat(upMoves) else "."

        // Calculate the remaining path from the class path
        val downPath = classPathParts.drop(commonLength).joinToString("/")

        // Combine the up path and down path
        return if (upPath == "." && downPath.isNotEmpty()) "./$downPath" else upPath + downPath
    }
}

class TypescriptParsedTypeParameter(
    name: String,
    override val bounds: List<TypescriptParsedClassifier>?
) : ParsedTypeParameter(
    name,
    bounds
) {
    fun asCode(): String {
        val sb = StringBuilder()

        sb.append(name)

        if (!bounds.isNullOrEmpty()) {
            sb.append(" extends ")
            sb.append(bounds.joinToString(", ") { it.asCode() })
        }

        return sb.toString()
    }
}

class TypescriptParsedProperty(
    name: String,
    override val returnType: TypescriptParsedType
) : ParsedProperty(name, returnType) {
    fun asCode(): String {
        val sb = StringBuilder()

        sb.append(name)
        if (returnType.nullable) {
            sb.append("?")
        }
        sb.append(": ")
        sb.append(returnType.asCode())
        sb.append(";")

        return sb.toString()
    }
}

class TypescriptParsedType(
    nullable: Boolean,
    override val classifier: TypescriptParsedClassifier,
    override val argumentsTypes: List<TypescriptParsedType>? = null
) : ParsedType(
    nullable,
    classifier,
    argumentsTypes
) {
    fun asCode(): String {
        val sb = StringBuilder()

        if (argumentsTypes != null && argumentsTypes.size == 2) {
            val returnType = argumentsTypes[1].asCode()
            sb.append("{ [key: string]: $returnType }")
        } else {
            val classifierCode = classifier.asCode()
            val shouldGenerateArguments = classifier.customType?.ignoreGenerics != true

            sb.append(classifierCode)

            if (classifierCode != "any" && !argumentsTypes.isNullOrEmpty() && shouldGenerateArguments) {
                sb.append("<")
                sb.append(argumentsTypes.joinToString(", ") { it.asCode() })
                sb.append(">")
            }
        }

        if (nullable) {
            sb.append(" | null")
        }

        return sb.toString()
    }
}

class TypescriptParsedClassifier(
    name: String,
    parsedClass: KotlinParsedClass? = null,
    override val parsedClassifier: TypescriptParsedClassifier? = null,
    var customType: TypescriptType?
) : ParsedClassifier(
    name,
    parsedClass,
    parsedClassifier
) {
    fun asCode(): String {
        val sb = StringBuilder()
        val customType = this.customType

        if (customType != null) {
            when (customType) {
                is AbsoluteTypescriptType -> sb.append(customType.name)
                is AbsoluteArrayTypescriptType -> sb.append(customType.name)
                is GenericObjectTypescriptType -> {}
            }
        } else if (parsedClass != null) {
            if (parsedClass.shouldGenerateNewFile) {
                sb.append(parsedClass.className)
            } else {
                sb.append("any")
            }
        } else if (name.isNotEmpty()) {
            sb.append(name)
        } else {
            sb.append("any")
        }

        return sb.toString()
    }
}