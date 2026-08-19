package com.vitwo.client.gui;

import com.vitwo.network.c2s.StartTowerC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TowerEntryScreen extends Screen {
    private final int highestCheckpoint;
    private final boolean isLeader;
    private final String partnerName;
    private int selectedCheckpoint = 1;

    private static final int[] CHECKPOINTS = {1, 10, 25, 50, 75, 90};

    public TowerEntryScreen(int highestCheckpoint, boolean isLeader, String partnerName) {
        super(Text.translatable("vitwo.tower.entry.title"));
        this.highestCheckpoint = highestCheckpoint;
        this.isLeader = isLeader;
        this.partnerName = partnerName;
        this.selectedCheckpoint = highestCheckpoint;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Checkpoint Selection Buttons in a row
        int startX = centerX - 165;
        for (int i = 0; i < CHECKPOINTS.length; i++) {
            int cp = CHECKPOINTS[i];
            boolean unlocked = cp <= highestCheckpoint;

            ButtonWidget cpBtn = ButtonWidget.builder(
                    Text.literal((unlocked ? "§a" : "§7🔒 ") + "Tầng " + cp),
                    btn -> this.selectedCheckpoint = cp
            ).dimensions(startX + (i * 56), centerY - 25, 52, 24).build();

            cpBtn.active = unlocked;
            this.addDrawableChild(cpBtn);
        }

        // Leader Start Button
        if (isLeader) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§a§l" + Text.translatable("vitwo.tower.entry.start").getString()),
                    btn -> {
                        ClientPlayNetworking.send(new StartTowerC2SPacket(false, selectedCheckpoint));
                        this.close();
                    }
            ).dimensions(centerX - 100, centerY + 25, 200, 30).build());
        }

        // Close Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Đóng"),
                btn -> this.close()
        ).dimensions(centerX - 50, centerY + 65, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Background box
        context.fill(centerX - 175, centerY - 80, centerX + 175, centerY + 95, 0xB0000000);

        context.drawCenteredTextWithShadow(this.textRenderer, "§6§lCỔNG VÀO THÁP ĐẤU TRƯỜNG (COBBLE TOWER)", centerX, centerY - 70, 0xFFAA00);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§fĐồng đội: §e" + partnerName + " §7| §fMốc chọn: §aTầng " + selectedCheckpoint), centerX, centerY - 50, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("vitwo.tower.entry.select_checkpoint"), centerX, centerY - 38, 0xCCCCCC);

        if (!isLeader) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eĐang chờ Đội trưởng bắt đầu trận đấu..."), centerX, centerY + 35, 0xFFFF55);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
