package xyz.uthofficial.arnyan.env.match.actions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.match.*
import xyz.uthofficial.arnyan.env.match.DummyPlayer
import xyz.uthofficial.arnyan.env.tile.*
import xyz.uthofficial.arnyan.env.wind.StandardWind.EAST
import xyz.uthofficial.arnyan.env.yaku.WinningMethod
import xyz.uthofficial.arnyan.env.yaku.YakuContext

class ActionHelpersTest : FunSpec({

    context("index() function") {
        test("should return correct index for Dragon tiles") {
            val whiteDragon = Tile(Dragon, 1, false)
            val greenDragon = Tile(Dragon, 2, false)
            val redDragon = Tile(Dragon, 3, false)

            whiteDragon.index() shouldBe 0
            greenDragon.index() shouldBe 1
            redDragon.index() shouldBe 2
        }

        test("should return correct index for Sou tiles") {
            val sou1 = Tile(Sou, 1, false)
            val sou5 = Tile(Sou, 5, false)
            val sou9 = Tile(Sou, 9, false)

            sou1.index() shouldBe 21
            sou5.index() shouldBe 25
            sou9.index() shouldBe 29
        }

        test("should return correct index for Wind tiles") {
            val east = Tile(Wind, 1, false)
            val south = Tile(Wind, 2, false)
            val west = Tile(Wind, 3, false)
            val north = Tile(Wind, 4, false)

            east.index() shouldBe 30
            south.index() shouldBe 31
            west.index() shouldBe 32
            north.index() shouldBe 33
        }

        test("should return correct index for Man tiles") {
            val man1 = Tile(Man, 1, false)
            val man5 = Tile(Man, 5, false)
            val man9 = Tile(Man, 9, false)

            man1.index() shouldBe 3
            man5.index() shouldBe 7
            man9.index() shouldBe 11
        }

        test("should return correct index for Pin tiles") {
            val pin1 = Tile(Pin, 1, false)
            val pin5 = Tile(Pin, 5, false)
            val pin9 = Tile(Pin, 9, false)

            pin1.index() shouldBe 12
            pin5.index() shouldBe 16
            pin9.index() shouldBe 20
        }
    }

    context("findSequenceTiles() function") {
        test("should find sequence tiles for 1-2-3 pattern") {
            val hand = listOf(
                Tile(Man, 1, false),
                Tile(Man, 3, false),
                Tile(Man, 5, false)
            )
            val subject = Tile(Man, 2, false)

            val result = findSequenceTiles(hand, subject)

            result.shouldNotBeNull()
            result.first shouldBe Tile(Man, 1, false)
            result.second shouldBe Tile(Man, 3, false)
        }

        test("should find sequence tiles for 7-8-9 pattern") {
            val hand = listOf(
                Tile(Pin, 7, false),
                Tile(Pin, 9, false)
            )
            val subject = Tile(Pin, 8, false)

            val result = findSequenceTiles(hand, subject)

            result.shouldNotBeNull()
            result.first shouldBe Tile(Pin, 7, false)
            result.second shouldBe Tile(Pin, 9, false)
        }

        test("should return null with out-of-bounds pattern") {
            val hand = listOf(
                Tile(Man, 1, false),
                Tile(Man, 2, false)
            )
            val subject = Tile(Man, 9, false)

            val result = findSequenceTiles(hand, subject)

            result.shouldBeNull()
        }

        test("should return null with insufficient tiles") {
            val hand = listOf(
                Tile(Sou, 5, false)
            )
            val subject = Tile(Sou, 4, false)

            val result = findSequenceTiles(hand, subject)

            result.shouldBeNull()
        }

        test("should return null when tiles are from different suits") {
            val hand = listOf(
                Tile(Man, 1, false),
                Tile(Pin, 3, false)
            )
            val subject = Tile(Man, 2, false)

            val result = findSequenceTiles(hand, subject)

            result.shouldBeNull()
        }
    }

    context("findMatchingTiles() function") {
        test("should find matching tiles for pon") {
            val hand = listOf(
                Tile(Man, 5, false),
                Tile(Man, 5, false),
                Tile(Man, 3, false)
            )
            val subject = Tile(Man, 5, false)

            val result = findMatchingTiles(hand, subject)

            result.shouldNotBeNull()
            result.first shouldBe Tile(Man, 5, false)
            result.second shouldBe Tile(Man, 5, false)
        }

        test("should return null with insufficient matches") {
            val hand = listOf(
                Tile(Pin, 7, false),
                Tile(Pin, 8, false)
            )
            val subject = Tile(Pin, 7, false)

            val result = findMatchingTiles(hand, subject)

            result.shouldBeNull()
        }

        test("should return null when no matching tiles") {
            val hand = listOf(
                Tile(Sou, 1, false),
                Tile(Sou, 2, false)
            )
            val subject = Tile(Sou, 9, false)

            val result = findMatchingTiles(hand, subject)

            result.shouldBeNull()
        }
    }

    context("isCompleteHand() function") {
        test("should return true for complete hand without subject") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            )

            isCompleteHand(hand) shouldBe true
        }

        test("should return true for complete hand with subject") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            )
            val subject = Tile(Pin, 4, false)

            isCompleteHand(hand, subject) shouldBe true
        }

        test("should return false for incomplete hand") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false),
                Tile(Sou, 6, false), Tile(Sou, 6, false)
            )

            isCompleteHand(hand) shouldBe false
        }
    }

    context("resolvePartitions() function") {
        test("should return partitions for complete hand") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            )

            val partitions = resolvePartitions(hand)

            partitions.shouldNotBeEmpty()
        }

        test("should return partitions for complete hand with subject") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            )
            val subject = Tile(Pin, 4, false)

            val partitions = resolvePartitions(hand, subject)

            partitions.shouldNotBeEmpty()
        }

    }

    context("computeMaxHan() function") {
        test("should return 0 for empty partitions") {
            val yakuConfig = createSimpleYakuConfiguration()
            val context = YakuContext(
                seatWind = EAST,
                roundWind = EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 1, false),
                winningMethod = WinningMethod.RON
            )

            val maxHan = computeMaxHan(yakuConfig, context, emptyList())

            maxHan shouldBe 0
        }

        test("should compute max han for valid partitions") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            )
            val partitions = resolvePartitions(hand)
            val yakuConfig = createSimpleYakuConfiguration()
            val context = YakuContext(
                seatWind = EAST,
                roundWind = EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Sou, 5, false),
                winningMethod = WinningMethod.TSUMO
            )

            val maxHan = computeMaxHan(yakuConfig, context, partitions)

            maxHan shouldBeGreaterThan 0
        }
    }

    context("computeBestPartition() function") {
        test("should return null for empty partitions") {
            val yakuConfig = createSimpleYakuConfiguration()
            val context = YakuContext(
                seatWind = EAST,
                roundWind = EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Man, 1, false),
                winningMethod = WinningMethod.RON
            )

            val result = computeBestPartition(yakuConfig, context, emptyList())

            result.shouldBeNull()
        }

        test("should return best partition with max han") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            )
            val partitions = resolvePartitions(hand)
            val yakuConfig = createSimpleYakuConfiguration()
            val context = YakuContext(
                seatWind = EAST,
                roundWind = EAST,
                isOpenHand = false,
                isRiichiDeclared = false,
                winningTile = Tile(Sou, 5, false),
                winningMethod = WinningMethod.TSUMO
            )

            val result = computeBestPartition(yakuConfig, context, partitions)

            result.shouldNotBeNull()
            result!!.first.shouldNotBeNull()
            result.second shouldBeGreaterThan 0
        }
    }

    context("tileGroupToMentsu() function") {
        test("should create Toitsu for 2 identical tiles") {
            val tiles = listOf(
                Tile(Man, 5, false),
                Tile(Man, 5, false)
            )

            val mentsu = tileGroupToMentsu(tiles, isOpen = false)

            mentsu.tileCount shouldBe 2
            mentsu.isOpen shouldBe false
        }

        test("should create Koutsu for 3 identical tiles") {
            val tiles = listOf(
                Tile(Pin, 7, false),
                Tile(Pin, 7, false),
                Tile(Pin, 7, false)
            )

            val mentsu = tileGroupToMentsu(tiles, isOpen = true)

            mentsu.tileCount shouldBe 3
            mentsu.isOpen shouldBe true
        }

        test("should create Shuntsu for 3 sequential tiles") {
            val tiles = listOf(
                Tile(Sou, 4, false),
                Tile(Sou, 5, false),
                Tile(Sou, 6, false)
            )

            val mentsu = tileGroupToMentsu(tiles, isOpen = false)

            mentsu.tileCount shouldBe 3
            mentsu.isOpen shouldBe false
        }

        test("should create Kantsu for 4 identical tiles") {
            val tiles = listOf(
                Tile(Man, 9, false),
                Tile(Man, 9, false),
                Tile(Man, 9, false),
                Tile(Man, 9, false)
            )

            val mentsu = tileGroupToMentsu(tiles, isOpen = true)

            mentsu.tileCount shouldBe 4
            mentsu.isOpen shouldBe true
        }

        test("should error for invalid pair") {
            val tiles = listOf(
                Tile(Man, 1, false),
                Tile(Man, 2, false)
            )

            shouldThrow<IllegalStateException> {
                tileGroupToMentsu(tiles, isOpen = false)
            }
        }

        test("should error for invalid triplet") {
            val tiles = listOf(
                Tile(Pin, 1, false),
                Tile(Pin, 1, false),
                Tile(Pin, 2, false)
            )

            shouldThrow<IllegalStateException> {
                tileGroupToMentsu(tiles, isOpen = false)
            }
        }

        test("should error for non-sequential shuntsu") {
            val tiles = listOf(
                Tile(Sou, 1, false),
                Tile(Sou, 3, false),
                Tile(Sou, 5, false)
            )

            shouldThrow<IllegalStateException> {
                tileGroupToMentsu(tiles, isOpen = false)
            }
        }

        test("should error for invalid quadruplet") {
            val tiles = listOf(
                Tile(Man, 5, false),
                Tile(Man, 5, false),
                Tile(Man, 5, false),
                Tile(Man, 6, false)
            )

            shouldThrow<IllegalStateException> {
                tileGroupToMentsu(tiles, isOpen = false)
            }
        }

        test("should error for invalid tile group size") {
            val tiles = listOf(
                Tile(Man, 1, false)
            )

            shouldThrow<IllegalStateException> {
                tileGroupToMentsu(tiles, isOpen = false)
            }
        }
    }

    context("computeScoringStateChanges() function") {
        test("should return empty when actor has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val actor = DummyPlayer()
            actor.seat = null
            val subject = Tile(Man, 1, false)
            val partitions = resolvePartitions(listOf(subject), subject)

            val result = computeScoringStateChanges(
                observation = match.observation,
                actor = actor,
                subject = subject,
                winningMethod = WinningMethod.RON,
                partitions = partitions,
                discardingSeat = EAST
            )

            result.shouldBeEmpty()
        }

        test("should compute scoring changes with riichi sticks") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.seat = EAST
            eastPlayer.score = 50000

            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            )
            val partitions = resolvePartitions(hand)
            val winningTile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.riichiSticks = 2
            state.honbaSticks = 0
            val obs = state.toObservation()

            val result = computeScoringStateChanges(
                observation = obs,
                actor = eastPlayer,
                subject = winningTile,
                winningMethod = WinningMethod.TSUMO,
                partitions = partitions
            )

            result.shouldNotBeEmpty()
        }

        test("should compute scoring changes with honba sticks") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.seat = EAST
            eastPlayer.score = 50000

            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            )
            val partitions = resolvePartitions(hand)
            val winningTile = Tile(Sou, 5, false)

            val state = match.observation.toState()
            state.riichiSticks = 0
            state.honbaSticks = 3
            val obs = state.toObservation()

            val result = computeScoringStateChanges(
                observation = obs,
                actor = eastPlayer,
                subject = winningTile,
                winningMethod = WinningMethod.TSUMO,
                partitions = partitions
            )

            result.shouldNotBeEmpty()
        }
    }

    context("canWin() function") {
        test("should return false when actor has no seat") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val actor = DummyPlayer()
            actor.seat = null
            val subject = Tile(Man, 1, false)

            val result = canWin(match.observation, actor, subject, WinningMethod.RON)

            result shouldBe false
        }

        test("should return true for winning hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            ))
            val subject = Tile(Sou, 5, false)

            val result = canWin(match.observation, eastPlayer, subject, WinningMethod.TSUMO)

            result shouldBe true
        }

        test("canWin should return false for incomplete hand") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.closeHand.addAll(listOf(
                Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false)
            ))
            val subject = Tile(Man, 3, false)

            val result = canWin(match.observation, eastPlayer, subject, WinningMethod.RON)

            result shouldBe false
        }

        test("should return false for hand with no yaku") {
            val players = List(3) { DummyPlayer() }
            val wallTiles = TestTileFactory.create40Wall()
            val match = MatchBuilder().withWallTiles(wallTiles).withCustomPlayers(*players.toTypedArray()).build()
            match.start().shouldBeSuccess()

            val eastPlayer = getPlayerBySeat(players, EAST)
            eastPlayer.closeHand.clear()
            eastPlayer.openHand.clear()
            eastPlayer.openHand.add(listOf(Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false)))
            eastPlayer.openHand.add(listOf(Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false)))
            eastPlayer.openHand.add(listOf(Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false)))
            eastPlayer.closeHand.addAll(listOf(
                Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false), Tile(Sou, 5, false)
            ))
            val subject = Tile(Pin, 4, false)

            val result = canWin(match.observation, eastPlayer, subject, WinningMethod.RON)

            result shouldBe false
        }
    }

    context("isInTenpai() function") {
        test("should return true for tenpai hand") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false)
            )
            val drawnTile = Tile(Sou, 5, false)

            val result = isInTenpai(hand, drawnTile)

            result shouldBe true
        }

        test("should return false for non-tenpai hand") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false)
            )
            val drawnTile = Tile(Pin, 4, false)

            val result = isInTenpai(hand, drawnTile)

            result shouldBe false
        }
    }

    context("getTenpaiWaitingTiles() function") {
        test("should return waiting tiles for tenpai hand") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false), Tile(Man, 2, false),
                Tile(Pin, 3, false), Tile(Pin, 3, false), Tile(Pin, 3, false),
                Tile(Pin, 4, false), Tile(Pin, 4, false), Tile(Pin, 4, false),
                Tile(Sou, 5, false)
            )

            val result = getTenpaiWaitingTiles(hand)

            result.shouldNotBeEmpty()
        }

        test("should return empty for non-tenpai hand") {
            val hand = listOf(
                Tile(Man, 1, false), Tile(Man, 1, false),
                Tile(Man, 2, false), Tile(Man, 2, false)
            )

            val result = getTenpaiWaitingTiles(hand)

            result.shouldBeEmpty()
        }
    }
})
