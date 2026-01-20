package xyz.uthofficial.arnyan.env.utils.ksp.registry

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import xyz.uthofficial.arnyan.env.error.ArnyanError
import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.TileType

class TileTypeRegistryHandler(
    val codeGenerator: CodeGenerator,
    val packageName: String = "xyz.uthofficial.arnyan.env.generated",
    val fileName: String = "TileTypeRegistry"
) {
    @OptIn(DelicateKotlinPoetApi::class)
    fun generateRegistry(symbols: List<KSClassDeclaration>) {
        val tileTypes = PropertySpec.builder(
            "tileTypes", List::class.asClassName().parameterizedBy(
                TileType::class.asClassName()
            )
        ).initializer(
            CodeBlock.builder().apply {
                add("listOf(\n")
                symbols.forEach { add("    %T,\n", it.toClassName()) }
                add(")")
            }.build()
        ).build()

        FileSpec.builder(packageName, fileName)
            .addType(
                TypeSpec.objectBuilder(fileName)
                    .addProperty(tileTypes)
                    .addProperty(
                        PropertySpec.builder(
                            "index", Result::class.asClassName().parameterizedBy(
                                Int::class.asClassName(),
                                ArnyanError::class.asClassName()
                            )
                        ).receiver(TileType::class.asClassName()).getter(
                            FunSpec.getterBuilder()
                                .addStatement("val idx = %N.indexOf(this)", tileTypes)
                                .beginControlFlow("return if (idx != -1)")
                                .addStatement("%T(idx)", Result.Success::class.asClassName())
                                .nextControlFlow("else")
                                .addStatement(
                                    "%T(%T(%S))",
                                    Result.Failure::class.asClassName(),
                                    ConfigurationError.InvalidConfiguration::class.asClassName(),
                                    "TileType not found in registry"
                                )
                                .endControlFlow()
                                .build()
                        ).build()
                    )
                    .build()
            ).build()
            .writeTo(codeGenerator, Dependencies(true, *symbols.mapNotNull { it.containingFile }.toTypedArray()))
    }
}