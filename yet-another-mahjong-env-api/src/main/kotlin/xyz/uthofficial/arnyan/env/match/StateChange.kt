package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.Wind

sealed class StateChange {
    data class RemoveTilesFromHand(val seat: Wind, val tiles: List<Tile>) : StateChange()
    data class AddOpenGroup(val seat: Wind, val group: List<Tile>) : StateChange()
    data class RemoveOpenGroup(val seat: Wind, val groupIndex: Int) : StateChange()
    data class RemoveTileFromDiscards(val seat: Wind, val tile: Tile) : StateChange()
    data class UpdatePlayerScore(val seat: Wind, val delta: Int) : StateChange()
    data class UpdateRiichiSticks(val delta: Int) : StateChange()
    data class UpdateHonbaSticks(val delta: Int) : StateChange()
    data class DrawReplacementTile(val seat: Wind) : StateChange()
}