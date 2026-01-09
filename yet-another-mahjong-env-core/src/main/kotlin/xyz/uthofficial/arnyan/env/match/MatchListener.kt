package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.tile.TileWall

interface MatchListener {
    fun onMatchInitialised(playerList: List<Player>, wall: TileWall)
}