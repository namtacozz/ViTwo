package com.vitwo.client.gui;

import com.vitwo.battle.LevelCapManager;
import com.vitwo.network.c2s.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TowerHubScreen extends Screen {
    public static boolean hasParty = false;
    public static boolean isLeader = false;
    public static String leaderName = "";
    public static String memberName = "";
    public static int currentFloor = 1;
    public static int soloCheckpoint = 1;
    public static int duoCheckpoint = 1;
    public static boolean inBattle = false;
    public static boolean isSpectating = false;
    public static String pendingInviterName = "";
    public static boolean inTowerSession = false;
    public static int forfeitVotes = 0;

    private static final int[] CHECKPOINTS = {1, 10, 25, 50, 75, 90};
    private int selectedCheckpoint = 1;
    private boolean isSoloTab = true;

    public TowerHubScreen() {
        super(Text.translatable("vitwo.tower.hub.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (hasParty) {
            isSoloTab = false;
        }

        // Tab Selectors
        this.addDrawableChild(create3DButton(
                Text.literal(isSoloTab ? "§b§lSOLO CHALLENGE" : "§7SOLO CHALLENGE"),
                centerX - 160, centerY - 80, 155, 22,
                btn -> {
                    this.isSoloTab = true;
                    this.clearAndInit();
                }
        ));

        this.addDrawableChild(create3DButton(
                Text.literal(!isSoloTab ? "§d§lCO-OP DUO" : "§7CO-OP DUO"),
                centerX + 5, centerY - 80, 155, 22,
                btn -> {
                    this.isSoloTab = false;
                    this.clearAndInit();
                }
        ));

        // Checkpoint Selection Buttons in a row
        int maxCp = isSoloTab ? soloCheckpoint : (hasParty ? duoCheckpoint : 1);
        int startX = centerX - 165;
        for (int i = 0; i < CHECKPOINTS.length; i++) {
            int cp = CHECKPOINTS[i];
            boolean unlocked = cp <= maxCp;
            boolean isSelected = (cp == selectedCheckpoint);

            String label = (unlocked ? (isSelected ? "§b§l" : "§f") : "§8🔒 ") + "F." + cp;
            ButtonWidget cpBtn = create3DButton(
                    Text.literal(label),
                    startX + (i * 56), centerY - 15, 52, 22,
                    btn -> this.selectedCheckpoint = cp
            );
            cpBtn.active = unlocked;
            this.addDrawableChild(cpBtn);
        }

        // Action Buttons
        if (inTowerSession) {
            // FORFEIT BUTTON
            String forfeitLabel;
            if (isSoloTab || !hasParty) {
                forfeitLabel = "§c§lFORFEIT RUN";
            } else {
                forfeitLabel = "§c§lFORFEIT RUN (" + forfeitVotes + "/2)";
            }

            this.addDrawableChild(create3DButton(
                    Text.literal(forfeitLabel),
                    centerX - 100, centerY + 35, 200, 26,
                    btn -> {
                        ClientPlayNetworking.send(new ForfeitTowerC2SPacket());
                        this.close();
                    }
            ));
        } else {
            // START BUTTON
            if (isSoloTab) {
                this.addDrawableChild(create3DButton(
                        Text.literal("§a§lSTART SOLO RUN (FLOOR " + selectedCheckpoint + ")"),
                        centerX - 100, centerY + 35, 200, 26,
                        btn -> {
                            ClientPlayNetworking.send(new StartTowerC2SPacket(true, selectedCheckpoint));
                            this.close();
                        }
                ));
            } else {
                if (hasParty && isLeader) {
                    this.addDrawableChild(create3DButton(
                            Text.literal("§a§lSTART CO-OP RUN (FLOOR " + selectedCheckpoint + ")"),
                            centerX - 100, centerY + 35, 200, 26,
                            btn -> {
                                ClientPlayNetworking.send(new StartTowerC2SPacket(false, selectedCheckpoint));
                                this.close();
                            }
                    ));
                } else if (hasParty) {
                    this.addDrawableChild(create3DButton(
                            Text.literal("§cLeave Party"),
                            centerX - 100, centerY + 35, 200, 26,
                            btn -> {
                                ClientPlayNetworking.send(new LeavePartyC2SPacket(true));
                                this.close();
                            }
                    ));
                }
            }
        }

        // Close Button
        this.addDrawableChild(create3DButton(
                Text.literal("§fClose"),
                centerX - 50, centerY + 70, 100, 20,
                btn -> this.close()
        ));
    }

    private ButtonWidget create3DButton(Text text, int x, int y, int width, int height, ButtonWidget.PressAction onPress) {
        return ButtonWidget.builder(text, onPress)
                .dimensions(x, y, width, height)
                .build();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Slate & Vibrant Cyan Container
        context.fill(centerX - 175, centerY - 115, centerX + 175, centerY + 120, 0xF012171E);
        context.drawBorder(centerX - 175, centerY - 115, 350, 235, 0xFF0FD9C2);
        context.fill(centerX - 174, centerY - 114, centerX + 174, centerY - 111, 0xFF0FD9C2);

        super.render(context, mouseX, mouseY, delta);

        // Header Title (Drawn after super.render with crisp Cyan glow)
        context.drawCenteredTextWithShadow(this.textRenderer, "§b§l❖ COBBLE TOWER HUB ❖", centerX, centerY - 103, 0x0FD9C2);

        // Mode Descriptions
        if (isSoloTab) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§fDouble Battle with your 6 Pokémon", centerX, centerY - 52, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Highest Floor Cleared: §bFloor " + soloCheckpoint + " §7| Recent: §eFloor " + currentFloor, centerX, centerY - 38, 0xEEEEEE);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, "§fDouble Battle with your 3 Pokémon and your partner's 3 Pokémon", centerX, centerY - 52, 0xFFFFFF);
            if (hasParty) {
                context.drawCenteredTextWithShadow(this.textRenderer, "§7Leader: §e" + leaderName + " §7| Partner: §e" + memberName + " §7| Shared Max: §bFloor " + duoCheckpoint, centerX, centerY - 38, 0xEEEEEE);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, "§cNo active party. §7Shift + Right-Click a player in Overworld to invite!", centerX, centerY - 38, 0xFFAAAA);
            }
        }

        // Starting Floor & Level Cap Indicator
        int maxCap = LevelCapManager.getMaxLevelCapForFloor(selectedCheckpoint);
        context.drawCenteredTextWithShadow(this.textRenderer, "§fStarting Floor: §bFloor " + selectedCheckpoint + " §7(Pokémon ≤ Lv." + maxCap + ")", centerX, centerY + 12, 0x0FD9C2);
        context.drawCenteredTextWithShadow(this.textRenderer, "§8[Clauses: Species Clause | Item Clause | Max 1-2 Legendaries]", centerX, centerY + 24, 0x888888);

        // Authors Credit (Bottom of Menu Y)
        context.drawCenteredTextWithShadow(this.textRenderer, "§7CobbleTower - Made by Vit, Arjun, Serik, Zitj and Nam", centerX, centerY + 104, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
