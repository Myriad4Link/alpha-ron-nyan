package xyz.uthofficial.arnyan.env.utils.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import xyz.uthofficial.arnyan.env.tile.TileType

@OptIn(ExperimentalCompilerApi::class)
class KSPProcessorTest : FunSpec({

    class TestProcessorProvider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return KSPProcessor(environment.codeGenerator, environment.logger)
        }
    }

    test("should generate registry for valid annotated object implementing TileType") {
        val source = SourceFile.kotlin(
            "TestTile.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
            import xyz.uthofficial.arnyan.env.tile.TileType

            @RegisterTileType
            object MyTile : TileType {
                override val intRange: IntRange = 1..1
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

        val tileClass = result.classLoader.loadClass("xyz.uthofficial.arnyan.env.tile.Tile")
        val myTileClass = result.classLoader.loadClass("test.pkg.MyTile")
        val myTileInstance = myTileClass.getField("INSTANCE").get(null)
        
        // Tile constructor: Tile(tileType: TileType, value: Int, isAka: Boolean = false)
        val tileConstructor = tileClass.constructors.first()
        val tileInstance = tileConstructor.newInstance(myTileInstance, 1, false)

        val getHistogramMethod = registryClass.getMethod("getHistogram", List::class.java)
        val histogram = getHistogramMethod.invoke(registryClass.kotlin.objectInstance, listOf(tileInstance)) as IntArray
        
        histogram.size shouldBe 1
        histogram[0] shouldBe 1
    }

    test("should verify registry reverse lookup and frequency table generation") {
        val source = SourceFile.kotlin(
            "MultiTypeTiles.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
            import xyz.uthofficial.arnyan.env.tile.TileType

            @RegisterTileType
            object TypeA : TileType {
                override val intRange: IntRange = 1..3
            }

            @RegisterTileType
            object TypeB : TileType {
                override val intRange: IntRange = 5..6
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
        val instance = registryClass.kotlin.objectInstance!!

        val tileClass = result.classLoader.loadClass("xyz.uthofficial.arnyan.env.tile.Tile")
        val typeAClass = result.classLoader.loadClass("test.pkg.TypeA")
        val typeBClass = result.classLoader.loadClass("test.pkg.TypeB")
        val typeA = typeAClass.getField("INSTANCE").get(null)
        val typeB = typeBClass.getField("INSTANCE").get(null)
        val tileCtor = tileClass.constructors.first()

        // getTileType(3) -> TypeB. (Indices: TypeA[0,1,2], TypeB[3,4])
        // TypeA size 3 (1,2,3). Offsets: [0, 3].
        
        val getTileTypeMethod = registryClass.getMethod("getTileType", Int::class.javaPrimitiveType)
        
        getTileTypeMethod.invoke(instance, 0) shouldBe typeA
        getTileTypeMethod.invoke(instance, 2) shouldBe typeA
        getTileTypeMethod.invoke(instance, 3) shouldBe typeB
        getTileTypeMethod.invoke(instance, 4) shouldBe typeB
        
        try {
            getTileTypeMethod.invoke(instance, 5)
        } catch (e: Exception) {
            e.cause shouldNotBe null
        }

        // Check Connectivity Mask
        val connectivityMaskProp = registryClass.getDeclaredMethod("getConnectivityMask")
        val connectivityMask = connectivityMaskProp.invoke(instance) as IntArray
        connectivityMask.size shouldBe 5 // 3 + 2
        
        // TypeA (indices 0,1,2) mask 1 (sorted first)
        connectivityMask[0] shouldBe 1
        connectivityMask[1] shouldBe 1
        connectivityMask[2] shouldBe 1
        // TypeB (indices 3,4) mask 2 (sorted second)
        connectivityMask[3] shouldBe 2
        connectivityMask[4] shouldBe 2

        // getHistogram
        // Create list of tiles
        // TypeA ranges 1..3. Value 2 -> Index 0 + (2 - 1) = 1.
        val t1 = tileCtor.newInstance(typeA, 2, false) 
        // TypeB ranges 5..6. Value 5 -> Index 3 + (5 - 5) = 3.
        val t2 = tileCtor.newInstance(typeB, 5, false)
        val list = listOf(t1, t2)
        
        val getHistogramMethod = registryClass.getMethod("getHistogram", List::class.java)
        val histogram = getHistogramMethod.invoke(instance, list) as IntArray
        
        histogram.size shouldBe 5
        histogram[0] shouldBe 0
        histogram[1] shouldBe 1
        histogram[2] shouldBe 0
        histogram[3] shouldBe 1
        histogram[4] shouldBe 0
    }

    test("should fail when annotated element is not an object") {
        val source = SourceFile.kotlin(
            "InvalidTile.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
            import xyz.uthofficial.arnyan.env.tile.TileType

            @RegisterTileType
            class MyClassTile : TileType {
                override val intRange: IntRange = 0..0
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

    test("should fail when annotated object does not implement TileType") {
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
})
