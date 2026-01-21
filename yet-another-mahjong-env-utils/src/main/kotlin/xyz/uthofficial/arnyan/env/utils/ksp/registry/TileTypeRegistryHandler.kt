package xyz.uthofficial.arnyan.env.utils.ksp.registry

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.tile.TileType
import java.io.File

class TileTypeRegistryHandler(
    val codeGenerator: CodeGenerator,
    val packageName: String = "xyz.uthofficial.arnyan.env.generated",
    val fileName: String = "TileTypeRegistry"
) {
    fun generateRegistry(symbols: List<KSClassDeclaration>) {
        val ranges = symbols.associateWith { getRange(it) }
        val offsets = mutableMapOf<KSClassDeclaration, Int>()
        var currentOffset = 0
        symbols.forEach {
            offsets[it] = currentOffset
            val range = ranges[it]!!
            currentOffset += (range.last - range.first + 1)
        }
        val totalSize = currentOffset

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

        val maskBlock = CodeBlock.builder()
        maskBlock.add("intArrayOf(")
        symbols.forEachIndexed {
            index,
            symbol ->
            val range = ranges[symbol]!!
            val count = range.last - range.first + 1
            val maskId = index + 1
            repeat(count) { maskBlock.add("%L, ", maskId) }
        }
        maskBlock.add(")")

        val connectivityMask = PropertySpec.builder(
            "connectivityMask", IntArray::class.asClassName()
        ).initializer(maskBlock.build()).build()

        val getHistogram = FunSpec.builder("getHistogram")
            .addParameter("hand", List::class.asClassName().parameterizedBy(Tile::class.asClassName()))
            .returns(IntArray::class)
            .addStatement("val histogram = IntArray(%L)", totalSize)
            .beginControlFlow("for (tile in hand)")
            .beginControlFlow("val index = when (tile.tileType)")
            .apply {
                symbols.forEach {
                    symbol ->
                    val range = ranges[symbol]!!
                    val offset = offsets[symbol]!!
                    val start = range.first
                    val adjustment = offset - start
                    addStatement("%T -> tile.value + (%L)", symbol.toClassName(), adjustment)
                }
            }
            .addStatement("else -> -1")
            .endControlFlow()
            .addStatement("if (index in 0 until %L) histogram[index]++", totalSize)
            .endControlFlow()
            .addStatement("return histogram")
            .build()

        val getTileType = FunSpec.builder("getTileType")
            .addParameter("index", Int::class)
            .returns(TileType::class.asClassName())
            .beginControlFlow("return when")
            .apply {
                symbols.forEach {
                    symbol ->
                    val range = ranges[symbol]!!
                    val offset = offsets[symbol]!!
                    val count = range.last - range.first + 1
                    val limit = offset + count
                    addStatement("index < %L -> %T", limit, symbol.toClassName())
                }
            }
            .addStatement("else -> throw %T(%S)", IllegalArgumentException::class, "Index out of bounds")
            .endControlFlow()
            .build()

        FileSpec.builder(packageName, fileName)
            .addType(
                TypeSpec.objectBuilder(fileName)
                    .addProperty(tileTypes)
                    .addProperty(connectivityMask)
                    .addFunction(getHistogram)
                    .addFunction(getTileType)
                    .build()
            ).build()
            .writeTo(codeGenerator, Dependencies(true, *symbols.mapNotNull { it.containingFile }.toTypedArray()))
    }

    private fun getRange(symbol: KSClassDeclaration): IntRange {
        val file = symbol.containingFile?.let { File(it.filePath) }
            ?: throw IllegalStateException("No file found for ${symbol.simpleName.asString()}")
        val text = file.readText()
        val name = symbol.simpleName.asString()
        val regex = Regex("""object\s+$name[\s\S]*?intRange[\s\S]*?=\s*(\d+)\s*\.\.\s*(\d+)""")
        val match = regex.find(text)
            ?: throw IllegalStateException("Could not parse intRange for $name in ${file.path}")
        val (start, end) = match.destructured
        return start.toInt()..end.toInt()
    }
}