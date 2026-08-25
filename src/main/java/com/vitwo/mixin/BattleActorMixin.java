package com.vitwo.mixin;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.PassActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = BattleActor.class, remap = false)
public abstract class BattleActorMixin {

    @Shadow
    private ShowdownActionRequest request;

    @Shadow
    private List<ShowdownActionResponse> responses;

    @Shadow
    private List<ActiveBattlePokemon> activePokemon;

    /**
     * Prevents mid-battle desync / IndexOutOfBoundsException in Double Battles
     * when a player or NPC is down to 1 active Pokémon and fewer action responses
     * are queued than the number of active slots requested by Showdown.
     */
    @Inject(method = "writeShowdownResponse", at = @At("HEAD"))
    private void vitwo$padMissingResponses(CallbackInfo ci) {
        if (this.request == null) return;

        int needed = 0;
        if (this.request.getActive() != null && !this.request.getActive().isEmpty()) {
            needed = this.request.getActive().size();
        } else if (this.request.getForceSwitch() != null && !this.request.getForceSwitch().isEmpty()) {
            needed = this.request.getForceSwitch().size();
        } else if (this.activePokemon != null) {
            needed = this.activePokemon.size();
        }

        if (needed <= 0) return;

        if (this.responses == null) {
            this.responses = new ArrayList<>();
        }

        while (this.responses.size() < needed) {
            this.responses.add(PassActionResponse.INSTANCE);
        }
    }
}
