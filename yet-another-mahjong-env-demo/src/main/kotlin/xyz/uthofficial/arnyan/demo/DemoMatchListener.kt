package xyz.uthofficial.arnyan.demo

import xyz.uthofficial.arnyan.env.match.MatchListener
import xyz.uthofficial.arnyan.env.match.MatchObservation

class DemoMatchListener : MatchListener {
    override fun onMatchStarted(observation: MatchObservation) {
        ConsoleDisplay.printInfo("=======================================")
        ConsoleDisplay.printInfo("MATCH STARTED")
        ConsoleDisplay.printInfo("=======================================")
        ConsoleDisplay.printInfo("Round Wind: ${observation.roundRotationStatus.place}")
        ConsoleDisplay.printInfo("Total players: ${observation.players.size}")
        ConsoleDisplay.printInfo("")
    }
    
    override fun onRoundStarted(observation: MatchObservation) {
        ConsoleDisplay.printInfo("")
        ConsoleDisplay.printInfo("---------------------------------------")
        ConsoleDisplay.printInfo("Round ${observation.roundRotationStatus.place} ${observation.roundRotationStatus.round} started")
        ConsoleDisplay.printInfo("---------------------------------------")
        ConsoleDisplay.printInfo("")
    }
}
