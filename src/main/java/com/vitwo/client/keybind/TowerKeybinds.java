package com.vitwo.client.keybind;

import com.vitwo.client.gui.TowerHubScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TowerKeybinds {
    public static KeyBinding openHubKey;

    public static void registerKeybinds() {
        openHubKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vitwo.hub",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "key.category.vitwo"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openHubKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new TowerHubScreen());
                }
            }
        });
    }
}
