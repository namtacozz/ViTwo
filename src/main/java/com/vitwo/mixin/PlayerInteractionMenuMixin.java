package com.vitwo.mixin;

import com.vitwo.client.gui.PartyInviteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerInteractionMenuMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true, require = 0)
    private void vitwo$onInteractWithPlayer(Entity target, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self.getWorld().isClient() && hand == Hand.MAIN_HAND && self.isSneaking() && target instanceof PlayerEntity targetPlayer) {
            // Open confirmation screen on client
            MinecraftClient.getInstance().setScreen(new PartyInviteScreen(targetPlayer));
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
