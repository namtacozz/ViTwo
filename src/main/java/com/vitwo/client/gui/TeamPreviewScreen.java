package com.vitwo.client.gui;

import com.vitwo.network.c2s.ReadyTeamPreviewC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TeamPreviewScreen extends Screen {
    private final int floor;
    private final int totalDuration;
    private final String opponentName;
    private final String opponentTitle;
    private final List<String> opponentTeam;
    private final List<String> playerTeam;

    private final List<Integer> currentOrder = new ArrayList<>();
    private long startTimestamp;
    private int selectedSlot = -1;
    private boolean submitted = false;

    public TeamPreviewScreen(int floor, int durationSeconds, String opponentName, String opponentTitle, List<String> opponentTeam, List<String> playerTeam) {
        super(Text.literal("Team Preview"));
        this.floor = floor;
        this.totalDuration = durationSeconds > 0 ? durationSeconds : 10;
        this.opponentName = opponentName;
        this.opponentTitle = opponentTitle;
        this.opponentTeam = opponentTeam != null ? opponentTeam : List.of();
        this.playerTeam = playerTeam != null ? new ArrayList<>(playerTeam) : new ArrayList<>();

        for (int i = 0; i < this.playerTeam.size(); i++) {
            currentOrder.add(i);
        }
    }

    @Override
    protected void init() {
        this.startTimestamp = System.currentTimeMillis();
        int centerX = this.width / 2;
        int bottomY = this.height - 38;

        // Ready / Start Now Button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§a§l✔ READY / START NOW"),
                btn -> submitAndClose()
        ).dimensions(centerX - 90, bottomY, 180, 26).build());
    }

    private void submitAndClose() {
        if (submitted) return;
        submitted = true;
        ClientPlayNetworking.send(new ReadyTeamPreviewC2SPacket(currentOrder));
        this.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int panelY = 55;

        // Check clicks on Player Team slots (Right side: centerX + 10 to centerX + 180)
        for (int i = 0; i < currentOrder.size(); i++) {
            int slotY = panelY + 22 + (i * 24);
            if (mouseX >= centerX + 10 && mouseX <= centerX + 190 && mouseY >= slotY && mouseY <= slotY + 20) {
                if (selectedSlot == -1) {
                    selectedSlot = i;
                } else {
                    // Swap slots
                    int temp = currentOrder.get(selectedSlot);
                    currentOrder.set(selectedSlot, currentOrder.get(i));
                    currentOrder.set(i, temp);
                    selectedSlot = -1;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        long elapsedSec = (System.currentTimeMillis() - startTimestamp) / 1000L;
        long remainingSec = Math.max(0, totalDuration - elapsedSec);

        if (remainingSec <= 0 && !submitted) {
            submitAndClose();
            return;
        }

        int centerX = this.width / 2;

        // Background container: Slate & Cyan Cobblemon aesthetic
        int w = 420;
        int h = 230;
        int left = centerX - (w / 2);
        int top = 25;

        context.fill(left, top, left + w, top + h, 0xEE1E232A);
        context.drawBorder(left, top, w, h, 0xFF0FD9C2);
        context.fill(left + 1, top + 1, left + w - 1, top + 3, 0xFF0FD9C2);

        // Header Title & Countdown
        String timerColor = remainingSec <= 5 ? "§c§l" : (remainingSec <= 10 ? "§e§l" : "§a§l");
        context.drawCenteredTextWithShadow(this.textRenderer, "§b§l❖ BATTLE TEAM PREVIEW — FLOOR " + floor + " ❖", centerX, top + 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Auto-start in: " + timerColor + remainingSec + "s §7| Click slots to swap Leads", centerX, top + 22, 0xDDDDDD);

        // Left Panel: Opponent Roster
        context.fill(left + 10, top + 36, centerX - 10, top + h - 45, 0x6611161D);
        context.drawBorder(left + 10, top + 36, (centerX - left - 20), (h - 81), 0xFF445566);
        context.drawTextWithShadow(this.textRenderer, "§c§l⚔ OPPONENT: " + opponentName, left + 16, top + 42, 0xFF7777);
        if (!opponentTitle.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "§8" + opponentTitle, left + 16, top + 53, 0x888888);
        }

        int oppY = top + 66;
        for (int i = 0; i < opponentTeam.size() && i < 6; i++) {
            String mon = opponentTeam.get(i);
            context.fill(left + 14, oppY, centerX - 14, oppY + 18, 0x44222B38);
            context.drawTextWithShadow(this.textRenderer, "§e● " + mon, left + 18, oppY + 5, 0xFFFFFF);
            oppY += 21;
        }

        // Right Panel: Player Roster & Ordering
        context.fill(centerX + 10, top + 36, left + w - 10, top + h - 45, 0x6611161D);
        context.drawBorder(centerX + 10, top + 36, (centerX - left - 20), (h - 81), 0xFF0FD9C2);
        context.drawTextWithShadow(this.textRenderer, "§a§l🛡 YOUR ROSTER §7(Swap Leads & Bench)", centerX + 16, top + 42, 0x55FF55);

        int plyY = top + 56;
        for (int i = 0; i < currentOrder.size() && i < 6; i++) {
            int originalIdx = currentOrder.get(i);
            String mon = (originalIdx < playerTeam.size()) ? playerTeam.get(originalIdx) : "Slot " + (i + 1);
            boolean isSelected = (selectedSlot == i);

            int slotColor = isSelected ? 0xFF0FD9C2 : (i < 2 ? 0x66334A3E : 0x44222B38);
            context.fill(centerX + 14, plyY, left + w - 14, plyY + 18, slotColor);
            if (isSelected) {
                context.drawBorder(centerX + 14, plyY, (left + w - centerX - 28), 18, 0xFFFFFF);
            }

            String tag = (i == 0) ? "§a[LEAD 1] " : (i == 1 ? "§a[LEAD 2] " : "§7[BENCH " + (i - 1) + "] ");
            context.drawTextWithShadow(this.textRenderer, tag + "§f" + mon, centerX + 18, plyY + 5, 0xFFFFFF);
            plyY += 21;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
