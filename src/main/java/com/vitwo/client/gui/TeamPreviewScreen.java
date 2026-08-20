package com.vitwo.client.gui;

import com.vitwo.network.c2s.ReadyTeamPreviewC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<String, Object> stateCache = new HashMap<>();

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
            int slotY = panelY + 20 + (i * 25);
            if (mouseX >= centerX + 10 && mouseX <= centerX + 200 && mouseY >= slotY && mouseY <= slotY + 23) {
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
        int w = 440;
        int h = 236;
        int left = centerX - (w / 2);
        int top = 22;

        context.fill(left, top, left + w, top + h, 0xEE1E232A);
        context.drawBorder(left, top, w, h, 0xFF0FD9C2);
        context.fill(left + 1, top + 1, left + w - 1, top + 3, 0xFF0FD9C2);

        // Header Title & Countdown
        String timerColor = remainingSec <= 5 ? "§c§l" : (remainingSec <= 10 ? "§e§l" : "§a§l");
        context.drawCenteredTextWithShadow(this.textRenderer, "§b§l❖ BATTLE TEAM PREVIEW — FLOOR " + floor + " ❖", centerX, top + 7, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Auto-start in: " + timerColor + remainingSec + "s §7| Click slots to swap Leads & Bench", centerX, top + 20, 0xDDDDDD);

        // Left Panel: Opponent Roster
        int panelW = (w / 2) - 16;
        context.fill(left + 10, top + 34, left + 10 + panelW, top + h - 45, 0x6611161D);
        context.drawBorder(left + 10, top + 34, panelW, (h - 79), 0xFF445566);
        context.drawTextWithShadow(this.textRenderer, "§c§l⚔ OPPONENT: " + opponentName, left + 16, top + 40, 0xFF7777);
        if (!opponentTitle.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "§8" + opponentTitle, left + 16, top + 50, 0x888888);
        }

        int oppY = top + 64;
        for (int i = 0; i < opponentTeam.size() && i < 6; i++) {
            String mon = opponentTeam.get(i);
            context.fill(left + 14, oppY, left + 10 + panelW - 4, oppY + 22, 0x44222B38);
            context.drawBorder(left + 14, oppY, panelW - 18, 22, 0x33445566);

            renderPokemonThumbnail(context, mon, left + 26, oppY + 11);
            String formattedMon = capitalize(mon);
            context.drawTextWithShadow(this.textRenderer, "§e" + (i + 1) + ". §f" + formattedMon, left + 42, oppY + 7, 0xFFFFFF);
            oppY += 25;
        }

        // Right Panel: Player Roster & Ordering
        context.fill(centerX + 6, top + 34, centerX + 6 + panelW, top + h - 45, 0x6611161D);
        context.drawBorder(centerX + 6, top + 34, panelW, (h - 79), 0xFF0FD9C2);
        context.drawTextWithShadow(this.textRenderer, "§a§l🛡 YOUR ROSTER §7(Swap Leads & Order)", centerX + 12, top + 40, 0x55FF55);

        int plyY = top + 64;
        for (int i = 0; i < currentOrder.size() && i < 6; i++) {
            int originalIdx = currentOrder.get(i);
            String mon = (originalIdx < playerTeam.size()) ? playerTeam.get(originalIdx) : "Slot " + (i + 1);
            boolean isSelected = (selectedSlot == i);

            int slotColor = isSelected ? 0x880FD9C2 : (i == 0 ? 0x551B4D3E : 0x44222B38);
            int borderColor = isSelected ? 0xFFFFFF55 : (i == 0 ? 0xFF0FD9C2 : 0x33445566);

            context.fill(centerX + 10, plyY, centerX + 6 + panelW - 4, plyY + 22, slotColor);
            context.drawBorder(centerX + 10, plyY, panelW - 18, 22, borderColor);

            renderPokemonThumbnail(context, mon, centerX + 22, plyY + 11);

            String roleTag = (i == 0) ? "§6[LEAD 1] " : (i == 1 ? "§e[LEAD 2] " : "§7[BENCH] ");
            String formattedMon = capitalize(mon);
            context.drawTextWithShadow(this.textRenderer, roleTag + "§f" + formattedMon, centerX + 38, plyY + 7, isSelected ? 0xFFFF55 : 0xFFFFFF);

            plyY += 25;
        }
    }

    private void renderPokemonThumbnail(DrawContext context, String speciesName, int x, int y) {
        if (speciesName == null || speciesName.isEmpty()) return;

        String cleanSpecies = speciesName.toLowerCase().replaceAll("[^a-z0-9_]", "");
        Identifier speciesId = Identifier.of("cobblemon", cleanSpecies);

        try {
            Object floatingState = stateCache.computeIfAbsent(cleanSpecies, k -> {
                try {
                    Class<?> clazz = Class.forName("com.cobblemon.mod.common.client.render.models.blockbench.FloatingState");
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    return null;
                }
            });

            if (floatingState != null) {
                Class<?> guiUtilsClass = Class.forName("com.cobblemon.mod.common.api.gui.GuiUtilsKt");
                Class<?> posableStateClass = Class.forName("com.cobblemon.mod.common.client.render.models.blockbench.PosableState");

                Method drawMethod = guiUtilsClass.getMethod("drawPosablePortrait",
                        Identifier.class,
                        net.minecraft.client.util.math.MatrixStack.class,
                        float.class,
                        float.class,
                        posableStateClass,
                        float.class
                );
                drawMethod.invoke(null, speciesId, context.getMatrices(), (float) x, (float) y, floatingState, 0.42f);
                return;
            }
        } catch (Throwable ignored) {}

        // Fallback Pokéball circle icon
        context.fill(x - 5, y - 5, x + 5, y, 0xFFEE4444);
        context.fill(x - 5, y, x + 5, y + 5, 0xFFDDDDDD);
        context.drawBorder(x - 5, y - 5, 10, 10, 0xFF222222);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
