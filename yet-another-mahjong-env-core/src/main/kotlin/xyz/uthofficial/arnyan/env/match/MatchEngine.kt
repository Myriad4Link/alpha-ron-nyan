package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.error.ActionError
import xyz.uthofficial.arnyan.env.player.Player
import xyz.uthofficial.arnyan.env.error.wrapActionError
import xyz.uthofficial.arnyan.env.result.Result
import xyz.uthofficial.arnyan.env.result.binding
import xyz.uthofficial.arnyan.env.tile.Tile
import xyz.uthofficial.arnyan.env.match.actions.Chii
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.match.actions.PassAction
import xyz.uthofficial.arnyan.env.match.actions.Pon
import xyz.uthofficial.arnyan.env.match.actions.RiichiAction
import xyz.uthofficial.arnyan.env.match.actions.Ron
import xyz.uthofficial.arnyan.env.match.actions.TsuMo

internal class MatchEngine(
    private val allActions: List<Action> = listOf(
        Chii, Pon, Ron, TsuMo, DiscardAction, PassAction, RiichiAction
    )
) {
    private val validator = ActionValidator()
    private val actionMaskBuilder = ActionMaskBuilder(validator, allActions)
    private val turnProgression = TurnProgression(actionMaskBuilder, validator)

    fun maskToActions(mask: Int): List<Action> {
        return actionMaskBuilder.maskToActions(mask)
    }

    fun updateAvailableActions(state: MatchState) {
        actionMaskBuilder.updateAvailableActions(state)
    }

    fun validateAction(state: MatchState, player: Player, action: Action, subject: Tile): Result<Unit, ActionError> {
        return validator.validateAction(state, player, action, subject)
    }

    fun start(state: MatchState): Result<StepResult, ActionError> {
        return turnProgression.start(state)
    }

    fun submitAction(
        state: MatchState,
        player: Player,
        action: Action,
        subject: Tile
    ): Result<StepResult, ActionError> = binding {
        validateAction(state, player, action, subject).bind()

        val stepResult = action.perform(state.toObservation(), player, subject).bind()

        turnProgression.applyActionStateChanges(state, action, player, stepResult)

        val finalStepResult =
            if (!stepResult.isOver && state.lastAction is LastAction.Discard && validator.allInterruptsResolved(state)) {
                state.currentSeatWind = state.topology.getShimocha(state.currentSeatWind).wrapActionError().bind()
                turnProgression.handlePostDiscardTurnAdvancement(state)
            } else {
                stepResult
            }
        finalStepResult
    }

    fun checkOver(state: MatchState): Boolean {
        return turnProgression.checkOver(state)
    }
}
