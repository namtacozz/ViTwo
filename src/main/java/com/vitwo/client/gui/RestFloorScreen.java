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

        // Button 1: Team Rest Recovery
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§b§lTEAM RECOVERY\n§7(+10% Fainted, +50% Alive, 100% PP)"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(1));
                    this.close();
                }
        ).dimensions(centerX - 165, centerY - 15, 160, 42).build());

        // Button 2: Mystery Loot Cache
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§e§lMYSTERY LOOT\n§7(Rare Items & Stat Boosts)"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(2));
                    this.close();
                }
        ).dimensions(centerX + 5, centerY - 15, 160, 42).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Cobblemon-style Slate & Cyan container
        context.fill(centerX - 180, centerY - 80, centerX + 180, centerY + 65, 0xEE1E232A);
        context.drawBorder(centerX - 180, centerY - 80, 360, 145, 0xFF0FD9C2);
        context.fill(centerX - 179, centerY - 79, centerX + 179, centerY - 77, 0xFF0FD9C2);

        context.drawCenteredTextWithShadow(this.textRenderer, "§b§lREST STATION - FLOOR " + floor, centerX, centerY - 65, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Choose 1 boon before ascending to the next tier:"), centerX, centerY - 45, 0xCCCCCC);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
