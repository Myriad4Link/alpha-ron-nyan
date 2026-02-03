package xyz.uthofficial.arnyan.env.wind

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.wind.StandardWind.*

class StandardRoundWindCycleTest : FunSpec({
    val standardCycle = StandardRoundWindCycle.fromMap(
        mapOf(
            EAST to 4,
            SOUTH to 4,
            WEST to 4,
            NORTH to 4
        )
    ).getOrThrow()

    test("startRound should be East 1 honba 0") {
        standardCycle.startRoundRotationStatus shouldBe RoundRotationStatus(EAST, 1, 0)
    }

    test("nextRound should advance round number within same wind") {
        val roundRotationStatus1 = RoundRotationStatus(EAST, 1, 0)
        val round2 = standardCycle.nextRound(roundRotationStatus1).getOrThrow()
        round2 shouldBe RoundRotationStatus(EAST, 2, 0)
    }

    test("nextRound should advance to next wind when round number exceeds wind rounds") {
        val roundRotationStatus4 = RoundRotationStatus(EAST, 4, 0)
        val next = standardCycle.nextRound(roundRotationStatus4).getOrThrow()
        next shouldBe RoundRotationStatus(SOUTH, 1, 0)
    }

    test("nextRound should reset honba to 0") {
        val roundRotationStatus = RoundRotationStatus(EAST, 1, 5)
        val next = standardCycle.nextRound(roundRotationStatus).getOrThrow()
        next.honba shouldBe 0
    }

    test("nextHonba should increment honba and keep same wind and number") {
        val roundRotationStatus = RoundRotationStatus(EAST, 2, 3)
        val next = standardCycle.nextHonba(roundRotationStatus)
        next shouldBe RoundRotationStatus(EAST, 2, 4)
    }

    test("nextRound should fail when wind not in cycle") {
        val invalidRoundRotationStatus = RoundRotationStatus(NORTH, 5, 0) // NORTH is in cycle but round 5 is invalid
        val result = standardCycle.nextRound(invalidRoundRotationStatus)
        result.shouldBeInstanceOf<Result.Failure<ConfigurationError.RoundWindConfigurationError.RoundNumberOutOfRange>>()
    }

    test("nextRound should fail when no next round beyond total") {
        val lastRoundRotationStatus = RoundRotationStatus(NORTH, 4, 0)
        val result = standardCycle.nextRound(lastRoundRotationStatus)
        result.shouldBeInstanceOf<Result.Failure<ConfigurationError.RoundWindConfigurationError.NoNextRoundBeyondTotal>>()
    }

    test("cycle with custom wind order should respect sequence") {
        val customCycle = StandardRoundWindCycle.fromMap(
            mapOf(
                SOUTH to 2,
                EAST to 3
            )
        ).getOrThrow()

        val start = customCycle.startRoundRotationStatus
        start shouldBe RoundRotationStatus(SOUTH, 1, 0)

        val next1 = customCycle.nextRound(start).getOrThrow()
        next1 shouldBe RoundRotationStatus(SOUTH, 2, 0)

        val next2 = customCycle.nextRound(next1).getOrThrow()
        next2 shouldBe RoundRotationStatus(EAST, 1, 0)
    }
})