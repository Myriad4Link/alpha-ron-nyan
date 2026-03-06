package xyz.uthofficial.arnyan.demo

fun main() {
    try {
        GameLoop.run()
    } catch (e: Exception) {
        println("An error occurred: ${e.message}")
        e.printStackTrace()
    }
}
