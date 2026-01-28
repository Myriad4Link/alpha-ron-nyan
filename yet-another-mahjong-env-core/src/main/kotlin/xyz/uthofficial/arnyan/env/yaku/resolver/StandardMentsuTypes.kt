package xyz.uthofficial.arnyan.env.yaku.resolver

import xyz.uthofficial.arnyan.env.utils.annotations.RegisterMentsuType

//enum class StandardMentsuType : MentsuType {
//    SHUNTSU,
//    KOUTSU,
//    KANTSU
//}

@RegisterMentsuType
object Shuntsu : MentsuType

@RegisterMentsuType
object Koutsu : MentsuType

@RegisterMentsuType
object Kantsu : MentsuType