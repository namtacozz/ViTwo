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
    
    // Runtime variables (Bottom-Left Battle Info)
    public static int runDurationSeconds = 0;
    public static int battleTurns = 0;
    public static int bpEarnedInRun = 0;
    public static int playerBp = 0;
    
    // Forfeit variables
    public static boolean forfeitVoteActive = false;
    public static boolean partnerVotedForfeit = false;
    public static int forfeitTimeLeft = 15;
    
    // Ghost Support variables
    public static int ghostCharges = 0;
    public static int maxGhostCharges = 2;

    // Cache & Dirty Flag System for HUD Optimization
    private static boolean isDirty = true;
    private static String cachedFloorText = "FLOOR 1/100";
    private static String cachedBossAndCapText = "Next Boss: Unknown | Cap: Lv.36";
    private static String cachedRuleText = "⚠ Rules: Competitive Clause";
    private static String cachedBattleInfo = "⏱ 00:00  |  Turn 0  |  BP in run: +0";
    private static String cachedGhostIcons = "§8⚡ ⚡";
    private static int lastRenderedSecond = -1;
    private static int lastRenderedTurn = -1;
    private static int lastRenderedBp = -1;
    private static int lastRenderedCharges = -1;

    public static void markDirty() {
        isDirty = true;
    }

    public static void clearCache() {
        inTowerSession = false;
        currentFloor = 1;
        inBattle = false;
        isSpectating = false;
        currentBossName = "";
        runDurationSeconds = 0;
        battleTurns = 0;
        bpEarnedInRun = 0;
        forfeitVoteActive = false;
        ghostCharges = 0;
        lastRenderedSecond = -1;
        lastRenderedTurn = -1;
        lastRenderedBp = -1;
        lastRenderedCharges = -1;
        isDirty = true;
    }

    private static void updateCacheIfNeeded() {
        if (!isDirty) return;

        if (currentFloor >= 100) {
            cachedFloorText = "§6§l★ FLOOR 100/100 (CHAMPION CYNTHIA) ★";
        } else if (currentFloor >= 90) {
            cachedFloorText = "§d§l✦ FLOOR " + currentFloor + "/100 (ELITE FOUR & CHAMPIONS) ✦";
        } else if (currentFloor % 5 == 0) {
            cachedFloorText = "§6§l⬡ FLOOR " + currentFloor + "/100 (GYM LEADER) ⬡";
        } else {
            cachedFloorText = "§b§lFLOOR " + currentFloor + "/100";
        }

        int maxCap = LevelCapManager.getMaxLevelCapForFloor(currentFloor);
        String bossName = currentBossName.isEmpty() ? "Unknown" : currentBossName;
        if (currentFloor >= 100) {
            cachedBossAndCapText = "§cBoss: §6Champion Cynthia §7| §bLevel Cap: §aLv." + maxCap;
        } else if (currentFloor >= 90) {
            cachedBossAndCapText = "§cBoss: §d" + bossName + " §7| §bLevel Cap: §aLv." + maxCap;
        } else if (currentFloor % 5 == 0) {
            cachedBossAndCapText = "§eBoss: §6" + bossName + " §7| §bLevel Cap: §aLv." + maxCap;
        } else {
            cachedBossAndCapText = "§fOpponent: §e" + bossName + " §7| §bLevel Cap: §aLv." + maxCap;
        }

        List<TowerCurseManager.TowerCurse> curses = TowerCurseManager.getInstance().getActiveCursesForFloor(currentFloor);
        if (!curses.isEmpty()) {
            cachedRuleText = "§c💀 Floor Curse: " + curses.get(0).hudBadge;
        } else {
            cachedRuleText = "";
        }

        isDirty = false;
    }

    public static void render(DrawContext context, float tickDelta) {
        if (!inTowerSession) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        updateCacheIfNeeded();

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // 1. Top Floor Progress Box (Top-Center)
        renderTopProgressBox(context, client, width);

        // 2. Battle Timer & Info (Bottom-Left)
        if (!isSpectating) {
            renderBattleInfo(context, client, height);
        } else {
            renderGhostSupportPanel(context, client, height);
        }

        // 3. Forfeit Voting Panel (Bottom-Right)
        if (forfeitVoteActive && !isSpectating) {
            renderForfeitPanel(context, client, width, height);
        }
    }

    private static void renderTopProgressBox(DrawContext context, MinecraftClient client, int screenWidth) {
        boolean hasRule = cachedRuleText != null && !cachedRuleText.isEmpty();
        int boxW = 280;
        int boxH = hasRule ? 50 : 38;
        int startX = (screenWidth - boxW) / 2;
        int startY = 5;

        // Dynamic Accent Border Color
        int borderColor;
        if (currentFloor >= 100) {
            borderColor = 0xFFFFD700; // Gold
        } else if (currentFloor >= 90) {
            borderColor = 0xFFE040FB; // Magenta
        } else if (currentFloor % 5 == 0) {
            borderColor = 0xFFFF9100; // Orange
        } else {
            borderColor = 0xFF00E5FF; // Cyan
        }

        // Deep slate background with themed border
        context.fill(startX, startY, startX + boxW, startY + boxH, 0xF210141D);
        context.drawBorder(startX, startY, boxW, boxH, borderColor);

        // Floor Text
        context.drawTextWithShadow(client.textRenderer, cachedFloorText, startX + 6, startY + 5, 0xFFFFFFFF);

        // Progress Bar
        int barW = boxW - 12;
        context.fill(startX + 6, startY + 17, startX + 6 + barW, startY + 21, 0xFF2A2E39);
        int fillW = (int) ((Math.min(100, Math.max(1, currentFloor)) / 100f) * barW);
        context.fill(startX + 6, startY + 17, startX + 6 + fillW, startY + 21, borderColor);

        // Next Boss & Cap Info
        context.drawTextWithShadow(client.textRenderer, cachedBossAndCapText, startX + 6, startY + 26, 0xFFCCCCCC);

        // Affixes / Rules (Only rendered when there is an active curse)
        if (hasRule) {
            context.drawTextWithShadow(client.textRenderer, cachedRuleText, startX + 6, startY + 38, 0xFFFFFFFF);
        }
    }

    private static void renderBattleInfo(DrawContext context, MinecraftClient client, int screenHeight) {
        int startX = 10;
        int startY = screenHeight - 22;

        if (runDurationSeconds != lastRenderedSecond || battleTurns != lastRenderedTurn || bpEarnedInRun != lastRenderedBp) {
            lastRenderedSecond = runDurationSeconds;
            lastRenderedTurn = battleTurns;
            lastRenderedBp = bpEarnedInRun;
            int min = Math.max(0, runDurationSeconds) / 60;
            int sec = Math.max(0, runDurationSeconds) % 60;
            cachedBattleInfo = String.format("§f⏱ Time: §b%02d:%02d  §7|  §fTurn: §eTurn %d  §7|  §fBP Earned: §a+%d BP", min, sec, battleTurns, bpEarnedInRun);
        }

        int textW = client.textRenderer.getWidth(cachedBattleInfo);
        context.fill(startX - 4, startY - 2, startX + textW + 4, startY + 11, 0xAA10141D);
        context.drawBorder(startX - 4, startY - 2, textW + 8, 13, 0xFF3D4A5D);
        context.drawTextWithShadow(client.textRenderer, cachedBattleInfo, startX, startY, 0xFFFFFFFF);
    }

    private static void renderForfeitPanel(DrawContext context, MinecraftClient client, int screenWidth, int screenHeight) {
        int boxW = 220;
        int boxH = 65;
        int startX = screenWidth - boxW - 10;
        int startY = screenHeight - boxH - 10;

        // Danger Red Border
        context.fill(startX, startY, startX + boxW, startY + boxH, 0xF212171E);
        context.drawBorder(startX, startY, boxW, boxH, 0xFFFF5555);

        context.drawCenteredTextWithShadow(client.textRenderer, "LEAVE RUN?", startX + boxW / 2, startY + 5, 0xFFFF5555);
        
        String partnerVote = partnerVotedForfeit ? "✅ Voted YES" : "❌ Not Voted";
        context.drawTextWithShadow(client.textRenderer, "Partner: " + partnerVote, startX + 5, startY + 18, 0xFFFFFFFF);
        
        context.drawCenteredTextWithShadow(client.textRenderer, "[F] YES, FORFEIT", startX + boxW / 4, startY + 32, 0xFFFF5555);
        context.drawCenteredTextWithShadow(client.textRenderer, "[X] NO, FIGHT", startX + boxW * 3 / 4, startY + 32, 0xFF55FF55);

        // Time bar
        context.drawTextWithShadow(client.textRenderer, "Vote Time: " + forfeitTimeLeft + "s", startX + 5, startY + 45, 0xFFCCCCCC);
        context.fill(startX + 5, startY + 56, startX + boxW - 5, startY + 58, 0xFF333333);
        int timeFill = (int) ((forfeitTimeLeft / 15f) * (boxW - 10));
        context.fill(startX + 5, startY + 56, startX + 5 + timeFill, startY + 58, 0xFFFF5555);
    }

    private static void renderGhostSupportPanel(DrawContext context, MinecraftClient client, int screenHeight) {
        int boxW = 200;
        int boxH = 65;
        int startX = 10;
        int startY = screenHeight - boxH - 10;

        context.fill(startX, startY, startX + boxW, startY + boxH, 0xF212171E);
        context.drawBorder(startX, startY, boxW, boxH, 0xFF00E5FF);

        context.drawTextWithShadow(client.textRenderer, "§c● SPECTATING §7([Y] Hub)", startX + 5, startY + 5, 0xFFFF5555);
        
        if (ghostCharges != lastRenderedCharges) {
            lastRenderedCharges = ghostCharges;
            cachedGhostIcons = "§e" + "⚡ ".repeat(Math.max(0, ghostCharges)) + "§8" + "⚡ ".repeat(Math.max(0, maxGhostCharges - ghostCharges));
        }
        context.drawTextWithShadow(client.textRenderer, "§b👻 GHOST SUPPORT: " + cachedGhostIcons.trim(), startX + 5, startY + 18, 0xFF55FFFF);
        
        context.drawTextWithShadow(client.textRenderer, "§b[Z] Heal Pulse (1⚡)", startX + 10, startY + 31, 0xFFAAFFFF);
        context.drawTextWithShadow(client.textRenderer, "§e[X] Quick Guard (1⚡)", startX + 10, startY + 42, 0xFFFFFF88);
        context.drawTextWithShadow(client.textRenderer, "§c[C] Battle Cry (2⚡)", startX + 10, startY + 53, 0xFFFF8888);
    }
}
