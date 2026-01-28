package xyz.uthofficial.arnyan.env.utils.ksp.registry

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType

class MentsuTypeRegistryHandler(
    val codeGenerator: CodeGenerator,
    val packageName: String = "xyz.uthofficial.arnyan.env.generated",
    val fileName: String = "MentsuTypeRegistry"
) {
    fun generateRegistry(symbols: List<KSClassDeclaration>) {
        val sortedSymbols = symbols.sortedBy { it.qualifiedName?.asString() ?: it.simpleName.asString() }
        
        val SIZE = PropertySpec.builder(
            "SIZE", Int::class.asClassName()
        ).addModifiers(KModifier.CONST)
         .initializer("%L", sortedSymbols.size).build()
        
        val mentsuTypes = PropertySpec.builder(
            "mentsuTypes", List::class.asClassName().parameterizedBy(
                MentsuType::class.asClassName()
            )
        ).initializer(
            CodeBlock.builder().apply {
                add("listOf(\n")
                sortedSymbols.forEach { add("    %T,\n", it.toClassName()) }
                add(")")
            }.build()
        ).build()
        
        val getIndex = FunSpec.builder("getIndex")
            .addParameter("mentsuType", MentsuType::class.asClassName())
            .returns(Int::class)
            .beginControlFlow("return when(mentsuType)")
            .apply {
                sortedSymbols.forEachIndexed { index, symbol ->
                    addStatement("%T -> %L", symbol.toClassName(), index)
                }
            }
            .addStatement("else -> -1")
            .endControlFlow()
            .build()
        
        val getMentsuType = FunSpec.builder("getMentsuType")
            .addParameter("index", Int::class)
            .returns(MentsuType::class.asClassName())
            .beginControlFlow("return when(index)")
            .apply {
                sortedSymbols.forEachIndexed { index, symbol ->
                    addStatement("%L -> %T", index, symbol.toClassName())
                }
            }
            .addStatement("else -> throw %T(%S)", IllegalArgumentException::class, "Index out of bounds")
            .endControlFlow()
            .build()
        
        FileSpec.builder(packageName, fileName)
            .addType(
                TypeSpec.objectBuilder(fileName).addModifiers(KModifier.FINAL)
                    .addProperty(mentsuTypes)
                    .addProperty(SIZE)
                    .addFunction(getIndex)
                    .addFunction(getMentsuType)
                    .build()
            ).build()
            .writeTo(codeGenerator, Dependencies(true, *sortedSymbols.mapNotNull { it.containingFile }.toTypedArray()))
    }
}