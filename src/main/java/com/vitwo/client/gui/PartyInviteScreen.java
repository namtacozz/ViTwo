package com.vitwo.client.gui;

import com.vitwo.network.c2s.InvitePlayerC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class PartyInviteScreen extends Screen {
    private final PlayerEntity targetPlayer;

    public PartyInviteScreen(PlayerEntity targetPlayer) {
        super(Text.translatable("vitwo.menu.tower_invite"));
        this.targetPlayer = targetPlayer;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Button: Confirm Invite
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§aSend Invite"),
                btn -> {
                    ClientPlayNetworking.send(new InvitePlayerC2SPacket(targetPlayer.getUuid()));
                    this.close();
                }
        ).dimensions(centerX - 105, centerY + 10, 100, 24).build());

        // Button: Cancel
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§cCancel"),
                btn -> this.close()
        ).dimensions(centerX + 5, centerY + 10, 100, 24).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Cobblemon-style Slate & Cyan container
        context.fill(centerX - 160, centerY - 60, centerX + 160, centerY + 45, 0xEE1E232A);
        context.drawBorder(centerX - 160, centerY - 60, 320, 105, 0xFF0FD9C2);
        context.fill(centerX - 159, centerY - 59, centerX + 159, centerY - 57, 0xFF0FD9C2);

        context.drawCenteredTextWithShadow(this.textRenderer, "§b§lCOBBLE TOWER CO-OP DUO", centerX, centerY - 48, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§fInvite §e" + targetPlayer.getName().getString() + " §fto Co-op Battle Tower?"), centerX, centerY - 28, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7(2v1 Double Battles with 100 Tiers & Regional Bosses)"), centerX, centerY - 12, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
