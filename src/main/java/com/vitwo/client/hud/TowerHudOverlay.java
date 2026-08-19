package com.vitwo.client.hud;

import com.vitwo.battle.LevelCapManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class TowerHudOverlay {
    public static boolean inTowerSession = false;
    public static int currentFloor = 1;
    public static int soloCheckpoint = 1;
    public static int duoCheckpoint = 1;
    public static boolean inBattle = false;
    public static boolean isSpectating = false;
    public static boolean isSolo = true;
    public static String currentBossName = "";

    public static void render(DrawContext context, float tickDelta) {
        // Strict Visibility: Only render when player is actively inside a tower run session
        if (!inTowerSession) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int x = 12;
        int y = 12;
        int width = 205;
        int height = 64;

        // Cobblemon "Slate & Cyan" HUD Design
        // Main Slate Panel
        context.fill(x - 4, y - 4, x + width, y + height, 0xEE1E232A);
        // Cyan Accent Border
        context.drawBorder(x - 4, y - 4, width + 4, height + 4, 0xFF0FD9C2);
        // Subtle top accent line
        context.fill(x - 3, y - 3, x + width - 1, y - 1, 0xFF0FD9C2);

        // Header Title & Mode
        context.drawTextWithShadow(client.textRenderer, "§b§lCOBBLE TOWER", x + 2, y + 2, 0xFFFFFF);
        String modeTag = isSolo ? "§7[SOLO]" : "§d[DUO]";
        context.drawTextWithShadow(client.textRenderer, modeTag, x + 95, y + 2, 0xFFFFFF);

        // Floor progress & Level Cap
        int maxCap = LevelCapManager.getMaxLevelCapForFloor(currentFloor > 0 ? currentFloor : 1);
        boolean hasShiny = LevelCapManager.hasShinyBossPokemon(currentFloor);
        String floorStr = "§eFloor: §f" + currentFloor + "/100 §7| §eCap: §aLv." + maxCap + (hasShiny ? " §d✨" : "");
        context.drawTextWithShadow(client.textRenderer, floorStr, x + 2, y + 14, 0xFFFFFF);

        // Opponent Name Display
        String oppName = currentBossName.isEmpty() ? "Trainer" : currentBossName;
        context.drawTextWithShadow(client.textRenderer, "§cOpponent: §f" + oppName, x + 2, y + 26, 0xFFFFFF);

        // Active Floor Curse
        com.vitwo.battle.TowerCurseManager.TowerCurse curse = com.vitwo.battle.TowerCurseManager.getInstance().getCurseForFloor(currentFloor);
        if (curse != com.vitwo.battle.TowerCurseManager.TowerCurse.NONE) {
            context.drawTextWithShadow(client.textRenderer, "§4Affix: " + curse.hudBadge, x + 2, y + 38, 0xFFAAAA);
        } else {
            context.drawTextWithShadow(client.textRenderer, "§7Affix: §aNone", x + 2, y + 38, 0x888888);
        }

        // Battle Status / Spectating & Hub Hint
        if (isSpectating) {
            context.drawTextWithShadow(client.textRenderer, "§c● SPECTATING §7([Y] Hub)", x + 2, y + 50, 0xFF5555);
        } else if (inBattle) {
            context.drawTextWithShadow(client.textRenderer, "§a● BATTLE ACTIVE §7([Y] Hub)", x + 2, y + 50, 0x55FF55);
        } else {
            context.drawTextWithShadow(client.textRenderer, "§6● REST STATION §7([Y] Hub)", x + 2, y + 50, 0xFFAA00);
        }
    }
}
