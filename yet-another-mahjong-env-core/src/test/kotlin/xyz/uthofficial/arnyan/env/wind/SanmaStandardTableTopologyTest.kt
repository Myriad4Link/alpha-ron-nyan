package xyz.uthofficial.arnyan.env.wind

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import xyz.uthofficial.arnyan.env.wind.Wind.*

class SanmaStandardTableTopologyTest : FunSpec({
    val seatOrder = listOf(EAST, SOUTH, WEST)
    val topology = SanmaStandardTableTopology(seatOrder)

    test("should get correct Shimocha (next player)") {
        topology.getShimocha(EAST) shouldBe SOUTH
        topology.getShimocha(SOUTH) shouldBe WEST
        topology.getShimocha(WEST) shouldBe EAST
    }

    test("should get correct Kamicha (previous player)") {
        topology.getKamicha(EAST) shouldBe WEST
        topology.getKamicha(WEST) shouldBe SOUTH
        topology.getKamicha(SOUTH) shouldBe EAST
    }

    test("getToimen should always throw exception in Sanma") {
        shouldThrow<UnsupportedOperationException> {
            topology.getToimen(EAST)
        }
    }

    test("should throw exception when wind is not in seat order") {
        shouldThrow<IllegalArgumentException> {
            topology.getShimocha(NORTH)
        }
    }

    test("should validate seat order in constructor") {
        shouldThrow<IllegalArgumentException> {
            SanmaStandardTableTopology(emptyList())
        }

        shouldThrow<IllegalArgumentException> {
            SanmaStandardTableTopology(listOf(EAST, EAST))
        }
    }
})
