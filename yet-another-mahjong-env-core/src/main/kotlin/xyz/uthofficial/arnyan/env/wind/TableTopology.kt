package xyz.uthofficial.arnyan.env.wind

interface TableTopology {
    val seats: List<Wind>
    fun getShimocha(current: Wind): Result<Wind>
    fun getKamicha(current: Wind): Result<Wind>
    fun getToimen(current: Wind): Result<Wind>
}
