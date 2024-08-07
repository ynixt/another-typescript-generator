package com.ynixt.anothertypescriptgenerator.generator

import com.ynixt.anothertypescriptgenerator.AbsoluteArrayTypescriptType
import com.ynixt.anothertypescriptgenerator.AbsoluteKotlinType
import com.ynixt.anothertypescriptgenerator.AbsoluteTypescriptType
import com.ynixt.anothertypescriptgenerator.CustomType
import com.ynixt.anothertypescriptgenerator.GenericObjectTypescriptType
import com.ynixt.anothertypescriptgenerator.KotlinType
import com.ynixt.anothertypescriptgenerator.MapDateOption
import com.ynixt.anothertypescriptgenerator.SubclassKotlinType
import com.ynixt.anothertypescriptgenerator.TypescriptType
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.reflect.KClass

class ClassesConverter(
    private val classes: Set<KClass<*>>,
    private val outputDir: String,
    private val mapDateOption: MapDateOption,
    private val ignoredClasses: Set<String>,
    private val ignoredFieldsByClass: Map<String, Set<String>>,
    userCustomTypes: List<CustomType>
) {
    private val customTypes: List<CustomType> = createCustomTypes(userCustomTypes)
    private val customTypesAbsolute: Map<KClass<*>, CustomType> =
        customTypes.filter { it.kotlin is AbsoluteKotlinType }.associateBy { (it.kotlin as AbsoluteKotlinType).kClass }
    private val customTypesSubclass: List<CustomType> = customTypes.filter { it.kotlin is SubclassKotlinType }

    fun convert() {
        val alreadyScannedClasses: MutableMap<KClass<*>, KotlinParsedClass> = mutableMapOf()

        parseTypeParameter(classes.map {
            KotlinParsedClass(it, true, ignoredClasses, ignoredFieldsByClass).also { parsed ->
                alreadyScannedClasses[parsed.originalClass] = parsed
            }
        }, alreadyScannedClasses)

        alreadyScannedClasses.values.forEach {
            it.parseProperties(alreadyScannedClasses.toMutableMap())
        }

        val filesCreated = alreadyScannedClasses.values.filter { it.shouldGenerateNewFile }
            .map { it.toTypescript(customTypes, customTypesAbsolute, customTypesSubclass) }
            .map {
                File(outputDir, it.packagePath).mkdirs()
                val newFile = File(outputDir, it.fileName + ".ts").also { file -> file.writeText(it.asCode()) }
                newFile.absolutePath
            }

        groupFilesByDirectory(filesCreated).entries.parallelStream().forEach {
            File(it.key, "index.ts").writeText(it.value.joinToString(separator = "\n") {
                "export * from './${
                    it.replace(
                        ".ts",
                        ""
                    )
                }'"
            } + "\n")
        }
    }

    private fun parseTypeParameter(
        parsedClasses: List<KotlinParsedClass>,
        alreadyScannedClasses: MutableMap<KClass<*>, KotlinParsedClass>
    ) {
        parsedClasses.forEach {
            val newParsed = it.parseTypeParameter(alreadyScannedClasses).toMutableList()

            newParsed.addAll(it.parseSupertypes(alreadyScannedClasses))

            if (newParsed.isNotEmpty()) {
                parseTypeParameter(newParsed, alreadyScannedClasses)
            }
        }
    }

    private fun createCustomTypes(userCustomTypes: List<CustomType>): List<CustomType> {
        val customTypes = userCustomTypes.toMutableList()

        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(String::class), AbsoluteTypescriptType(name = "string"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(Byte::class), AbsoluteTypescriptType(name = "number"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(Short::class), AbsoluteTypescriptType(name = "number"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(Int::class), AbsoluteTypescriptType(name = "number"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(Long::class), AbsoluteTypescriptType(name = "number"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(Float::class), AbsoluteTypescriptType(name = "number"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(Double::class), AbsoluteTypescriptType(name = "number"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(BigDecimal::class), AbsoluteTypescriptType(name = "number"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(BigInteger::class), AbsoluteTypescriptType(name = "number"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(Boolean::class), AbsoluteTypescriptType(name = "boolean"))
        insertTypeIfNotFound(customTypes, SubclassKotlinType(Collection::class), AbsoluteArrayTypescriptType(name = "Array"))
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(UUID::class), AbsoluteTypescriptType(name = "string"))
        insertTypeIfNotFound(customTypes, SubclassKotlinType(Map::class), GenericObjectTypescriptType())
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(Any::class), AbsoluteTypescriptType(name = "any"))
        createCustomTypesForDates(customTypes)

        return customTypes
    }

    private fun insertTypeIfNotFound(customTypes: MutableList<CustomType>, kotlinType: KotlinType, typescriptType: TypescriptType) {
        if (customTypes.find { it.kotlin == kotlinType } == null) {
            customTypes.add(
                CustomType(
                    kotlinType,
                    typescriptType
                )
            )
        }
    }

    private fun createCustomTypesForDates(customTypes: MutableList<CustomType>) {
        val typescriptType = when (mapDateOption) {
            MapDateOption.AS_DATE -> AbsoluteTypescriptType(name = "Date")
            MapDateOption.AS_STRING -> AbsoluteTypescriptType(name = "string")
            MapDateOption.AS_NUMBER -> AbsoluteTypescriptType(name = "number")
            MapDateOption.AS_MOMENT -> AbsoluteTypescriptType(name = "moment.Moment", "import moment from 'moment';")
            MapDateOption.AS_LUXON -> AbsoluteTypescriptType(
                name = "DateTime",
                "import { DateTime } from 'luxon';"
            )
        }

        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(LocalDate::class), typescriptType)
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(LocalDateTime::class), typescriptType)
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(ZonedDateTime::class), typescriptType)
        insertTypeIfNotFound(customTypes, AbsoluteKotlinType(Date::class), typescriptType)
    }

    private fun groupFilesByDirectory(filePaths: List<String>): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()

        for (filePath in filePaths) {
            val file = File(filePath)
            val directory = file.parent ?: continue
            val fileName = file.name

            if (directory !in result) {
                result[directory] = mutableListOf()
            }
            result[directory]?.add(fileName)
        }

        return result
    }
}