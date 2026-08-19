package com.vitwo.client.hud;

import com.vitwo.battle.LevelCapManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class TowerHudOverlay {
    public static int currentFloor = 0;
    public static int soloCheckpoint = 1;
    public static int duoCheckpoint = 1;
    public static boolean inBattle = false;
    public static boolean isSpectating = false;
    public static boolean isSolo = true;

    public static void render(DrawContext context, float tickDelta) {
        if (!inBattle && currentFloor == 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int x = 12;
        int y = 12;
        int width = 165;
        int height = 48;

        // Cobblemon-style dark slate HUD panel
        context.fill(x - 4, y - 4, x + width, y + height, 0xD0121722);
        context.drawBorder(x - 4, y - 4, width + 4, height + 4, 0xFF4B6080);

        // Header Title
        context.drawTextWithShadow(client.textRenderer, "§6§lCOBBLE TOWER", x + 2, y, 0xFFFFFF);

        // Mode badge
        String modeTag = isSolo ? "§b[SOLO 2-SLOT]" : "§d[DUO 2v1]";
        context.drawTextWithShadow(client.textRenderer, modeTag, x + 85, y, 0xFFFFFF);

        // Floor progress & Checkpoint
        int activeCp = isSolo ? soloCheckpoint : duoCheckpoint;
        context.drawTextWithShadow(client.textRenderer, "§eTầng: §f" + currentFloor + "/100 §7(CP: " + activeCp + ")", x + 2, y + 14, 0xFFFFFF);

        // Level Cap Rule
        int maxCap = LevelCapManager.getMaxLevelCapForFloor(currentFloor > 0 ? currentFloor : 1);
        boolean hasShiny = LevelCapManager.hasShinyBossPokemon(currentFloor);
        String ruleTag = "§7Cap: Lv." + maxCap + (hasShiny ? " §d✨" : "");
        context.drawTextWithShadow(client.textRenderer, ruleTag, x + 95, y + 14, 0xAAAAAA);

        if (isSpectating) {
            context.drawTextWithShadow(client.textRenderer, "§c● ĐANG XEM ĐỒNG ĐỘI", x + 2, y + 28, 0xFF5555);
        } else {
            context.drawTextWithShadow(client.textRenderer, "§a● TRẬN ĐẤU ĐANG DIỄN RA", x + 2, y + 28, 0x55FF55);
        }
    }
}
