package xyz.uthofficial.arnyan.env.match

import xyz.uthofficial.arnyan.env.match.actions.AnKan
import xyz.uthofficial.arnyan.env.match.actions.Chii
import xyz.uthofficial.arnyan.env.match.actions.DiscardAction
import xyz.uthofficial.arnyan.env.match.actions.KaKan
import xyz.uthofficial.arnyan.env.match.actions.MinKan
import xyz.uthofficial.arnyan.env.match.actions.PassAction
import xyz.uthofficial.arnyan.env.match.actions.Pon
import xyz.uthofficial.arnyan.env.match.actions.RiichiAction
import xyz.uthofficial.arnyan.env.match.actions.Ron
import xyz.uthofficial.arnyan.env.match.actions.TsuMo
import xyz.uthofficial.arnyan.env.match.actions.NukiPei
import xyz.uthofficial.arnyan.env.wind.Wind

internal class ActionMaskBuilder(
    private val validator: ActionValidator,
    private val allActions: List<Action> = listOf(
        Chii, Pon, Ron, TsuMo, DiscardAction, PassAction, RiichiAction, AnKan, MinKan, KaKan, NukiPei
    )
) {
    fun maskToActions(mask: Int): List<Action> {
        if (mask == 0) return emptyList()
        val result = mutableListOf<Action>()
        allActions.forEach { action ->
            if (action.id and mask != 0) {
                result.add(action)
            }
        }
        return result
    }

    fun updateAvailableActions(state: MatchState) {
        val obs = state.toObservation()

        state.topology.seats.forEach { seat ->
            state.availableActionsMaskPerPlayer[seat] = 0
        }

        val interruptAvailability = mutableMapOf<Wind, Int>()
        val discardingSeat = when (val last = obs.lastAction) {
            is LastAction.Discard -> last.player.seat
            else -> null
        }

        for (player in state.players) {
            val seat = player.seat ?: continue
            if (seat in state.passedPlayers) continue
            
            var mask = 0
            val subject = when (obs.lastAction) {
                is LastAction.Discard -> (obs.lastAction as LastAction.Discard).tile
                else -> null
            }
            if (subject != null) {
                if (Chii.availableWhen(obs, player, subject)) mask = mask or Action.ID_CHII
                if (Pon.availableWhen(obs, player, subject)) mask = mask or Action.ID_PON
                if (Ron.availableWhen(obs, player, subject)) mask = mask or Action.ID_RON
                if (MinKan.availableWhen(obs, player, subject)) mask = mask or Action.ID_MINKAN
            }
            if (mask != 0) {
                interruptAvailability[seat] = mask
            }
        }

        val prioritizedInterrupts = if (discardingSeat != null) {
            validator.applyInterruptPriority(discardingSeat, state.topology, interruptAvailability)
        } else {
            interruptAvailability
        }

        for (player in state.players) {
            val seat = player.seat ?: continue
            var playerMask = prioritizedInterrupts[seat] ?: 0

            if (player.isRiichiDeclared) {
                playerMask = playerMask and (Action.ID_TSUMO or Action.ID_RON or Action.ID_PASS)
            }

            if (seat == state.currentSeatWind) {
                player.closeHand.forEach { tile ->
                    if (DiscardAction.availableWhen(obs, player, tile)) {
                        playerMask = playerMask or Action.ID_DISCARD
                        return@forEach
                    }
                }
            }

            val drawSubject = when (obs.lastAction) {
                is LastAction.Draw -> (obs.lastAction as LastAction.Draw).tile
                else -> null
            }
            if (drawSubject != null) {
                if (TsuMo.availableWhen(obs, player, drawSubject)) {
                    playerMask = playerMask or Action.ID_TSUMO
                }
                if (seat == state.currentSeatWind) {
                    if (AnKan.availableWhen(obs, player, drawSubject)) {
                        playerMask = playerMask or Action.ID_ANKAN
                    }
                    if (KaKan.availableWhen(obs, player, drawSubject)) {
                        playerMask = playerMask or Action.ID_KAKAN
                    }
                    if (RiichiAction.availableWhen(obs, player, drawSubject)) {
                        playerMask = playerMask or Action.ID_RIICHI
                    }
                    if (NukiPei.availableWhen(obs, player, drawSubject)) {
                        playerMask = playerMask or Action.ID_NUKI
                    }
                }
            }

            val discardSubject = when (obs.lastAction) {
                is LastAction.Discard -> (obs.lastAction as LastAction.Discard).tile
                else -> null
            }
            if (discardSubject != null && seat !in state.passedPlayers) {
                val hasInterrupt = (playerMask and (Action.ID_CHII or Action.ID_PON or Action.ID_RON)) != 0
                if (hasInterrupt) {
                    playerMask = playerMask or Action.ID_PASS
                }
            }

            state.availableActionsMaskPerPlayer[seat] = playerMask
        }
    }
}
