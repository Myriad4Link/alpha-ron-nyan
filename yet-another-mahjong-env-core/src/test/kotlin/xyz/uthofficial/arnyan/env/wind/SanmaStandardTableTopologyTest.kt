package xyz.uthofficial.arnyan.env.wind

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.error.ConfigurationError
import xyz.uthofficial.arnyan.env.error.TopologyError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.wind.StandardWind.*

class SanmaStandardTableTopologyTest : FunSpec({
    val seatOrder = listOf(EAST, SOUTH, WEST)
    val topology = SanmaStandardTableTopology(seatOrder)

    test("should get correct Shimocha (next player)") {
        topology.getShimocha(EAST).getOrThrow() shouldBe SOUTH
        topology.getShimocha(SOUTH).getOrThrow() shouldBe WEST
        topology.getShimocha(WEST).getOrThrow() shouldBe EAST
    }

    test("should get correct Kamicha (previous player)") {
        topology.getKamicha(EAST).getOrThrow() shouldBe WEST
        topology.getKamicha(WEST).getOrThrow() shouldBe SOUTH
        topology.getKamicha(SOUTH).getOrThrow() shouldBe EAST
    }

    test("getToimen should always return failure in Sanma") {
        val result = topology.getToimen(EAST)
        result.shouldBeInstanceOf<Result.Failure<TopologyError>>()
        result.error.shouldBeInstanceOf<TopologyError.NoToimenAvailable>()
    }

    test("should return failure when wind is not in seat order") {
        val result = topology.getShimocha(NORTH)
        result.shouldBeInstanceOf<Result.Failure<TopologyError>>()
        result.error.shouldBeInstanceOf<TopologyError.WindNotInCycle>()
    }

    test("should validate seat order in configuration builder") {
        val emptyConfig = PlayerSeatWindRotationConfiguration()
        val emptyResult = emptyConfig.build()
        emptyResult.shouldBeInstanceOf<Result.Failure<ConfigurationError>>()
        emptyResult.error.shouldBeInstanceOf<ConfigurationError.SeatOrderConfigurationError.EmptySeatOrder>()

        val duplicateConfig = PlayerSeatWindRotationConfiguration().apply {
            EAST - EAST
        }
        val duplicateResult = duplicateConfig.build()
        duplicateResult.shouldBeInstanceOf<Result.Failure<ConfigurationError>>()
        duplicateResult.error.shouldBeInstanceOf<ConfigurationError.SeatOrderConfigurationError.DuplicateSeats>()
    }
})
