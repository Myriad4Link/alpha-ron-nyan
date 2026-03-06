package xyz.uthofficial.arnyan.demo

import java.util.Scanner

object UserInput {
    private val scanner = Scanner(System.`in`)
    
    fun readActionIndex(maxIndex: Int): Int {
        while (true) {
            print("Enter action index (0-$maxIndex): ")
            val input = scanner.nextLine().trim()
            
            val index = input.toIntOrNull()
            if (index != null && index in 0..maxIndex) {
                return index
            }
            
            println("Invalid input. Please enter a number between 0 and $maxIndex.")
        }
    }
    
    fun readTileIndex(handSize: Int): Int {
        while (true) {
            print("Enter tile index (0-${handSize - 1}): ")
            val input = scanner.nextLine().trim()
            
            val index = input.toIntOrNull()
            if (index != null && index in 0 until handSize) {
                return index
            }
            
            println("Invalid input. Please enter a number between 0 and ${handSize - 1}.")
        }
    }
    
    fun readContinuePrompt(): Boolean {
        while (true) {
            print("Start next round? (y/n): ")
            val input = scanner.nextLine().trim().lowercase()
            
            when (input) {
                "y", "yes" -> return true
                "n", "no" -> return false
                else -> println("Invalid input. Please enter 'y' or 'n'.")
            }
        }
    }
    
    fun readPlayerName(): String {
        print("Enter your name: ")
        return scanner.nextLine().trim().ifEmpty { "Player" }
    }
}
