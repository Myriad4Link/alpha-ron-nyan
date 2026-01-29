package xyz.uthofficial.arnyan.env.utils.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import xyz.uthofficial.arnyan.env.tile.TileType
import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
import xyz.uthofficial.arnyan.env.utils.ksp.registry.TileTypeRegistryHandler
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType
import xyz.uthofficial.arnyan.env.utils.annotations.RegisterMentsuType
import xyz.uthofficial.arnyan.env.utils.ksp.registry.MentsuTypeRegistryHandler

class KSPProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val tileSymbols = resolver.getSymbolsWithAnnotation(RegisterTileType::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                when {
                    it.classKind != ClassKind.OBJECT -> {
                        logger.error("@${RegisterTileType::class.simpleName!!} can only applied to objects.", it)
                        false
                    }

                    it.getAllSuperTypes().none { superType ->
                        superType.declaration.qualifiedName?.asString() == TileType::class.qualifiedName
                    } -> {
                        logger.error("Objects with @${RegisterTileType::class.simpleName!!} must be implementing ${TileType::class.simpleName}.")
                        false
                    }

                    else -> true
                }
            }
            .toList()

        val mentsuSymbols = resolver.getSymbolsWithAnnotation(RegisterMentsuType::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                when {
                    it.classKind != ClassKind.OBJECT -> {
                        logger.error("@${RegisterMentsuType::class.simpleName!!} can only applied to objects.", it)
                        false
                    }

                    it.getAllSuperTypes().none { superType ->
                        superType.declaration.qualifiedName?.asString() == MentsuType::class.qualifiedName
                    } -> {
                        logger.error("Objects with @${RegisterMentsuType::class.simpleName!!} must be implementing ${MentsuType::class.simpleName}.")
                        false
                    }

                    else -> true
                }
            }
            .toList()

        if (tileSymbols.isNotEmpty()) TileTypeRegistryHandler(codeGenerator).generateRegistry(tileSymbols)
        if (mentsuSymbols.isNotEmpty()) MentsuTypeRegistryHandler(codeGenerator).generateRegistry(mentsuSymbols)

        return emptyList()
    }
}