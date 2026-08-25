package com.vitwo.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class TowerBattleGradeOverlay {
    private static boolean active = false;
    private static int floor = 1;
    private static String grade = "S";
    private static int bonusBp = 0;
    private static int turns = 0;
    private static int faints = 0;
    private static int ticksRemaining = 0;
    private static final int TOTAL_DURATION = 70; // 3.5 seconds

    public static void showGrade(int floorNum, String rankGrade, int bonus, int turnCount, int faintCount) {
        floor = floorNum;
        grade = rankGrade != null ? rankGrade.toUpperCase() : "S";
        bonusBp = bonus;
        turns = turnCount;
        faints = faintCount;
        ticksRemaining = TOTAL_DURATION;
        active = true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if ("S".equals(grade)) {
                client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            } else {
                client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    public static void tick() {
        if (active && ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                active = false;
            }
        }
    }

    public static void render(DrawContext context, float tickDelta) {
        if (!active || ticksRemaining <= 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int width = client.getWindow().getScaledWidth();

        float progress = 1.0f - ((float) ticksRemaining / (float) TOTAL_DURATION);
        float alpha = 1.0f;
        if (progress < 0.10f) {
            alpha = progress / 0.10f;
        } else if (progress > 0.70f) {
            alpha = (1.0f - progress) / 0.30f;
        }
        alpha = MathHelper.clamp(alpha, 0.0f, 1.0f);

        int alphaHex = (int) (alpha * 255);
        if (alphaHex <= 0) return;

        int boxW = 200;
        int boxH = 60;
        int startX = (width - boxW) / 2;
        int startY = 60; // Just below the top progress bar

        // Background box
        int bg = ((int) (alpha * 0.90f * 255) << 24) | 0x10151C;
        context.fill(startX, startY, startX + boxW, startY + boxH, bg);

        int borderColor = switch (grade) {
            case "S" -> 0xFFFFD700; // Gold
            case "A" -> 0xFF55FF55; // Green
            case "B" -> 0xFF00E5FF; // Cyan
            default -> 0xFFAAAAAA;  // Gray
        };
        int borderWithAlpha = (alphaHex << 24) | (borderColor & 0x00FFFFFF);
        context.drawBorder(startX, startY, boxW, boxH, borderWithAlpha);

        // Grade Badge Icon on Left
        String gradeLabel = switch (grade) {
            case "S" -> "§6§l★ S ★";
            case "A" -> "§a§l[ A ]";
            case "B" -> "§b§l[ B ]";
            default -> "§7§l[ C ]";
        };
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(gradeLabel), startX + 35, startY + 22, 0xFFFFFF);

        // Title and evaluation
        String title = "§e§lBATTLE GRADE: FLOOR " + floor;
        context.drawTextWithShadow(client.textRenderer, Text.literal(title), startX + 70, startY + 10, 0xFFFFFF);

        String perf = "§7Turns: §e" + turns + " §7| Faints: §c" + faints;
        context.drawTextWithShadow(client.textRenderer, Text.literal(perf), startX + 70, startY + 24, 0xCCCCCC);

        String bonusText = bonusBp > 0 ? "§a+" + bonusBp + " BP Rank Bonus!" : "§7Standard Clear";
        context.drawTextWithShadow(client.textRenderer, Text.literal(bonusText), startX + 70, startY + 38, 0x55FF55);
    }
}
