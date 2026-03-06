package xyz.uthofficial.arnyan.simplified.evolution


import xyz.uthofficial.arnyan.simplified.agent.EvolutionaryAgent
import xyz.uthofficial.arnyan.simplified.agent.Playstyle

sealed class MatchEvent {
    data class RonWin(val handValue: Int) : MatchEvent()
    data class TsumoWin(val handValue: Int) : MatchEvent()
    data class RiichiDeclared(val turn: Int, val isSuccessful: Boolean) : MatchEvent()
    data class Pon(val turn: Int) : MatchEvent()
    data class Chii(val turn: Int) : MatchEvent()
    data class Kan(val turn: Int, val isAnkan: Boolean) : MatchEvent()
    data class TenpaiEnd(val isRiichi: Boolean) : MatchEvent()
    object Furiten : MatchEvent()
    data class DealIn(val handValue: Int) : MatchEvent()
    data class PassAvailable(val turn: Int) : MatchEvent()
    data class SafeDiscard(val turn: Int) : MatchEvent()
    data class EarlyRiichi(val turn: Int) : MatchEvent()
    data class RiichiSafe(val turn: Int) : MatchEvent()
    object TenpaiAchieved : MatchEvent()
}

class MatchEventTracker {
    private val events = mutableListOf<MatchEvent>()
    private var currentTurn = 0

    fun record(event: MatchEvent) {
        events.add(event)
    }

    fun incrementTurn() {
        currentTurn++
    }

    fun getEvents(): List<MatchEvent> = events.toList()

    fun reset() {
        events.clear()
        currentTurn = 0
    }
}

class FitnessCalculator(private val playstyle: Playstyle) {

    private val aggressiveRewards = mapOf(
        "RonWin" to +10f,
        "TsumoWin" to +8f,
        "RiichiDeclared" to +5f,
        "Pon" to +3f,
        "Chii" to +3f,
        "Kan" to +3f,
        "TenpaiEnd" to +2f,
        "Furiten" to -5f,
        "DealIn" to -3f,
        "PassAvailable" to -1f
    )

    private val conservativeRewards = mapOf(
        "RonWin" to +8f,
        "TsumoWin" to +6f,
        "TenpaiAchieved" to +5f,
        "RiichiSafe" to +3f,
        "SafeDiscard" to +2f,
        "Kan" to +1f,
        "DealIn" to -3f,
        "Furiten" to -2f,
        "EarlyRiichi" to -1f
    )

    fun calculate(agent: EvolutionaryAgent, events: List<MatchEvent>): Float {
        val rewards = when (playstyle) {
            Playstyle.AGGRESSIVE -> aggressiveRewards
            Playstyle.CONSERVATIVE -> conservativeRewards
        }

        return events.sumOf { event ->
            val eventName = event::class.java.simpleName
            when (playstyle) {
                Playstyle.AGGRESSIVE -> aggressiveRewards[eventName] ?: 0f
                Playstyle.CONSERVATIVE -> conservativeRewards[eventName] ?: 0f
            }.toDouble()
        }.toFloat()
    }

    fun calculateWithDetails(agent: EvolutionaryAgent, events: List<MatchEvent>): FitnessDetails {
        val rewards = when (playstyle) {
            Playstyle.AGGRESSIVE -> aggressiveRewards
            Playstyle.CONSERVATIVE -> conservativeRewards
        }

        val breakdown = mutableMapOf<String, Float>()
        var total = 0f

        events.forEach { event ->
            val eventName = event::class.java.simpleName
            val reward = when (event) {
                is MatchEvent.RonWin -> rewards["RonWin"] ?: 0f
                is MatchEvent.TsumoWin -> rewards["TsumoWin"] ?: 0f
                is MatchEvent.RiichiDeclared -> if (event.isSuccessful) rewards["RiichiDeclared"] ?: 0f else 0f
                is MatchEvent.Pon -> rewards["Pon"] ?: 0f
                is MatchEvent.Chii -> rewards["Chii"] ?: 0f
                is MatchEvent.Kan -> rewards["Kan"] ?: 0f
                is MatchEvent.TenpaiEnd -> rewards["TenpaiEnd"] ?: 0f
                is MatchEvent.TenpaiAchieved -> rewards["TenpaiAchieved"] ?: 0f
                is MatchEvent.Furiten -> rewards["Furiten"] ?: 0f
                is MatchEvent.DealIn -> rewards["DealIn"] ?: 0f
                is MatchEvent.PassAvailable -> rewards["PassAvailable"] ?: 0f
                is MatchEvent.SafeDiscard -> rewards["SafeDiscard"] ?: 0f
                is MatchEvent.EarlyRiichi -> rewards["EarlyRiichi"] ?: 0f
                is MatchEvent.RiichiSafe -> rewards["RiichiSafe"] ?: 0f
            }

            if (reward != 0f) {
                breakdown[eventName] = (breakdown[eventName] ?: 0f) + reward
                total += reward
            }
        }

        return FitnessDetails(total, breakdown, playstyle)
    }
}

data class FitnessDetails(
    val total: Float,
    val breakdown: Map<String, Float>,
    val playstyle: Playstyle
) {
    override fun toString(): String {
        return buildString {
            appendLine("Fitness: $total ($playstyle)")
            breakdown.forEach { (event, reward) ->
                appendLine("  $event: $reward")
            }
        }
    }
}
