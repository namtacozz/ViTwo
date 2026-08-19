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

        // Button 1: Full Team Rest (Choice 1)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a§lFULL TEAM REST\n§7(100% HP Revive & All PP)"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(1));
                    this.close();
                }
        ).dimensions(centerX - 240, centerY - 15, 155, 42).build());

        // Button 2: War Preparation (Choice 2)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§c§lWAR PREPARATION\n§7(+10% Boost for 5 Floors)"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(2));
                    this.close();
                }
        ).dimensions(centerX - 77, centerY - 15, 155, 42).build());

        // Button 3: Treasure Cache (Choice 3)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§6§lTREASURE CACHE\n§7(Bonus BP & Rare Items)"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(3));
                    this.close();
                }
        ).dimensions(centerX + 85, centerY - 15, 155, 42).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Cobblemon-style Slate & Cyan container
        context.fill(centerX - 255, centerY - 80, centerX + 255, centerY + 65, 0xEE1E232A);
        context.drawBorder(centerX - 255, centerY - 80, 510, 145, 0xFF0FD9C2);
        context.fill(centerX - 254, centerY - 79, centerX + 254, centerY - 77, 0xFF0FD9C2);

        context.drawCenteredTextWithShadow(this.textRenderer, "§b§l❖ REST STATION — FLOOR " + floor + " ❖", centerX, centerY - 65, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§a✔ Base Heal (+25% HP, +50% PP) applied! §7Choose 1 additional boon:"), centerX, centerY - 45, 0xCCCCCC);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
