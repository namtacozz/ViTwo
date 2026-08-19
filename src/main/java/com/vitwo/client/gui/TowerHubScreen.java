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
    public static int playerBp = 0;
    public static boolean isTrueRun = true;

    private static final int[] CHECKPOINTS = {1, 26, 51, 76};
    private int selectedCheckpoint = 1;
    private boolean isSoloTab = true;

    public TowerHubScreen() {
        super(Text.literal("CobbleTower Hub"));
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
                Text.literal(isSoloTab ? "§b§lSOLO (6v6)" : "§7SOLO (6v6)"),
                centerX - 160, centerY - 80, 155, 22,
                btn -> {
                    this.isSoloTab = true;
                    this.clearAndInit();
                }
        ));

        this.addDrawableChild(create3DButton(
                Text.literal(!isSoloTab ? "§d§lCO-OP DUO (3+3)" : "§7CO-OP DUO (3+3)"),
                centerX + 5, centerY - 80, 155, 22,
                btn -> {
                    this.isSoloTab = false;
                    this.clearAndInit();
                }
        ));

        // Checkpoint Selection Buttons
        int maxCp = isSoloTab ? soloCheckpoint : (hasParty ? duoCheckpoint : 1);
        int startX = centerX - 160;
        for (int i = 0; i < CHECKPOINTS.length; i++) {
            int cp = CHECKPOINTS[i];
            boolean unlocked = cp <= maxCp;
            boolean isSelected = (cp == selectedCheckpoint);

            String tag = (cp == 1) ? " (True Run)" : " (CP)";
            String label = (unlocked ? (isSelected ? "§b§l" : "§f") : "§8🔒 ") + "F." + cp + tag;
            ButtonWidget cpBtn = create3DButton(
                    Text.literal(label),
                    startX + (i * 80), centerY - 15, 76, 22,
                    btn -> this.selectedCheckpoint = cp
            );
            cpBtn.active = unlocked;
            this.addDrawableChild(cpBtn);
        }

        // Action Buttons
        if (inTowerSession) {
            String forfeitLabel = (isSoloTab || !hasParty)
                    ? "§c§lFORFEIT RUN"
                    : "§c§lFORFEIT RUN (" + forfeitVotes + "/2)";

            this.addDrawableChild(create3DButton(
                    Text.literal(forfeitLabel),
                    centerX - 160, centerY + 40, 155, 26,
                    btn -> {
                        ClientPlayNetworking.send(new ForfeitTowerC2SPacket());
                        this.close();
                    }
            ));
        } else {
            if (isSoloTab) {
                String runTitle = (selectedCheckpoint == 1)
                        ? "§a§lSTART TRUE RUN (F.1)"
                        : "§e§lSTART CHECKPOINT (F." + selectedCheckpoint + ")";
                this.addDrawableChild(create3DButton(
                        Text.literal(runTitle),
                        centerX - 160, centerY + 40, 155, 26,
                        btn -> {
                            ClientPlayNetworking.send(new StartTowerC2SPacket(true, selectedCheckpoint));
                            this.close();
                        }
                ));
            } else {
                if (hasParty && isLeader) {
                    String runTitle = (selectedCheckpoint == 1)
                            ? "§a§lSTART CO-OP TRUE RUN"
                            : "§e§lSTART CO-OP (F." + selectedCheckpoint + ")";
                    this.addDrawableChild(create3DButton(
                            Text.literal(runTitle),
                            centerX - 160, centerY + 40, 155, 26,
                            btn -> {
                                ClientPlayNetworking.send(new StartTowerC2SPacket(false, selectedCheckpoint));
                                this.close();
                            }
                    ));
                } else if (hasParty) {
                    this.addDrawableChild(create3DButton(
                            Text.literal("§cLeave Party"),
                            centerX - 160, centerY + 40, 155, 26,
                            btn -> {
                                ClientPlayNetworking.send(new LeavePartyC2SPacket(true));
                                this.close();
                            }
                    ));
                }
            }
        }

        // BP Exchange Shop Button
        this.addDrawableChild(create3DButton(
                Text.literal("§6§l❖ BP EXCHANGE SHOP ❖"),
                centerX + 5, centerY + 40, 155, 26,
                btn -> {
                    TowerBpShopScreen.currentBpBalance = playerBp;
                    this.client.setScreen(new TowerBpShopScreen());
                }
        ));

        // Close Button
        this.addDrawableChild(create3DButton(
                Text.literal("§fClose Hub"),
                centerX - 60, centerY + 78, 120, 20,
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

        // Container Window (Slate & Vibrant Cyan)
        context.fill(centerX - 175, centerY - 118, centerX + 175, centerY + 120, 0xF012171E);
        context.drawBorder(centerX - 175, centerY - 118, 350, 238, 0xFF0FD9C2);
        context.fill(centerX - 174, centerY - 117, centerX + 174, centerY - 114, 0xFF0FD9C2);

        super.render(context, mouseX, mouseY, delta);

        // Header Title
        context.drawCenteredTextWithShadow(this.textRenderer, "§b§l❖ COBBLE TOWER HUB ❖", centerX, centerY - 106, 0x0FD9C2);

        // BP Balance Indicator (Top Right in Header)
        context.drawTextWithShadow(this.textRenderer, "§6BP: §e" + playerBp, centerX + 100, centerY - 106, 0xFFD700);

        // Mode Descriptions
        if (isSoloTab) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§fSolo 6v6 Double Battle Challenge", centerX, centerY - 52, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Max Floor Cleared: §bFloor " + soloCheckpoint + " §7| Current Floor: §eFloor " + currentFloor, centerX, centerY - 38, 0xEEEEEE);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, "§fCo-op Duo 3+3 Merged Double Battle", centerX, centerY - 52, 0xFFFFFF);
            if (hasParty) {
                context.drawCenteredTextWithShadow(this.textRenderer, "§7Leader: §e" + leaderName + " §7| Partner: §e" + memberName + " §7| Shared Max: §bFloor " + duoCheckpoint, centerX, centerY - 38, 0xEEEEEE);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, "§cNo active party. §7Shift + Right-Click a player to invite!", centerX, centerY - 38, 0xFFAAAA);
            }
        }

        // Starting Floor & Level Cap Indicator
        int maxCap = LevelCapManager.getMaxLevelCapForFloor(selectedCheckpoint);
        boolean isTrue = (selectedCheckpoint == 1);
        String runTypeDesc = isTrue
                ? "§a★ True Run: Full BP & Prestige Milestones"
                : "§e⚡ Checkpoint Run: 50% Floor BP, Practice Mode";

        context.drawCenteredTextWithShadow(this.textRenderer, "§fSelected: §bFloor " + selectedCheckpoint + " §7(Cap: Lv." + maxCap + ") §7— " + runTypeDesc, centerX, centerY + 12, 0x0FD9C2);
        context.drawCenteredTextWithShadow(this.textRenderer, "§8[Clauses: Species Clause | Item Clause | Max 1 Restricted Legend | Locked Party]", centerX, centerY + 24, 0x888888);

        // Authors Credit
        context.drawCenteredTextWithShadow(this.textRenderer, "§7CobbleTower - Made by Vit, Arjun, Serik, Zitj and Nam", centerX, centerY + 104, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
