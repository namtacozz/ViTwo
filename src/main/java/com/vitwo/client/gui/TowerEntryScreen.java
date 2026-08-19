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
        super(Text.literal("CobbleTower Gateway"));
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
            boolean isSelected = (cp == selectedCheckpoint);

            String label = (unlocked ? (isSelected ? "§b§l" : "§f") : "§8🔒 ") + "F." + cp;
            ButtonWidget cpBtn = ButtonWidget.builder(
                    Text.literal(label),
                    btn -> this.selectedCheckpoint = cp
            ).dimensions(startX + (i * 56), centerY - 25, 52, 24).build();

            cpBtn.active = unlocked;
            this.addDrawableChild(cpBtn);
        }

        // Leader Start Button
        if (isLeader) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§a§lSTART RUN (FLOOR " + selectedCheckpoint + ")"),
                    btn -> {
                        ClientPlayNetworking.send(new StartTowerC2SPacket(false, selectedCheckpoint));
                        this.close();
                    }
            ).dimensions(centerX - 100, centerY + 25, 200, 30).build());
        }

        // Close Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                btn -> this.close()
        ).dimensions(centerX - 50, centerY + 65, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Cobblemon-style Slate & Cyan Container
        context.fill(centerX - 175, centerY - 80, centerX + 175, centerY + 95, 0xEE1E232A);
        context.drawBorder(centerX - 175, centerY - 80, 350, 175, 0xFF0FD9C2);
        context.fill(centerX - 174, centerY - 79, centerX + 174, centerY - 77, 0xFF0FD9C2);

        context.drawCenteredTextWithShadow(this.textRenderer, "§b§lCOBBLE TOWER GATEWAY", centerX, centerY - 70, 0xFFFFFF);
        String pText = partnerName.isEmpty() ? "None" : partnerName;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§bPartner: §f" + pText + " §7| §bSelected: §fFloor " + selectedCheckpoint), centerX, centerY - 50, 0xFFFFFF);

        if (!isLeader) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Waiting for Party Leader to start the run..."), centerX, centerY + 35, 0xAAAAAA);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
