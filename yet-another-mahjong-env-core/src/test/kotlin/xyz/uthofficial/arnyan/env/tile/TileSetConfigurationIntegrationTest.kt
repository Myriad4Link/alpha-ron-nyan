package xyz.uthofficial.arnyan.env.tile

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.slf4j.LoggerFactory
import xyz.uthofficial.arnyan.env.error.WallError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.tile.StandardTileType.*
import xyz.uthofficial.arnyan.env.tile.dsl.and
import xyz.uthofficial.arnyan.env.tile.dsl.of

class TileSetConfigurationIntegrationTest : FunSpec({
    fun buildNormalTileWall(): StandardTileWall = (TileSetConfiguration().setGroup {
        (1..9 of (Sou and Man and Pin)) +
                (1..4 of Wind) +
                (1..3 of Dragon)
    } repeatFor 4).build().getOrThrow()

    val logger = LoggerFactory.getLogger(this::class.java)

    test("should generate correct tile wall") {
        val tileWall = buildNormalTileWall()

        tileWall.tileWall.take(3) shouldContainInOrder listOf(
            Tile(Sou, 1), Tile(Sou, 2), Tile(Sou, 3)
        )

        tileWall.size shouldBe 136
    }

    test("draw should act correctly") {
        val tileWall = buildNormalTileWall()

        val beforeDraw = tileWall.tileWall.toMutableList()
        val result = tileWall.draw(13)

        result.shouldBeInstanceOf<Result.Success<List<Tile>>>()
        val value = result.value

        value.size shouldBe 13
        value shouldBeEqual List(13) { beforeDraw.removeLast() }
        logger.info("Chosen: {}", value)
    }

    test("should shuffle correctly") {
        val tileWall = buildNormalTileWall()

        val beforeShuffle = tileWall.tileWall.toList()
        tileWall.shuffle()
        tileWall.tileWall shouldNotBeEqual beforeShuffle
        logger.info("Shuffled: {}", tileWall.tileWall)
    }

    test("should support combinator logic for tile types") {
        val wall = TileSetConfiguration().setGroup {
            1..2 of (Man and Pin)
        }.build().getOrThrow()

        wall.size shouldBe 4
        wall.tileWall.count { it.tileType == Man } shouldBe 2
        wall.tileWall.count { it.tileType == Pin } shouldBe 2
    }

    test("repeatFor should respect order of operations") {
        val wall = TileSetConfiguration()
            .setGroup { 1..1 of Man }
            .repeatFor(2)
            .setGroup { 1..1 of Pin }
            .build()
            .getOrThrow()

        wall.size shouldBe 3
        wall.tileWall.count { it.tileType == Man } shouldBe 2
        wall.tileWall.count { it.tileType == Pin } shouldBe 1
    }

    test("draw should return failure if not enough tiles") {
        val result = TileSetConfiguration().setGroup { 1..1 of Man }.build().getOrThrow().draw(2)
        result.shouldBeInstanceOf<Result.Failure<WallError>>()
        result.error.shouldBeInstanceOf<WallError.NotEnoughTiles>()
    }

    test("deal should distribute tiles to players correctly") {
        val wall = (TileSetConfiguration().setGroup {
            1..9 of Man
        } repeatFor 4).build().getOrThrow()

        val p1 = Player()
        val p2 = Player()
        val players = listOf(p1, p2)

        val initialSize = wall.size
        val dealAmount = 13

        ((wall deal dealAmount) randomlyTo players).shouldBeInstanceOf<Result.Success<Unit>>()

        p1.hand.size shouldBe dealAmount
        p2.hand.size shouldBe dealAmount
        wall.size shouldBe initialSize - (dealAmount * 2)

        logger.info("P1 hand: {}\nP2 hand: {}\nWall: {}", p1.hand, p2.hand, wall.tileWall)
    }

    test("should correctly configure red doras for single type") {
        val wall = ((TileSetConfiguration().setGroup {
            1..9 of Man
        } repeatFor 4)
            .whereEvery { Man and Sou } has 1 redDoraOn 5)
            .build()
            .getOrThrow()

        wall.tileWall.filter { it.tileType == Man && it.value == 5 && it.isAka }.size shouldBe 1

        wall.tileWall.filter { it.tileType == Man && it.value == 5 && !it.isAka }.size shouldBe 3

        logger.info("Wall with red dora Pins: {}", wall.tileWall)
    }

    test("should correctly configure red doras for multiple types") {
        val wall = ((TileSetConfiguration().setGroup {
            1..9 of (Man and Pin)
        } repeatFor 4)
            .whereEvery { Man and Pin } has 1 redDoraOn 5)
            .build()
            .getOrThrow()

        wall.tileWall.count { it.tileType == Man && it.isAka } shouldBe 1

        wall.tileWall.count { it.tileType == Pin && it.isAka } shouldBe 1

        logger.info("Wall with red dora Pins and Mans: {}", wall.tileWall)
    }
})