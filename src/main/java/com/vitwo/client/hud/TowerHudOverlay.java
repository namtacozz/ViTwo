package com.vitwo.client.hud;

import com.vitwo.battle.LevelCapManager;
import com.vitwo.battle.TowerCurseManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class TowerHudOverlay {
    public static boolean inTowerSession = false;
    public static int currentFloor = 1;
    public static int soloCheckpoint = 1;
    public static int duoCheckpoint = 1;
    public static boolean inBattle = false;
    public static boolean isSpectating = false;
    public static boolean isSolo = true;
    public static boolean isTrueRun = true;
    public static String currentBossName = "";
    public static int playerBp = 0;
    public static int ghostCharges = 0;
    public static int maxGhostCharges = 2;

    public static void render(DrawContext context, float tickDelta) {
        // Strict Visibility: Only render when player is actively inside a tower run session
        if (!inTowerSession) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int x = 12;
        int y = 12;
        int height = isSpectating ? 122 : 76;

        // Calculate dynamic width based on longest content line
        int maxCap = LevelCapManager.getMaxLevelCapForFloor(currentFloor > 0 ? currentFloor : 1);
        boolean hasShiny = LevelCapManager.hasShinyBossPokemon(currentFloor);
        String floorStr = "§eFloor: §f" + currentFloor + "/100 §7| §eCap: §aLv." + maxCap + (hasShiny ? " §d✨" : "");
        String oppName = currentBossName.isEmpty() ? "Trainer" : currentBossName;
        String oppStr = "§cOpponent: §f" + oppName;

        List<TowerCurseManager.TowerCurse> curses = TowerCurseManager.getInstance().getActiveCursesForFloor(currentFloor);
        String curseStr = curses.isEmpty() ? "§7Affix: §aNone" : (curses.size() == 1 ? "§4Affix: " + curses.get(0).hudBadge : "§4Affixes: " + curses.get(0).hudBadge + " §7| " + curses.get(1).hudBadge);

        int textW1 = client.textRenderer.getWidth(oppStr);
        int textW2 = client.textRenderer.getWidth(curseStr);
        int textW3 = client.textRenderer.getWidth(floorStr);
        int width = Math.max(240, Math.max(textW1, Math.max(textW2, textW3)) + 16);

        // Cobblemon "Slate & Cyan" HUD Design
        // Main Slate Panel
        context.fill(x - 4, y - 4, x + width, y + height, 0xF212171E);
        // Cyan Accent Border
        context.drawBorder(x - 4, y - 4, width + 4, height + 4, 0xFF0FD9C2);
        // Top accent bar
        context.fill(x - 3, y - 3, x + width - 1, y - 1, 0xFF0FD9C2);

        // Header Title & Mode & True Run Badge
        context.drawTextWithShadow(client.textRenderer, "§b§lCOBBLE TOWER", x + 2, y + 2, 0xFFFFFF);
        String modeTag = isSolo ? "§7[SOLO]" : "§d[DUO]";
        String runTag = isTrueRun ? " §a[★ TRUE]" : " §e[⚡ CP]";
        context.drawTextWithShadow(client.textRenderer, modeTag + runTag, x + width - 85, y + 2, 0xFFFFFF);

        // Floor progress & Level Cap
        context.drawTextWithShadow(client.textRenderer, floorStr, x + 2, y + 14, 0xFFFFFF);

        // Opponent Name Display
        context.drawTextWithShadow(client.textRenderer, oppStr, x + 2, y + 26, 0xFFFFFF);

        // Active Floor Curses
        context.drawTextWithShadow(client.textRenderer, curseStr, x + 2, y + 38, 0xFFAAAA);

        // Party Health Indicator Pips (6 pips)
        String partyPips = isSolo ? "§a● ● ● ● ● ● §7(6v6 Party)" : "§a● ● ● §d● ● ● §7(3+3 Team)";
        context.drawTextWithShadow(client.textRenderer, "§7Team: " + partyPips, x + 2, y + 50, 0xCCCCCC);

        // Battle Status / Spectating & Hub Hint
        if (isSpectating) {
            context.drawTextWithShadow(client.textRenderer, "§c● SPECTATING §7([Y] Hub)", x + 2, y + 62, 0xFF5555);

            // Ghost Support Mode Panel
            String chargeIcons = "§e" + "⚡ ".repeat(ghostCharges) + "§8" + "⚡ ".repeat(Math.max(0, maxGhostCharges - ghostCharges));
            context.drawTextWithShadow(client.textRenderer, "§b👻 GHOST SUPPORT: " + chargeIcons.trim(), x + 2, y + 74, 0x55FFFF);
            context.drawTextWithShadow(client.textRenderer, "§b[Z] Heal Pulse (1⚡ - +20% HP)", x + 6, y + 86, 0xAAFFFF);
            context.drawTextWithShadow(client.textRenderer, "§e[X] Quick Guard (1⚡ - Priority Shield)", x + 6, y + 97, 0xFFFF88);
            context.drawTextWithShadow(client.textRenderer, "§c[C] Battle Cry (2⚡ - +1 Def/SpD)", x + 6, y + 108, 0xFF8888);
        } else if (inBattle) {
            context.drawTextWithShadow(client.textRenderer, "§a● BATTLE ACTIVE §7([Y] Hub)", x + 2, y + 62, 0x55FF55);
        } else {
            context.drawTextWithShadow(client.textRenderer, "§6● REST STATION §7([Y] Hub)", x + 2, y + 62, 0xFFAA00);
        }
    }
}
