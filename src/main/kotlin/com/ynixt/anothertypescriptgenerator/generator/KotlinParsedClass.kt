package com.ynixt.anothertypescriptgenerator.generator

import com.ynixt.anothertypescriptgenerator.CustomType
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class KotlinParsedClass(
    val originalClass: KClass<*>,
    _shouldGenerateNewFile: Boolean,
    private val ignoredClasses: Set<String>,
    private val ignoredFieldsByClass: Map<String, Set<String>>,
) {
    val shouldGenerateNewFile = _shouldGenerateNewFile && !ignoredClasses.contains(originalClass.qualifiedName)

    val qualifiedName: String = originalClass.qualifiedName!!
    val className: String = originalClass.simpleName!!
    val classPackage: String = qualifiedName.replace(".$className", "")

    var supertypes: List<ParsedType>? = null
    var typeParameters: List<ParsedTypeParameter>? = null
    var properties: List<ParsedProperty>? = null

    fun getNameForFile(): String {
        return className.replace(Regex("([a-z])([A-Z]+)"), "$1-$2")
            .lowercase(Locale.getDefault())
    }

    fun getPackagePath(): String {
        return classPackage.replace(".", "/")
    }

    fun getFileName(): String {
        return getPackagePath() + "/" + getNameForFile()
    }


    fun parseSupertypes(alreadyScannedClasses: MutableMap<KClass<*>, KotlinParsedClass>): List<KotlinParsedClass> {
        val newClassesToParse = mutableListOf<KotlinParsedClass>()

        if (originalClass.supertypes.isNotEmpty()) {
            this.supertypes = originalClass.supertypes.mapNotNull {
                parseType(it, alreadyScannedClasses, newClassesToParse)
            }
        }

        return newClassesToParse
    }

    fun parseTypeParameter(alreadyScannedClasses: MutableMap<KClass<*>, KotlinParsedClass>): List<KotlinParsedClass> {
        val newClassesToParse = mutableListOf<KotlinParsedClass>()

        if (originalClass.typeParameters.isNotEmpty()) {
            this.typeParameters = originalClass.typeParameters.map {
                val bounds = if (it.upperBounds.isEmpty()) null else it.upperBounds.mapNotNull { bound ->
                    parseClassifier(bound.classifier, alreadyScannedClasses, newClassesToParse)
                }

                ParsedTypeParameter(
                    name = it.name,
                    bounds = bounds
                )
            }
        }

        return newClassesToParse
    }

    fun parseProperties(alreadyScannedClasses: MutableMap<KClass<*>, KotlinParsedClass>) {
        val publicProperties =
            originalClass.memberProperties.filter {
                it.visibility == KVisibility.PUBLIC &&
                        ignoredFieldsByClass[originalClass.qualifiedName]?.contains(
                            it.name
                        ) != true
            }

        properties = publicProperties.mapNotNull {
            val parsedType = parseType(it.returnType, alreadyScannedClasses, null)

            if (parsedType != null) {
                ParsedProperty(
                    it.name,
                    parsedType
                )
            } else {
                null
            }
        }
    }

    fun toTypescript(
        customTypes: List<CustomType>,
        customTypesAbsolute: Map<KClass<*>, CustomType>,
        customTypesSubclass: List<CustomType>,
    ): TypescriptParsedClass {
        return TypescriptParsedClass(this, customTypes, customTypesAbsolute, customTypesSubclass)
    }

    private fun parseType(
        kType: KType,
        alreadyScannedClasses: MutableMap<KClass<*>, KotlinParsedClass>,
        newClassesToParse: MutableList<KotlinParsedClass>?
    ): ParsedType? {
        val parsedClassifier = parseClassifier(kType.classifier, alreadyScannedClasses, newClassesToParse)

        if (parsedClassifier != null) {
            val argumentsTypes =
                kType.arguments.mapNotNull { argument ->
                    if (argument.type == null) null else parseType(
                        argument.type!!,
                        alreadyScannedClasses,
                        newClassesToParse
                    )
                }

            return ParsedType(
                nullable = kType.isMarkedNullable,
                classifier = parsedClassifier,
                argumentsTypes = argumentsTypes
            )
        }
        return null
    }

    private fun parseClassifier(
        classifier: KClassifier?,
        alreadyScannedClasses: MutableMap<KClass<*>, KotlinParsedClass>,
        newClassesToParse: MutableList<KotlinParsedClass>?
    ): ParsedClassifier? {
        return when (classifier) {
            is KClass<*> -> {
                if (classifier.simpleName == "Any" || classifier.simpleName == "Serializable") {
                    null
                } else {
                    if (!alreadyScannedClasses.containsKey(classifier)) {
                        val newClass =
                            KotlinParsedClass(classifier, newClassesToParse != null, ignoredClasses, ignoredFieldsByClass)
                        alreadyScannedClasses[classifier] = newClass

                        newClassesToParse?.add(newClass)
                    }

                    val classifierParsed = alreadyScannedClasses[classifier]
                    ParsedClassifier(classifier.simpleName!!, classifierParsed)
                }
            }

            is KTypeParameter -> ParsedClassifier(classifier.name)
            is KType ->
                ParsedClassifier("", parsedClassifier = parseClassifier(classifier, alreadyScannedClasses, newClassesToParse))

            else -> null
        }
    }
}

open class ParsedTypeParameter(
    val name: String,
    open val bounds: List<ParsedClassifier>?
)

open class ParsedClassifier(
    val name: String,
    val parsedClass: KotlinParsedClass? = null,
    open val parsedClassifier: ParsedClassifier? = null,
)

open class ParsedProperty(
    val name: String,
    open val returnType: ParsedType
)

open class ParsedType(
    val nullable: Boolean,
    open val classifier: ParsedClassifier,
    open val argumentsTypes: List<ParsedType>? = null
)

