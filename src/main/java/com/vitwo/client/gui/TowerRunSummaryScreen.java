package com.vitwo.client.gui;

import com.vitwo.client.gui.widget.TowerButton;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.text.Text;

public class TowerRunSummaryScreen extends AbstractTowerScreen {
    private final int floor;
    private final boolean isVictory;
    private final boolean isTrueRun;
    private final int durationSeconds;
    private final int totalTurns;
    private final int totalFaints;
    private final int bpEarned;
    private final int newHighestFloor;

    public TowerRunSummaryScreen(int floor, boolean isVictory, boolean isTrueRun, int durationSeconds, int totalTurns, int totalFaints, int bpEarned, int newHighestFloor) {
        super(Text.literal("Run Summary"));
        this.floor = floor;
        this.isVictory = isVictory;
        this.isTrueRun = isTrueRun;
        this.durationSeconds = durationSeconds;
        this.totalTurns = totalTurns;
        this.totalFaints = totalFaints;
        this.bpEarned = bpEarned;
        this.newHighestFloor = newHighestFloor;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addDrawableChild(TowerButton.towerBuilder(Text.literal("§fClose Summary"), btn -> this.close())
                .dimensions(centerX - 80, centerY + 80, 160, 22)
                .style(TowerButton.ButtonStyle.DEFAULT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Container Window
        int borderColor = isVictory ? TowerTheme.SECONDARY_GOLD : TowerTheme.DANGER_RED;
        context.fill(centerX - 160, centerY - 110, centerX + 160, centerY + 115, TowerTheme.PANEL_BACKGROUND);
        context.drawBorder(centerX - 160, centerY - 110, 320, 225, borderColor);
        context.fill(centerX - 159, centerY - 109, centerX + 159, centerY - 106, borderColor);

        super.render(context, mouseX, mouseY, delta);

        // Header Title
        String title = isVictory ? "§6§l🏆 TOWER CONQUERED! 🏆" : "§c§l💀 RUN DEFEATED 💀";
        context.drawCenteredTextWithShadow(this.textRenderer, title, centerX, centerY - 95, 0xFFFFFFFF);

        // Run Type Badge
        String runTypeBadge = isTrueRun ? "§a[★ TRUE RUN]" : "§e[⚡ CHECKPOINT RUN]";
        context.drawCenteredTextWithShadow(this.textRenderer, runTypeBadge, centerX, centerY - 80, 0xFFFFFFFF);

        // Stats Lines
        int min = durationSeconds / 60;
        int sec = durationSeconds % 60;
        String timeStr = String.format("%02d:%02d", min, sec);

        context.drawTextWithShadow(this.textRenderer, "§7Floor Reached:", centerX - 120, centerY - 55, 0xFFCCCCCC);
        context.drawTextWithShadow(this.textRenderer, "§eFloor " + floor + (isVictory ? " (Cleared)" : ""), centerX + 20, centerY - 55, 0xFFFFFFFF);

        context.drawTextWithShadow(this.textRenderer, "§7Total Time:", centerX - 120, centerY - 38, 0xFFCCCCCC);
        context.drawTextWithShadow(this.textRenderer, "§b" + timeStr, centerX + 20, centerY - 38, 0xFFFFFFFF);

        context.drawTextWithShadow(this.textRenderer, "§7Total Turns:", centerX - 120, centerY - 21, 0xFFCCCCCC);
        context.drawTextWithShadow(this.textRenderer, "§f" + totalTurns + " turns", centerX + 20, centerY - 21, 0xFFFFFFFF);

        context.drawTextWithShadow(this.textRenderer, "§7Pokémon Fainted:", centerX - 120, centerY - 4, 0xFFCCCCCC);
        context.drawTextWithShadow(this.textRenderer, "§c" + totalFaints, centerX + 20, centerY - 4, 0xFFFFFFFF);

        context.drawTextWithShadow(this.textRenderer, "§7Battle Points Earned:", centerX - 120, centerY + 13, 0xFFCCCCCC);
        context.drawTextWithShadow(this.textRenderer, "§6+" + bpEarned + " BP", centerX + 20, centerY + 13, 0xFFFFD700);

        context.drawTextWithShadow(this.textRenderer, "§7Personal Best Floor:", centerX - 120, centerY + 30, 0xFFCCCCCC);
        context.drawTextWithShadow(this.textRenderer, "§dFloor " + newHighestFloor, centerX + 20, centerY + 30, 0xFFFFFFFF);

        // Motivational / Footer note
        String note = isVictory ? "§aCongratulations on conquering all 100 floors!" : "§7Train hard and challenge the Tower again!";
        context.drawCenteredTextWithShadow(this.textRenderer, note, centerX, centerY + 58, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
