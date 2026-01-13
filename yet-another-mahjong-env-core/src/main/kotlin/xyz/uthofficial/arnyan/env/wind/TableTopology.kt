package xyz.uthofficial.arnyan.env.wind

import xyz.uthofficial.arnyan.env.error.TopologyError
import xyz.uthofficial.arnyan.env.result.Result

interface TableTopology {
    val seats: List<Wind>
    fun getShimocha(current: Wind): Result<Wind, TopologyError>
    fun getKamicha(current: Wind): Result<Wind, TopologyError>
    fun getToimen(current: Wind): Result<Wind, TopologyError>
}
