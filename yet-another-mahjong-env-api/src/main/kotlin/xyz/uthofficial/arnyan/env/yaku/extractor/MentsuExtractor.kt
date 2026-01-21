package xyz.uthofficial.arnyan.env.yaku.extractor

import xyz.uthofficial.arnyan.env.tile.Tile

fun interface MentsuExtractor {
    fun extract(hand: List<Tile>): Mentsu
}