package com.vitwo.mixin;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public class PlayerAdvancementTrackerMixin {
    @Shadow private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("HEAD"), cancellable = true)
    private void vitwo$cancelNonTowerAdvancementsInTower(AdvancementEntry advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if (this.owner != null && this.owner.getServerWorld() != null) {
            var key = this.owner.getServerWorld().getRegistryKey();
            if (key != null && key.getValue().getPath().contains("tower")) {
                // If player is inside the tower dimension, ONLY allow 'vitwo' tower achievements!
                if (advancement != null && advancement.id() != null && !advancement.id().getNamespace().equals("vitwo")) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
