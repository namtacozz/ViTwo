package com.vitwo.client.gui;

import com.vitwo.network.c2s.RestChoiceC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class RestFloorScreen extends Screen {
    private final int floor;

    public RestFloorScreen(int floor) {
        super(Text.translatable("vitwo.tower.rest_floor.title", floor));
        this.floor = floor;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Card 1 Button: Full Team Rest (Choice 1)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a§lSELECT REST"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(1));
                    this.close();
                }
        ).dimensions(centerX - 240, centerY + 30, 150, 24).build());

        // Card 2 Button: War Preparation (Choice 2)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§c§lSELECT BUFF"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(2));
                    this.close();
                }
        ).dimensions(centerX - 75, centerY + 30, 150, 24).build());

        // Card 3 Button: Treasure Cache (Choice 3)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§6§lSELECT LOOT"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(3));
                    this.close();
                }
        ).dimensions(centerX + 90, centerY + 30, 150, 24).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Dim background
        context.fill(0, 0, this.width, this.height, 0x90000000);

        // Cobblemon-style Slate & Cyan container
        int containerW = 510;
        int containerH = 180;
        int left = centerX - (containerW / 2);
        int top = centerY - 95;

        context.fill(left, top, left + containerW, top + containerH, 0xEE1E232A);
        context.drawBorder(left, top, containerW, containerH, 0xFF0FD9C2);
        context.fill(left + 1, top + 1, left + containerW - 1, top + 3, 0xFF0FD9C2);

        // Title Header
        context.drawCenteredTextWithShadow(this.textRenderer, "§b§l❖ REST STATION — FLOOR " + floor + " ❖", centerX, top + 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "§a✔ Base Heal (+25% HP, +50% PP) applied! §7Choose 1 additional boon:", centerX, top + 24, 0xCCCCCC);

        int cardY = top + 42;
        int cardH = 75;

        // Card 1: Full Team Rest
        drawRestCard(context, centerX - 240, cardY, 150, cardH,
                0x44225533, 0xFF55FF55,
                "§a§lFULL TEAM REST",
                "§f• Revive all fainted PKM",
                "§f• Restore 100% HP & PP",
                "§a✔ Complete Team Refresh"
        );

        // Card 2: War Preparation
        drawRestCard(context, centerX - 75, cardY, 150, cardH,
                0x44552222, 0xFFFF5555,
                "§c§lWAR PREPARATION",
                "§f• +10% Attack & Sp.Atk",
                "§f• +10% Speed Boost",
                "§c✔ Active for 5 Floors"
        );

        // Card 3: Treasure Cache
        drawRestCard(context, centerX + 90, cardY, 150, cardH,
                0x44554422, 0xFFFFBB33,
                "§6§lTREASURE CACHE",
                "§f• +250 Bonus BP",
                "§f• 2x Supply Crates",
                "§6✔ High Value Rewards"
        );

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawRestCard(DrawContext context, int x, int y, int w, int h, int bg, int border, String title, String l1, String l2, String l3) {
        context.fill(x, y, x + w, y + h, bg);
        context.drawBorder(x, y, w, h, border);

        context.drawCenteredTextWithShadow(this.textRenderer, title, x + (w / 2), y + 7, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, l1, x + 8, y + 23, 0xEEEEEE);
        context.drawTextWithShadow(this.textRenderer, l2, x + 8, y + 36, 0xEEEEEE);
        context.drawTextWithShadow(this.textRenderer, l3, x + 8, y + 51, 0xFFEE88);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
