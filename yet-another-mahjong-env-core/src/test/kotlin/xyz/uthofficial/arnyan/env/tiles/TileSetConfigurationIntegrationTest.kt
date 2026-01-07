package xyz.uthofficial.arnyan.env.tiles

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import org.slf4j.LoggerFactory
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.player.PlayerList
import xyz.uthofficial.arnyan.env.tiles.TileType.*

class TileSetConfigurationIntegrationTest : FunSpec({
    fun buildNormalTileWall(): TileWall = (TileSetConfiguration().setGroup {
        1..9 of (SOU and MAN and PIN)
        1..4 of WIND
        1..3 of DRAGON
    } repeatFor 4).build()

    val logger = LoggerFactory.getLogger(this::class.java)

    test("should generate correct tile wall") {
        val tileWall = buildNormalTileWall()

        tileWall.tileWall.take(3) shouldContainInOrder listOf(
            Tile(SOU, 1), Tile(SOU, 2), Tile(SOU, 3)
        )

        tileWall.size shouldBe 136
    }

    test("draw should act correctly") {
        val tileWall = buildNormalTileWall()

        val beforeDraw = tileWall.tileWall.toMutableList()
        tileWall.draw(13).shouldBeSuccess {
            it.size shouldBe 13
            it shouldBeEqual List(13) { beforeDraw.removeLast() }
            logger.info("Chosen: {}", it)
        }
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
            1..2 of (MAN and PIN)
        }.build()

        wall.size shouldBe 4
        wall.tileWall.count { it.tileType == MAN } shouldBe 2
        wall.tileWall.count { it.tileType == PIN } shouldBe 2
    }

    test("repeatFor should respect order of operations") {
        val wall = TileSetConfiguration()
            .setGroup { 1..1 of MAN }
            .repeatFor(2)
            .setGroup { 1..1 of PIN }
            .build()

        wall.size shouldBe 3
        wall.tileWall.count { it.tileType == MAN } shouldBe 2
        wall.tileWall.count { it.tileType == PIN } shouldBe 1
    }

    test("draw should return failure if not enough tiles") {
        val wall = TileSetConfiguration().setGroup { 1..1 of MAN }.build()
        wall.draw(2).shouldBeFailure<NoSuchElementException>()
    }

    test("deal should distribute tiles to players correctly") {
        val wall = (TileSetConfiguration().setGroup {
            1..9 of MAN
        } repeatFor 4).build()

        val p1 = Player()
        val p2 = Player()
        val players = listOf(p1, p2)
        val playerList = object : PlayerList {
            override fun forEach(block: (Player) -> Unit) = players.forEach(block)
        }

        val initialSize = wall.size
        val dealAmount = 13

        ((wall deal dealAmount) randomlyTo playerList).shouldBeSuccess()

        p1.hand.size shouldBe dealAmount
        p2.hand.size shouldBe dealAmount
        wall.size shouldBe initialSize - (dealAmount * 2)
    }
})