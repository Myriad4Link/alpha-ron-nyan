package xyz.uthofficial.arnyan.env.yaku

class StandardYakuConfiguration: YakuConfiguration {
    override infix fun Int.han(block: () -> Unit) {

    }

    private class NHanConfiguration(hanCount: Int)
}