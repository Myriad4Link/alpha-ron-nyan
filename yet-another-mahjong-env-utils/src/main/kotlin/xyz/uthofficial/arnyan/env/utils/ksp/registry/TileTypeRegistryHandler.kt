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
        val sortedSymbols = symbols.sortedBy { it.qualifiedName?.asString() ?: it.simpleName.asString() }
        val ranges = sortedSymbols.associateWith { getRange(it) }
        val offsets = mutableMapOf<KSClassDeclaration, Int>()
        var currentOffset = 0
        sortedSymbols.forEach {
            offsets[it] = currentOffset
            val range = ranges[it]!!
            currentOffset += (range.last - range.first + 1)
        }
        val totalSize = currentOffset
        val SIZE = PropertySpec.builder(
            "SIZE", Int::class.asClassName()
        ).addModifiers(KModifier.CONST)
         .initializer("%L", totalSize).build()
                val tileTypes = PropertySpec.builder(
                    "tileTypes", List::class.asClassName().parameterizedBy(
                        TileType::class.asClassName()
                    )
                ).initializer(
                    CodeBlock.builder().apply {
                        add("listOf(\n")
                        sortedSymbols.forEach { add("    %T,\n", it.toClassName()) }
                        add(")")
                    }.build()
                ).build()
        val maskBlock = CodeBlock.builder()
        maskBlock.add("intArrayOf(")
        sortedSymbols.forEachIndexed {
            index,
            symbol ->
            val range = ranges[symbol]!!
            val count = range.last - range.first + 1
            val isCont = isContinuous(symbol)
            val maskId = if (isCont) -(index + 1) else (index + 1)
            repeat(count) { maskBlock.add("%L, ", maskId) }
        }
        maskBlock.add(")")

        val connectivityMask = PropertySpec.builder(
            "connectivityMask", IntArray::class.asClassName()
        ).initializer(maskBlock.build()).build()

        val getHistogram = FunSpec.builder("getHistogram")
            .addParameter("hand", List::class.asClassName().parameterizedBy(Tile::class.asClassName()))
            .addParameter(
                ParameterSpec.builder("output", IntArray::class)
                    .defaultValue("%T(%N)", IntArray::class.asClassName(), "SIZE")
                    .build()
            )
            .returns(IntArray::class)
            .addStatement("output.fill(0)")
            .beginControlFlow("for (tile in hand)")
            .addStatement("val index = when (tile.tileType) {")
            .apply {
                sortedSymbols.forEach {
                    symbol ->
                    val range = ranges[symbol]!!
                    val offset = offsets[symbol]!!
                    val min = range.first
                    val adjustment = offset - min
                    addStatement("%T -> tile.value + (%L)", symbol.toClassName(), adjustment)
                }
            }
            .addStatement("else -> -1")
            .addStatement("}")
            .addStatement("if (index in 0 until SIZE) output[index]++")
            .endControlFlow()
            .addStatement("return output")
            .build()

        val getTileType = FunSpec.builder("getTileType")
            .addParameter("index", Int::class)
            .returns(TileType::class.asClassName())
            .beginControlFlow("return when(index)")
            .apply {
                sortedSymbols.forEach {
                    symbol ->
                    val range = ranges[symbol]!!
                    val offset = offsets[symbol]!!
                    val count = range.last - range.first + 1
                    val limit = offset + count
                    addStatement("in %L until %L -> %T", offset, limit, symbol.toClassName())
                }
            }
            .addStatement("else -> throw %T(%S)", IllegalArgumentException::class, "Index out of bounds")
            .endControlFlow()
            .build()

        // Pre-allocate segment arrays
        val segmentProperties = sortedSymbols.map { symbol ->
            val range = ranges[symbol]!!
            val offset = offsets[symbol]!!
            val count = range.last - range.first + 1
            PropertySpec.builder(
                "SEGMENT_${symbol.simpleName.asString().uppercase()}",
                IntArray::class
            )
                .addModifiers(KModifier.PRIVATE)
                .initializer("intArrayOf(%L, %L)", offset, count)
                .build()
        }

        val segmentNone = PropertySpec.builder("SEGMENT_NONE", IntArray::class)
            .addModifiers(KModifier.PRIVATE)
            .initializer("intArrayOf(-1, 0)")
            .build()

        val getSegment = FunSpec.builder("getSegment")
            .addParameter("tileType", TileType::class.asClassName())
            .returns(IntArray::class)
            .beginControlFlow("return when(tileType)")
            .apply {
                sortedSymbols.forEach { symbol ->
                    addStatement("%T -> SEGMENT_%L", symbol.toClassName(), symbol.simpleName.asString().uppercase())
                }
            }
            .addStatement("else -> SEGMENT_NONE")
            .endControlFlow()
            .build()

        FileSpec.builder(packageName, fileName)
            .addType(
                TypeSpec.objectBuilder(fileName).addModifiers(KModifier.FINAL)
                    .addProperty(tileTypes)
                    .addProperty(SIZE)
                    .addProperty(connectivityMask)
                    .addProperties(segmentProperties)
                    .addProperty(segmentNone)
                    .addFunction(getHistogram)
                    .addFunction(getTileType)
                    .addFunction(getSegment)
                    .build()
            ).build()
            .writeTo(codeGenerator, Dependencies(true, *sortedSymbols.mapNotNull { it.containingFile }.toTypedArray()))
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

    private fun isContinuous(symbol: KSClassDeclaration): Boolean {
        val file = symbol.containingFile?.let { File(it.filePath) }
            ?: return false
        val text = file.readText()
        val name = symbol.simpleName.asString()
        // Extract the block for this object
        val objectBlockRegex = Regex("""object\s+$name[\s\S]*?\{([\s\S]*?)\n\s*}""")
        val match = objectBlockRegex.find(text) ?: return false
        val body = match.groupValues[1]
        return body.contains(Regex("""isContinuous[\s\S]*?=\s*true"""))
    }
}