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

    private static final int[] CHECKPOINTS = {1, 10, 25, 50, 75, 90, 100};
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

        // Checkpoint Selection Buttons (7 buttons: 1, 10, 25, 50, 75, 90, 100)
        int maxCp = isSoloTab ? soloCheckpoint : (hasParty ? duoCheckpoint : 1);
        int cpBtnW = 42;
        int cpGap = 4;
        int totalW = (CHECKPOINTS.length * cpBtnW) + ((CHECKPOINTS.length - 1) * cpGap);
        int startX = centerX - (totalW / 2);

        for (int i = 0; i < CHECKPOINTS.length; i++) {
            int cp = CHECKPOINTS[i];
            boolean unlocked = cp <= maxCp;
            boolean isSelected = (cp == selectedCheckpoint);

            String label = (unlocked ? (isSelected ? "§b§l" : "§f") : "§8🔒 ") + "F." + cp;
            ButtonWidget cpBtn = create3DButton(
                    Text.literal(label),
                    startX + (i * (cpBtnW + cpGap)), centerY - 15, cpBtnW, 22,
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
                        : "§e§lSTART (F." + selectedCheckpoint + ")";
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

        // ==========================================
        // LEFT DEV / CHEAT MENU PANEL (FOR TESTING)
        // ==========================================
        int debugPanelX = centerX - 175 - 146;
        int debugPanelY = centerY - 118;
        int btnW = 130;
        int btnH = 18;

        // 1. Unlock All Checkpoints (F1 - F100)
        this.addDrawableChild(create3DButton(
                Text.literal("§a🔓 Unlock All (F100)"),
                debugPanelX + 5, debugPanelY + 22, btnW, btnH,
                btn -> {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.DebugTowerActionC2SPacket("UNLOCK_ALL", 100));
                    soloCheckpoint = 100;
                    duoCheckpoint = 100;
                    this.clearAndInit();
                }
        ));

        // 2. Add 5,000 Battle Points
        this.addDrawableChild(create3DButton(
                Text.literal("§6💰 +5,000 BP"),
                debugPanelX + 5, debugPanelY + 44, btnW, btnH,
                btn -> {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.DebugTowerActionC2SPacket("ADD_BP", 5000));
                    playerBp += 5000;
                    this.clearAndInit();
                }
        ));

        // 3. Set Max Prestige Level (P.5)
        this.addDrawableChild(create3DButton(
                Text.literal("§b⭐ Max Prestige (P.5)"),
                debugPanelX + 5, debugPanelY + 66, btnW, btnH,
                btn -> {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.DebugTowerActionC2SPacket("SET_PRESTIGE", 5));
                    this.clearAndInit();
                }
        ));

        // 4. Full Party Heal (Instant Revive & Max PP)
        this.addDrawableChild(create3DButton(
                Text.literal("§2❤ Full Party Heal"),
                debugPanelX + 5, debugPanelY + 88, btnW, btnH,
                btn -> {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.DebugTowerActionC2SPacket("HEAL_PARTY", 0));
                }
        ));

        // 5. Toggle Ghost Mode / Extra Ghost Charges
        this.addDrawableChild(create3DButton(
                Text.literal("§d👻 Max Ghost Energy"),
                debugPanelX + 5, debugPanelY + 110, btnW, btnH,
                btn -> {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.DebugTowerActionC2SPacket("MAX_GHOST", 2));
                }
        ));

        // 6. Fast Win Current Battle / Floor Clear
        this.addDrawableChild(create3DButton(
                Text.literal("§c⚔ Auto-Clear Floor"),
                debugPanelX + 5, debugPanelY + 132, btnW, btnH,
                btn -> {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.DebugTowerActionC2SPacket("CLEAR_FLOOR", currentFloor));
                    this.close();
                }
        ));

        // 7. Fast Jump to Boss Floors
        this.addDrawableChild(create3DButton(
                Text.literal("§e⚡ Jump F.90"),
                debugPanelX + 5, debugPanelY + 154, 63, btnH,
                btn -> {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.DebugTowerActionC2SPacket("START_FLOOR", 90));
                    this.close();
                }
        ));

        this.addDrawableChild(create3DButton(
                Text.literal("§4👑 Jump F.100"),
                debugPanelX + 72, debugPanelY + 154, 63, btnH,
                btn -> {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.DebugTowerActionC2SPacket("START_FLOOR", 100));
                    this.close();
                }
        ));

        // 8. Reset All Tower Save Data
        this.addDrawableChild(create3DButton(
                Text.literal("§9🔄 Reset All Data"),
                debugPanelX + 5, debugPanelY + 176, btnW, btnH,
                btn -> {
                    ClientPlayNetworking.send(new com.vitwo.network.c2s.DebugTowerActionC2SPacket("RESET_DATA", 0));
                    soloCheckpoint = 1;
                    duoCheckpoint = 1;
                    playerBp = 0;
                    this.clearAndInit();
                }
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
        context.fill(centerX - 175, centerY - 118, centerX + 175, centerY + 120, 0xF212171E);
        context.drawBorder(centerX - 175, centerY - 118, 350, 238, 0xFF0FD9C2);
        context.fill(centerX - 174, centerY - 117, centerX + 174, centerY - 114, 0xFF0FD9C2);

        // ==========================================
        // LEFT DEV / CHEAT MENU CONTAINER RENDER
        // ==========================================
        int debugPanelX = centerX - 175 - 146;
        int debugPanelY = centerY - 118;
        int debugPanelWidth = 140;
        int debugPanelHeight = 238;

        context.fill(debugPanelX, debugPanelY, debugPanelX + debugPanelWidth, debugPanelY + debugPanelHeight, 0xF212171E);
        context.drawBorder(debugPanelX, debugPanelY, debugPanelWidth, debugPanelHeight, 0xFFE5A50A);
        context.fill(debugPanelX + 1, debugPanelY + 1, debugPanelX + debugPanelWidth - 1, debugPanelY + 3, 0xFFE5A50A);

        context.drawCenteredTextWithShadow(this.textRenderer, "§e§l🛠 DEV CHEAT MENU", debugPanelX + (debugPanelWidth / 2), debugPanelY + 8, 0xFFE5A50A);
        context.drawCenteredTextWithShadow(this.textRenderer, "§8[Direct Mod Testing]", debugPanelX + (debugPanelWidth / 2), debugPanelY + 200, 0x888888);

        super.render(context, mouseX, mouseY, delta);

        // Header Title
        context.drawCenteredTextWithShadow(this.textRenderer, "§b§l❖ COBBLE TOWER HUB ❖", centerX, centerY - 106, 0x0FD9C2);

        // BP Balance Indicator (Top Right in Header)
        context.drawTextWithShadow(this.textRenderer, "§6BP: §e" + playerBp, centerX + 95, centerY - 106, 0xFFD700);

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
                ? "§a★ True Run: Full BP & Milestones"
                : "§e⚡ Checkpoint Run: 50% Floor BP";

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
