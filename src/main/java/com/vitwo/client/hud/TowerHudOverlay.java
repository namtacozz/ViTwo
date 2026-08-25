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
    
    // Partner & Runtime variables
    public static String partnerName = "";
    public static int partnerAliveCount = 0;
    public static float partnerHpPercent = 1.0f;
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
    private static String cachedPartnerTitle = "CO-OP PARTNER";
    private static String cachedPartnerInfo = "Name: Partner";
    private static String cachedPartnerAlive = "Alive PKM: 0/6";
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
        partnerName = "";
        partnerAliveCount = 0;
        partnerHpPercent = 1.0f;
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

        cachedFloorText = "FLOOR " + currentFloor + "/100";
        int maxCap = LevelCapManager.getMaxLevelCapForFloor(currentFloor);
        String bossName = currentBossName.isEmpty() ? "Unknown" : currentBossName;
        cachedBossAndCapText = "Next Boss: " + bossName + " | Cap: Lv." + maxCap;

        List<TowerCurseManager.TowerCurse> curses = TowerCurseManager.getInstance().getActiveCursesForFloor(currentFloor);
        String ruleText = "⚠ Rules: Competitive Clause";
        if (!curses.isEmpty()) {
            ruleText += " | 💀 " + curses.get(0).hudBadge;
        }
        cachedRuleText = ruleText;

        String pName = partnerName.isEmpty() ? "Partner" : partnerName;
        cachedPartnerInfo = "Name: " + pName;
        cachedPartnerAlive = "Alive PKM: " + partnerAliveCount + "/6";

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

        // 2. Co-op Partner Box (Top-Left)
        if (!isSolo && !isSpectating) {
            renderPartnerBox(context, client);
        }

        // 3. Battle Timer & Info (Bottom-Left)
        if (!isSpectating) {
            renderBattleInfo(context, client, height);
        } else {
            renderGhostSupportPanel(context, client, height);
        }

        // 4. Forfeit Voting Panel (Bottom-Right)
        if (forfeitVoteActive && !isSpectating) {
            renderForfeitPanel(context, client, width, height);
        }
    }

    private static void renderTopProgressBox(DrawContext context, MinecraftClient client, int screenWidth) {
        int boxW = 260;
        int boxH = 50;
        int startX = (screenWidth - boxW) / 2;
        int startY = 5;

        // Slate background with Primary Cyan border
        context.fill(startX, startY, startX + boxW, startY + boxH, 0xF212171E);
        context.drawBorder(startX, startY, boxW, boxH, 0xFF00E5FF);

        // Floor Text
        context.drawTextWithShadow(client.textRenderer, cachedFloorText, startX + 5, startY + 5, 0xFFFFFF);

        // Progress Bar
        int barW = boxW - 10;
        context.fill(startX + 5, startY + 16, startX + 5 + barW, startY + 20, 0xFF333333);
        int fillW = (int) ((Math.min(100, Math.max(1, currentFloor)) / 100f) * barW);
        context.fill(startX + 5, startY + 16, startX + 5 + fillW, startY + 20, 0xFF00E5FF);

        // Next Boss & Cap Info
        context.drawTextWithShadow(client.textRenderer, cachedBossAndCapText, startX + 5, startY + 24, 0xCCCCCC);

        // Affixes / Rules
        context.drawTextWithShadow(client.textRenderer, cachedRuleText, startX + 5, startY + 36, 0xFFFF5555);
    }

    private static void renderPartnerBox(DrawContext context, MinecraftClient client) {
        int startX = 10;
        int startY = 10;
        int boxW = 140;
        int boxH = 55;

        // Muted Purple Border
        context.fill(startX, startY, startX + boxW, startY + boxH, 0xF212171E);
        context.drawBorder(startX, startY, boxW, boxH, 0xFFE040FB);

        context.drawCenteredTextWithShadow(client.textRenderer, cachedPartnerTitle, startX + boxW / 2, startY + 5, 0xFFE040FB);
        context.drawTextWithShadow(client.textRenderer, cachedPartnerInfo, startX + 5, startY + 18, 0xFFFFFF);
        context.drawTextWithShadow(client.textRenderer, cachedPartnerAlive, startX + 5, startY + 29, 0xCCCCCC);

        // Partner Team HP Bar
        context.fill(startX + 5, startY + 42, startX + boxW - 5, startY + 46, 0xFF333333);
        int hpFill = (int) (Math.min(1.0f, Math.max(0.0f, partnerHpPercent)) * (boxW - 10));
        int hpColor = partnerHpPercent > 0.5f ? 0xFF55FF55 : (partnerHpPercent > 0.2f ? 0xFFFFFF55 : 0xFFFF5555);
        context.fill(startX + 5, startY + 42, startX + 5 + hpFill, startY + 46, hpColor);
    }

    private static void renderBattleInfo(DrawContext context, MinecraftClient client, int screenHeight) {
        int startX = 10;
        int startY = screenHeight - 20;

        if (runDurationSeconds != lastRenderedSecond || battleTurns != lastRenderedTurn || bpEarnedInRun != lastRenderedBp) {
            lastRenderedSecond = runDurationSeconds;
            lastRenderedTurn = battleTurns;
            lastRenderedBp = bpEarnedInRun;
            int min = runDurationSeconds / 60;
            int sec = runDurationSeconds % 60;
            cachedBattleInfo = String.format("⏱ %02d:%02d  |  Turn %d  |  BP in run: +%d", min, sec, battleTurns, bpEarnedInRun);
        }

        context.drawTextWithShadow(client.textRenderer, cachedBattleInfo, startX, startY, 0xFFFFFF);
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
        context.drawTextWithShadow(client.textRenderer, "Partner: " + partnerVote, startX + 5, startY + 18, 0xFFFFFF);
        
        context.drawCenteredTextWithShadow(client.textRenderer, "[F] YES, FORFEIT", startX + boxW / 4, startY + 32, 0xFFFF5555);
        context.drawCenteredTextWithShadow(client.textRenderer, "[X] NO, FIGHT", startX + boxW * 3 / 4, startY + 32, 0xFF55FF55);

        // Time bar
        context.drawTextWithShadow(client.textRenderer, "Vote Time: " + forfeitTimeLeft + "s", startX + 5, startY + 45, 0xCCCCCC);
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

        context.drawTextWithShadow(client.textRenderer, "§c● SPECTATING §7([Y] Hub)", startX + 5, startY + 5, 0xFF5555);
        
        if (ghostCharges != lastRenderedCharges) {
            lastRenderedCharges = ghostCharges;
            cachedGhostIcons = "§e" + "⚡ ".repeat(Math.max(0, ghostCharges)) + "§8" + "⚡ ".repeat(Math.max(0, maxGhostCharges - ghostCharges));
        }
        context.drawTextWithShadow(client.textRenderer, "§b👻 GHOST SUPPORT: " + cachedGhostIcons.trim(), startX + 5, startY + 18, 0x55FFFF);
        
        context.drawTextWithShadow(client.textRenderer, "§b[Z] Heal Pulse (1⚡)", startX + 10, startY + 31, 0xAAFFFF);
        context.drawTextWithShadow(client.textRenderer, "§e[X] Quick Guard (1⚡)", startX + 10, startY + 42, 0xFFFF88);
        context.drawTextWithShadow(client.textRenderer, "§c[C] Battle Cry (2⚡)", startX + 10, startY + 53, 0xFF8888);
    }
}
