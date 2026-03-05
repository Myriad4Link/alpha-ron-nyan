package xyz.uthofficial.arnyan.env.match.actions

import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.error.MatchError
import xyz.uthofficial.arnyan.env.error.wrapActionError
import xyz.uthofficial.arnyan.env.match.Action
import xyz.uthofficial.arnyan.env.match.ErrorMessages
import xyz.uthofficial.arnyan.env.match.LastAction
import xyz.uthofficial.arnyan.env.match.MatchObservation
import xyz.uthofficial.arnyan.env.match.StateChange
import xyz.uthofficial.arnyan.env.match.StepResult
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.wind.StandardWind

object KaKan : Action {
    override val id = Action.ID_KAKAN
    override fun toString() = "KA_KAN"

    override fun availableWhen(observation: MatchObservation, actor: Player, subject: Tile): Boolean {
        if (actor.seat != observation.currentSeatWind) return false
        
        val lastAction = observation.lastAction
        if (lastAction !is LastAction.Draw) return false
        
        val existingPon = findExistingPon(actor.openHand, subject)
        return existingPon != null
    }

    override fun perform(observation: MatchObservation, actor: Player, subject: Tile): Result<StepResult, ActionError> =
        binding {
            val actorSeat = actor.seat ?: Result.Failure<ActionError>(
                MatchError.ActionNotAvailable(toString(), StandardWind.EAST, ErrorMessages.PLAYER_HAS_NO_SEAT)
                    .wrapActionError()
            ).bind()

            val lastAction = observation.lastAction
            if (lastAction !is LastAction.Draw) {
                Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        "Kakan can only be performed after drawing a tile"
                    ).wrapActionError()
                ).bind()
            }

            val existingPonIndex = findExistingPonIndex(actor.openHand, subject)
                ?: Result.Failure<ActionError>(
                    MatchError.ActionNotAvailable(
                        toString(),
                        actorSeat,
                        "No existing pon to add tile for kakan"
                    ).wrapActionError()
                ).bind()

            val existingPon = actor.openHand[existingPonIndex]
            val kanGroup = (existingPon + subject).sortedBy { it.index() }
            
            val stateChanges = listOf(
                StateChange.RemoveTilesFromHand(actorSeat, listOf(subject)),
                StateChange.RemoveOpenGroup(actorSeat, existingPonIndex),
                StateChange.AddOpenGroup(actorSeat, kanGroup)
            )

            val newObservation = MatchObservation(
                players = observation.players,
                wall = observation.wall,
                topology = observation.topology,
                currentSeatWind = actorSeat,
                roundRotationStatus = observation.roundRotationStatus,
                discards = observation.discards,
                lastAction = LastAction.KaKan(subject, actor),
                yakuConfiguration = observation.yakuConfiguration,
                scoringCalculator = observation.scoringCalculator,
                riichiSticks = observation.riichiSticks,
                honbaSticks = observation.honbaSticks
            )

            StepResult(newObservation, actorSeat, false, stateChanges)
        }
}

internal fun findExistingPon(openHand: List<List<Tile>>, subject: Tile): List<Tile>? {
    val subjectIdx = subject.index()
    return openHand.find { group ->
        group.size == 3 && group.all { it.index() == subjectIdx }
    }
}

internal fun findExistingPonIndex(openHand: List<List<Tile>>, subject: Tile): Int? {
    val subjectIdx = subject.index()
    return openHand.indexOfFirst { group ->
        group.size == 3 && group.all { it.index() == subjectIdx }
    }.takeIf { it != -1 }
}
