package xyz.uthofficial.arnyan.demo

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind
import xyz.uthofficial.arnyan.env.yaku.resolver.Mentsu
import java.util.UUID

class HumanPlayer(
    val name: String,
    override val closeHand: MutableList<Tile> = mutableListOf(),
    override val openHand: MutableList<List<Tile>> = mutableListOf(),
    override val currentMentsusComposition: MutableList<List<Mentsu>> = mutableListOf(),
    override var seat: Wind? = null,
    override var score: Int = 25000,
    override var isRiichiDeclared: Boolean = false,
    override var riichiSticksDeposited: Int = 0,
    override var nukiCount: Int = 0
) : Player {
    override val id: UUID = UUID.randomUUID()
    
    override fun toString(): String = "HumanPlayer($name, seat=$seat, score=$score, nuki=$nukiCount)"
}
