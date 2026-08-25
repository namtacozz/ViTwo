package com.vitwo.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class TowerBossIntroOverlay {
    private static boolean active = false;
    private static int floor = 1;
    private static String bossName = "";
    private static String bossTitle = "";
    private static String quote = "";
    private static boolean isApex = false;
    private static int ticksRemaining = 0;
    private static final int TOTAL_DURATION = 90; // 4.5 seconds

    public static void showIntro(int floorNum, String name, String title, String bossQuote, boolean apex) {
        floor = floorNum;
        bossName = name != null ? name : "Tower Sovereign";
        bossTitle = title != null ? title : "Apex Guardian";
        quote = bossQuote != null ? bossQuote : "";
        isApex = apex;
        ticksRemaining = TOTAL_DURATION;
        active = true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (apex || floor >= 90) {
                client.player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.9f);
            } else {
                client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
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
        int height = client.getWindow().getScaledHeight();

        float progress = 1.0f - ((float) ticksRemaining / (float) TOTAL_DURATION);
        float alpha = 1.0f;
        if (progress < 0.15f) {
            alpha = progress / 0.15f; // Fade in
        } else if (progress > 0.75f) {
            alpha = (1.0f - progress) / 0.25f; // Fade out
        }
        alpha = MathHelper.clamp(alpha, 0.0f, 1.0f);

        int alphaHex = (int) (alpha * 255);
        if (alphaHex <= 0) return;

        int barHeight = Math.min(48, height / 6);
        int barColor = (alphaHex << 24); // Solid black cinematic letterbox bars

        // Top & Bottom Letterbox Bars
        context.fill(0, 0, width, barHeight, barColor);
        context.fill(0, height - barHeight, width, height, barColor);

        // Center Banner Box
        int bannerY = height / 2 - 40;
        int bannerH = 80;
        int bannerBg = ((int) (alpha * 0.85f * 255) << 24) | 0x0A0D12;
        context.fill(0, bannerY, width, bannerY + bannerH, bannerBg);

        int borderColor = isApex ? 0xFFFF2222 : 0xFFFFD700;
        int borderAlpha = ((int) (alpha * 255) << 24) | (borderColor & 0x00FFFFFF);
        context.fill(0, bannerY, width, bannerY + 2, borderAlpha);
        context.fill(0, bannerY + bannerH - 2, width, bannerY + bannerH, borderAlpha);

        // Floor / Apex Tag
        String tag = isApex ? "§4§l❖ APEX CLIMAX — FLOOR " + floor + " ❖" : "§6§l❖ BOSS CHALLENGE — FLOOR " + floor + " ❖";
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(tag), width / 2, bannerY + 10, 0xFFFFFF);

        // Boss Name (Large & Glowing)
        String styledName = isApex ? "§c§l" + bossName.toUpperCase() : "§e§l" + bossName.toUpperCase();
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(styledName), width / 2, bannerY + 26, 0xFFFFFF);

        // Boss Title Subtitle
        String styledTitle = "§b« " + bossTitle + " »";
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(styledTitle), width / 2, bannerY + 42, 0x55FFFF);

        // Quote
        if (!quote.isEmpty()) {
            context.drawCenteredTextWithShadow(client.textRenderer, Text.literal("§7\"" + quote + "\""), width / 2, bannerY + 58, 0xAAAAAA);
        }
    }
}
