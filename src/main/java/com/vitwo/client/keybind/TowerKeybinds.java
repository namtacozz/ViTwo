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
        });
    }
}
