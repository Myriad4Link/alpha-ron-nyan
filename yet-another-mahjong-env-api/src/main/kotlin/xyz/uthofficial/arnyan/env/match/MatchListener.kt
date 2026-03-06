package xyz.uthofficial.arnyan.env.match

interface MatchListener {
    fun onMatchStarted(observation: MatchObservation)
    fun onRoundStarted(observation: MatchObservation)
}