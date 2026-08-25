package com.vitwo.mixin;

import com.vitwo.client.hud.TowerHudOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ClientScreenMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void vitwo$restrictStorageScreenInTower(Screen screen, CallbackInfo ci) {
        if (screen == null) return;

        if (TowerHudOverlay.inTowerSession) {
            String className = screen.getClass().getName().toLowerCase();
            String title = screen.getTitle() != null ? screen.getTitle().getString().toLowerCase() : "";

            // Check if attempting to open Cobblemon PC / Pokebox GUI
            if (className.contains("pc") || className.contains("pokebox") || className.contains("storage") || title.contains("box") || title.contains("pc")) {
                @SuppressWarnings("resource")
                MinecraftClient client = (MinecraftClient) (Object) this;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§c[CobbleTower] PC Storage is locked during Tower Runs! Press [C] for Summary."), true);
                }
                ci.cancel();
            }
        }
    }
}
