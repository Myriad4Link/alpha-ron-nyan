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
import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType

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

        val getHistogramMethod = registryClass.getMethod("getHistogram", List::class.java, IntArray::class.java)
        val histogram = getHistogramMethod.invoke(registryClass.kotlin.objectInstance, listOf(tileInstance), IntArray(1)) as IntArray
        
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
        
        val getHistogramMethod = registryClass.getMethod("getHistogram", List::class.java, IntArray::class.java)
        val histogram = getHistogramMethod.invoke(instance, list, IntArray(5)) as IntArray
        
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

    test("should return correct segment for tile types") {
        val source = SourceFile.kotlin(
            "SegmentTestTiles.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
            import xyz.uthofficial.arnyan.env.tile.TileType

            @RegisterTileType
            object SegmentTypeA : TileType {
                override val intRange: IntRange = 1..5
            }

            @RegisterTileType
            object SegmentTypeB : TileType {
                override val intRange: IntRange = 1..10
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
        
        val typeAClass = result.classLoader.loadClass("test.pkg.SegmentTypeA")
        val typeBClass = result.classLoader.loadClass("test.pkg.SegmentTypeB")
        val typeA = typeAClass.getField("INSTANCE").get(null)
        val typeB = typeBClass.getField("INSTANCE").get(null)
        val tileTypeClass = result.classLoader.loadClass("xyz.uthofficial.arnyan.env.tile.TileType")

        val getSegmentMethod = registryClass.getMethod("getSegment", tileTypeClass)
        
        // Sorted: SegmentTypeA (A comes before B in 'test.pkg.SegmentTypeA' vs 'test.pkg.SegmentTypeB')
        // TypeA: size 5. Offset 0.
        // TypeB: size 10. Offset 5.

        val segmentA = getSegmentMethod.invoke(instance, typeA) as IntArray
        segmentA[0] shouldBe 0
        segmentA[1] shouldBe 5

        val segmentB = getSegmentMethod.invoke(instance, typeB) as IntArray
        segmentB[0] shouldBe 5
        segmentB[1] shouldBe 10
    }

    test("should use provided buffer for histogram generation") {
        val source = SourceFile.kotlin(
            "BufferTestTiles.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
            import xyz.uthofficial.arnyan.env.tile.TileType

            @RegisterTileType
            object BufferType : TileType {
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
        val instance = registryClass.kotlin.objectInstance!!
        val tileClass = result.classLoader.loadClass("xyz.uthofficial.arnyan.env.tile.Tile")
        val bufferTypeClass = result.classLoader.loadClass("test.pkg.BufferType")
        val bufferType = bufferTypeClass.getField("INSTANCE").get(null)
        val tileCtor = tileClass.constructors.first()

        val t1 = tileCtor.newInstance(bufferType, 1, false)
        val list = listOf(t1)

        val getHistogramMethod = registryClass.getMethod("getHistogram", List::class.java, IntArray::class.java)
        
        // Size 1.
        val buffer = IntArray(1)
        buffer[0] = 99 // Set dirty

        getHistogramMethod.invoke(instance, list, buffer)

        buffer[0] shouldBe 1
    }

    test("should generate negative mask for continuous tile types") {
        val source = SourceFile.kotlin(
            "ContinuousTiles.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
            import xyz.uthofficial.arnyan.env.tile.TileType

            @RegisterTileType
            object ContinuousType : TileType {
                override val intRange: IntRange = 1..3
                override val isContinuous: Boolean = true
            }

            @RegisterTileType
            object NormalType : TileType {
                override val intRange: IntRange = 1..2
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

        val connectivityMaskProp = registryClass.getDeclaredMethod("getConnectivityMask")
        val connectivityMask = connectivityMaskProp.invoke(instance) as IntArray
        
        // Sorted by qualified name: 
        // 1. test.pkg.ContinuousType (size 3) -> maskId = -(0+1) = -1
        // 2. test.pkg.NormalType (size 2) -> maskId = (1+1) = 2
        
        connectivityMask.size shouldBe 5
        connectivityMask[0] shouldBe -1
        connectivityMask[1] shouldBe -1
        connectivityMask[2] shouldBe -1
        connectivityMask[3] shouldBe 2
        connectivityMask[4] shouldBe 2
    }

    test("should generate correct yaochuhai indices for honors and suits") {
        val source = SourceFile.kotlin(
            "YaochuhaiTestTiles.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterTileType
            import xyz.uthofficial.arnyan.env.tile.TileType

            @RegisterTileType
            object HonorType : TileType {
                override val intRange: IntRange = 1..4
            }

            @RegisterTileType
            object SuitType : TileType {
                override val intRange: IntRange = 1..9
                override val isContinuous: Boolean = true
            }

            @RegisterTileType
            object AnotherSuit : TileType {
                override val intRange: IntRange = 1..5
                override val isContinuous: Boolean = true
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

        val yaochuhaiIndicesProp = registryClass.getDeclaredMethod("getYaochuhaiIndices")
        val yaochuhaiIndices = yaochuhaiIndicesProp.invoke(instance) as IntArray

        // Expected indices: AnotherSuit (offset 0, count 5) -> first 0, last 4
        // HonorType (offset 5, count 4) -> all 5,6,7,8
        // SuitType (offset 9, count 9) -> first 9, last 17
        // Sorted: 0,4,5,6,7,8,9,17
        yaochuhaiIndices.size shouldBe 8
        yaochuhaiIndices[0] shouldBe 0
        yaochuhaiIndices[1] shouldBe 4
        yaochuhaiIndices[2] shouldBe 5
        yaochuhaiIndices[3] shouldBe 6
        yaochuhaiIndices[4] shouldBe 7
        yaochuhaiIndices[5] shouldBe 8
        yaochuhaiIndices[6] shouldBe 9
        yaochuhaiIndices[7] shouldBe 17
    }

    test("should generate registry for valid annotated object implementing MentsuType") {
        val source = SourceFile.kotlin(
            "TestMentsu.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterMentsuType
            import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType

            @RegisterMentsuType
            object MyMentsu : MentsuType
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

        val registryClass = result.classLoader.loadClass("xyz.uthofficial.arnyan.env.generated.MentsuTypeRegistry")
        val mentsuTypesProp = registryClass.getDeclaredMethod("getMentsuTypes")
        val mentsuTypes = mentsuTypesProp.invoke(registryClass.kotlin.objectInstance) as List<*>

        mentsuTypes.size shouldBe 1
        mentsuTypes[0]!!.javaClass.name shouldBe "test.pkg.MyMentsu"

        val getIndexMethod = registryClass.getMethod("getIndex", MentsuType::class.java)
        val myMentsuClass = result.classLoader.loadClass("test.pkg.MyMentsu")
        val myMentsuInstance = myMentsuClass.getField("INSTANCE").get(null)
        val index = getIndexMethod.invoke(registryClass.kotlin.objectInstance, myMentsuInstance) as Int
        index shouldBe 0

        val getMentsuTypeMethod = registryClass.getMethod("getMentsuType", Int::class.javaPrimitiveType)
        val retrieved = getMentsuTypeMethod.invoke(registryClass.kotlin.objectInstance, 0)
        retrieved shouldBe myMentsuInstance
    }

    test("should fail when annotated element is not an object for MentsuType") {
        val source = SourceFile.kotlin(
            "InvalidMentsu.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterMentsuType
            import xyz.uthofficial.arnyan.env.yaku.resolver.MentsuType

            @RegisterMentsuType
            class MyClassMentsu : MentsuType
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
        result.messages shouldContain "@RegisterMentsuType can only applied to objects"
    }

    test("should fail when annotated object does not implement MentsuType") {
        val source = SourceFile.kotlin(
            "InvalidObject.kt",
            """
            package test.pkg

            import xyz.uthofficial.arnyan.env.utils.annotations.RegisterMentsuType

            @RegisterMentsuType
            object NotAMentsu
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
        result.messages shouldContain "must be implementing MentsuType"
    }
})
