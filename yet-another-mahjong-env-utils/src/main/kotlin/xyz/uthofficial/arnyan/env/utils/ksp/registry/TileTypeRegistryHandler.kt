package xyz.uthofficial.arnyan.env.utils.ksp.registry

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import xyz.uthofficial.arnyan.env.error.ArnyanError
import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.tile.TileType

class TileTypeRegistryHandler(
    val packageName: String = "xyz.uthofficial.arnyan.env.generated",
    val fileName: String = "TileTypeRegistry"
) {
    @OptIn(DelicateKotlinPoetApi::class)
    fun generateRegistry(symbols: List<KSClassDeclaration>) {
        val tileTypes = PropertySpec.builder(
            "tileTypes", List::class.java.asClassName().parameterizedBy(
                TileType::class.java.asClassName()
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
                            "index", Result::class.java.asClassName().parameterizedBy(
                                Int::class.java.asClassName(),
                                ArnyanError::class.java.asClassName()
                            )
                        ).receiver(TileType::class.java).getter(
                            FunSpec.getterBuilder()
                                .addStatement("val idx = %N.indexOf(this)", tileTypes)
                                .beginControlFlow("return if (idx != -1)")
                                .addStatement("%T.Success(idx)", Result::class)
                                .nextControlFlow("else")
                                .addStatement(
                                    "%T.Failure(%T.InvalidConfiguration(%S))",
                                    Result::class,
                                    ConfigurationError.InvalidConfiguration::class,
                                    "TileType not found in registry"
                                )
                                .endControlFlow()
                                .build()
                        ).build()
                    )
                    .build()
            )
    }
}