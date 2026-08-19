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
        super(Text.translatable("vitwo.tower.dashboard.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (hasParty) {
            // Button: Leave Party
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§c" + Text.translatable("vitwo.tower.dashboard.leave").getString()),
                    btn -> {
                        ClientPlayNetworking.send(new LeavePartyC2SPacket(true));
                        this.close();
                    }
            ).dimensions(centerX - 75, centerY + 45, 150, 24).build());
        }

        // Button: Close
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Đóng"),
                btn -> this.close()
        ).dimensions(centerX - 50, centerY + 75, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Background box
        context.fill(centerX - 130, centerY - 80, centerX + 130, centerY + 105, 0xB0000000);

        context.drawCenteredTextWithShadow(this.textRenderer, "§6§lCOBBLE TOWER DASHBOARD", centerX, centerY - 70, 0xFFAA00);

        if (!hasParty) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Bạn hiện chưa ở trong đội leo tháp nào."), centerX, centerY - 30, 0xCCCCCC);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§fShift + Chuột phải §7vào bạn bè để gửi lời mời!"), centerX, centerY - 10, 0xAAAAAA);
        } else {
            int y = centerY - 50;
            context.drawTextWithShadow(this.textRenderer, "§eĐội trưởng: §f" + leaderName, centerX - 110, y, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "§eThành viên: §f" + memberName, centerX - 110, y + 15, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "§eTầng hiện tại: §f" + currentFloor + " / 100", centerX - 110, y + 30, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "§eCheckpoint đã mở: §fTầng " + highestCheckpoint, centerX - 110, y + 45, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "§eTrạng thái: " + (inBattle ? "§cĐang Trong Trận Đấu" : "§aĐang Chờ Tại Cổng"), centerX - 110, y + 60, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
