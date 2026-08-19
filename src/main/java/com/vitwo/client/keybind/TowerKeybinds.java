package com.vitwo.client.keybind;

import com.vitwo.client.gui.TowerHubScreen;
import com.vitwo.client.hud.TowerHudOverlay;
import com.vitwo.network.c2s.ForfeitTowerC2SPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TowerKeybinds {
    public static KeyBinding openHubKey;
    public static KeyBinding forfeitKey;
    public static KeyBinding ghostHealKey;
    public static KeyBinding ghostGuardKey;
    public static KeyBinding ghostBuffKey;

    public static void registerKeybinds() {
        openHubKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vitwo.hub",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "key.category.vitwo"
        ));

        forfeitKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vitwo.forfeit",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "key.category.vitwo"
        ));

        ghostHealKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vitwo.ghost_heal",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "key.category.vitwo"
        ));

        ghostGuardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vitwo.ghost_guard",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "key.category.vitwo"
        ));

        ghostBuffKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vitwo.ghost_buff",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "key.category.vitwo"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openHubKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new TowerHubScreen());
                }
            }

            while (forfeitKey.wasPressed()) {
                if (client.player != null && TowerHudOverlay.inTowerSession) {
                    ClientPlayNetworking.send(new ForfeitTowerC2SPacket());
                }
            }

            while (ghostHealKey.wasPressed()) {
                if (client.player != null && TowerHudOverlay.inTowerSession && TowerHudOverlay.isSpectating) {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.GhostSupportActionC2SPacket(1));
                }
            }

            while (ghostGuardKey.wasPressed()) {
                if (client.player != null && TowerHudOverlay.inTowerSession && TowerHudOverlay.isSpectating) {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.GhostSupportActionC2SPacket(2));
                }
            }

            while (ghostBuffKey.wasPressed()) {
                if (client.player != null && TowerHudOverlay.inTowerSession && TowerHudOverlay.isSpectating) {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.GhostSupportActionC2SPacket(3));
                }
            }
        });
    }
}
