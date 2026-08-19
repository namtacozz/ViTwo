package com.vitwo.client.gui;

import com.vitwo.network.c2s.LeavePartyC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TowerDashboardScreen extends Screen {
    public static boolean hasParty = false;
    public static boolean isLeader = false;
    public static String leaderName = "";
    public static String memberName = "";
    public static int currentFloor = 1;
    public static int highestCheckpoint = 1;
    public static boolean inBattle = false;
    public static boolean isSpectating = false;

    public TowerDashboardScreen() {
        super(Text.literal("CobbleTower Dashboard"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (hasParty) {
            // Button: Leave Party
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§cLeave Party"),
                    btn -> {
                        ClientPlayNetworking.send(new LeavePartyC2SPacket(true));
                        this.close();
                    }
            ).dimensions(centerX - 75, centerY + 45, 150, 24).build());
        }

        // Button: Close
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                btn -> this.close()
        ).dimensions(centerX - 50, centerY + 75, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Cobblemon-style Slate & Cyan Container
        context.fill(centerX - 130, centerY - 80, centerX + 130, centerY + 105, 0xEE1E232A);
        context.drawBorder(centerX - 130, centerY - 80, 260, 185, 0xFF0FD9C2);
        context.fill(centerX - 129, centerY - 79, centerX + 129, centerY - 77, 0xFF0FD9C2);

        context.drawCenteredTextWithShadow(this.textRenderer, "§b§lCOBBLE TOWER DASHBOARD", centerX, centerY - 70, 0xFFFFFF);

        if (!hasParty) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Not in an active tower party."), centerX, centerY - 30, 0xCCCCCC);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§bShift + Right-Click §7partner to invite!"), centerX, centerY - 10, 0x0FD9C2);
        } else {
            int y = centerY - 50;
            context.drawTextWithShadow(this.textRenderer, "§bParty Leader: §f" + leaderName, centerX - 110, y, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "§bPartner: §f" + memberName, centerX - 110, y + 15, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "§bCurrent Floor: §f" + currentFloor + " / 100", centerX - 110, y + 30, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "§bCheckpoint: §fFloor " + highestCheckpoint, centerX - 110, y + 45, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "§bStatus: " + (inBattle ? "§aBattle Active" : "§7Waiting at Gateway"), centerX - 110, y + 60, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
