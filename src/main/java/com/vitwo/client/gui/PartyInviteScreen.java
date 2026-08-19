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
                Text.literal("§aGửi Lời Mời"),
                btn -> {
                    ClientPlayNetworking.send(new InvitePlayerC2SPacket(targetPlayer.getUuid()));
                    this.close();
                }
        ).dimensions(centerX - 105, centerY + 10, 100, 24).build());

        // Button: Cancel
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§cHủy"),
                btn -> this.close()
        ).dimensions(centerX + 5, centerY + 10, 100, 24).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, "§6§lCOBBLE TOWER DUO", centerX, centerY - 45, 0xFFAA00);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§fBạn có muốn mời §e" + targetPlayer.getName().getString() + " §fcùng tham gia leo tháp?"), centerX, centerY - 25, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7(Đấu đôi 2v1 với 100 tầng thử thách & 4 Battle Gimmicks)"), centerX, centerY - 10, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
