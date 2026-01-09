package xyz.uthofficial.arnyan.env.wind

interface TableTopology {
    fun getShimocha(current: Wind): Wind
    fun getKamicha(current: Wind): Wind
    fun getToimen(current: Wind): Wind
}
