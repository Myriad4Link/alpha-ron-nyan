package xyz.uthofficial.arnyan.env.utils.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.TileType

@OptIn(ExperimentalCompilerApi::class)
class KSPProcessorTest : AnnotationSpec() {

    class TestProcessorProvider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return KSPProcessor(environment.codeGenerator, environment.logger)
        }
    }

    @Test
    fun `should generate registry for valid annotated object implementing TileType`() {
        val source = SourceFile.kotlin(
            "TestTile.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
            import xyz.uthofficial.arnyan.env.tile.TileType

            @RegisterTileType
            object MyTile : TileType {
                override val intRange: IntRange = 0..0
            }
            """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            useKsp2()
            symbolProcessorProviders = mutableListOf(TestProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }

        val result = compilation.compile()

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK

        val registryClass = result.classLoader.loadClass("xyz.uthofficial.arnyan.env.generated.TileTypeRegistry")
        val tileTypesProp = registryClass.getDeclaredMethod("getTileTypes")
        val tileTypes = tileTypesProp.invoke(registryClass.kotlin.objectInstance) as List<*>

        tileTypes.size shouldBe 1
        tileTypes[0]!!.javaClass.name shouldBe "test.pkg.MyTile"

        val getIndexMethod = registryClass.getMethod("getIndex", TileType::class.java)
        val myTileClass = result.classLoader.loadClass("test.pkg.MyTile")
        val myTileInstance = myTileClass.getField("INSTANCE").get(null)

        val indexResult = getIndexMethod.invoke(registryClass.kotlin.objectInstance, myTileInstance)
        
        (indexResult is Result.Success<*>) shouldBe true
        (indexResult as Result.Success<*>).value shouldBe 0
    }

    @Test
    fun `should fail when annotated element is not an object`() {
        val source = SourceFile.kotlin(
            "InvalidTile.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
            import xyz.uthofficial.arnyan.env.tile.TileType

            @RegisterTileType
            class MyClassTile : TileType {
                override fun compareTo(other: TileType): Int = 0
            }
            """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            useKsp2()
            symbolProcessorProviders = mutableListOf(TestProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "@RegisterTileType can only applied to objects"
    }

    @Test
    fun `should fail when annotated object does not implement TileType`() {
        val source = SourceFile.kotlin(
            "InvalidObject.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType

            @RegisterTileType
            object NotATile
            """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            useKsp2()
            symbolProcessorProviders = mutableListOf(TestProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "must be implementing TileType"
    }
}