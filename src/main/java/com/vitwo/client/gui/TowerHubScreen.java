package com.vitwo.client.gui;

import com.vitwo.battle.LevelCapManager;
import com.vitwo.client.gui.widget.TowerButton;
import com.vitwo.config.TowerLeaderboardManager.LeaderboardEntry;
import com.vitwo.network.c2s.ForfeitTowerC2SPacket;
import com.vitwo.network.c2s.LeavePartyC2SPacket;
import com.vitwo.network.c2s.StartTowerC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TowerHubScreen extends AbstractTowerScreen {
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
    public static int highestFloor = 0;
    public static List<LeaderboardEntry> cachedLeaderboard = new ArrayList<>();

    private static final int[] CHECKPOINTS = {1, 10, 25, 50, 75, 90, 100};
    private int selectedCheckpoint = 1;
    private boolean isSoloTab = true;
    private boolean isLeaderboardView = false;

    public TowerHubScreen() {
        super(Text.literal("CobbleTower Hub"));
    }

    @Override
    protected void init() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.vitwo.network.c2s.RequestHubSyncC2SPacket());
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world != null && mc.world.getRegistryKey().getValue().getPath().contains("tower")) {
            inTowerSession = true;
        }

        if (inTowerSession && inBattle) {
            this.close();
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (hasParty) {
            isSoloTab = false;
        }

        if (isLeaderboardView) {
            // Back button from Leaderboard
            this.addDrawableChild(create3DButton(
                    Text.literal("§fBack to Hub"),
                    centerX - 60, centerY + 95, 120, 22,
                    btn -> {
                        isLeaderboardView = false;
                        this.clearAndInit();
                    }
            ));
            return;
        }

        boolean hasPending = pendingInviterName != null && !pendingInviterName.isBlank();
        int tabY = hasPending ? centerY - 65 : centerY - 80;

        // Pending Invite Accept / Decline banner buttons
        if (hasPending) {
            this.addDrawableChild(create3DButton(
                    Text.literal("§a✔ ACCEPT DUO (" + pendingInviterName + ")"),
                    centerX - 160, centerY - 92, 205, 22,
                    btn -> {
                        ClientPlayNetworking.send(new com.vitwo.network.c2s.RespondInviteC2SPacket(true));
                        pendingInviterName = "";
                        this.isSoloTab = false;
                        this.clearAndInit();
                    }
            ));

            this.addDrawableChild(create3DButton(
                    Text.literal("§c✖ DECLINE"),
                    centerX + 50, centerY - 92, 110, 22,
                    btn -> {
                        ClientPlayNetworking.send(new com.vitwo.network.c2s.RespondInviteC2SPacket(false));
                        pendingInviterName = "";
                        this.clearAndInit();
                    }
            ));
        }

        // Tab Selectors
        this.addDrawableChild(create3DButton(
                Text.literal(isSoloTab ? "§b§lSOLO (6v6)" : "§7SOLO (6v6)"),
                centerX - 160, tabY, 155, 22,
                btn -> {
                    this.isSoloTab = true;
                    this.clearAndInit();
                }
        ));

        this.addDrawableChild(create3DButton(
                Text.literal(!isSoloTab ? "§d§lCO-OP DUO (3+3)" : "§7CO-OP DUO (3+3)"),
                centerX + 5, tabY, 155, 22,
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
            boolean unlocked = cp <= maxCp && !inTowerSession;
            boolean isSelected = (cp == selectedCheckpoint);

            String label = (unlocked ? (isSelected ? "§b§l" : "§f") : (inTowerSession ? "§8" : "§8🔒 ")) + "F." + cp;
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
                this.addDrawableChild(create3DButton(
                        Text.literal("§a§lSTART RUN"),
                        centerX - 160, centerY + 40, 155, 26,
                        btn -> {
                            ClientPlayNetworking.send(new StartTowerC2SPacket(true, selectedCheckpoint));
                            this.close();
                        }
                ));
            } else {
                if (hasParty && isLeader) {
                    this.addDrawableChild(create3DButton(
                            Text.literal("§a§lSTART CO-OP"),
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
                } else {
                    this.addDrawableChild(create3DButton(
                            Text.literal("§d§l+ INVITE PARTNER"),
                            centerX - 160, centerY + 40, 155, 26,
                            btn -> {
                                if (this.client != null && this.client.player != null) {
                                    this.client.player.sendMessage(Text.literal("§e[CobbleTower] §fTo invite a partner, use §b/tower duo <PlayerName> §for §bShift + Right-Click §fa player!"), false);
                                    this.close();
                                }
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

        // Leaderboard Button
        this.addDrawableChild(create3DButton(
                Text.literal("§e🏆 LEADERBOARD"),
                centerX - 140, centerY + 78, 120, 20,
                btn -> {
                    isLeaderboardView = true;
                    this.clearAndInit();
                }
        ));

        // Close Button
        this.addDrawableChild(create3DButton(
                Text.literal("§fClose Hub"),
                centerX + 20, centerY + 78, 120, 20,
                btn -> this.close()
        ));
    }

    private TowerButton create3DButton(Text text, int x, int y, int width, int height, ButtonWidget.PressAction onPress) {
        return TowerButton.towerBuilder(text, onPress)
                .dimensions(x, y, width, height)
                .build();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (isLeaderboardView) {
            renderLeaderboardView(context, centerX, centerY, mouseX, mouseY, delta);
            return;
        }

        // Container Window
        this.renderPanelBackground(context, centerX - 175, centerY - 118, 350, 238);

        super.render(context, mouseX, mouseY, delta);

        // Header Title / Pending Invite Notice
        boolean hasPending = pendingInviterName != null && !pendingInviterName.isBlank();
        if (hasPending) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§6📩 DUO INVITE FROM: §e" + pendingInviterName.toUpperCase(), centerX - 25, centerY - 106, 0xFFFFFF55);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, "§b§l❖ COBBLE TOWER HUB ❖", centerX, centerY - 106, 0xFF0FD9C2);
        }

        // BP Balance Indicator (Top Right in Header)
        context.drawTextWithShadow(this.textRenderer, "§6BP: §e" + playerBp, centerX + 95, centerY - 106, 0xFFFFD700);

        // Mode Descriptions
        int displayMax = Math.max(highestFloor, Math.max(soloCheckpoint, (inTowerSession ? currentFloor : 1)));
        if (isSoloTab) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§fSolo 6v6 Double Battle Challenge", centerX, centerY - 52, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Personal Best: §6Floor " + displayMax + "/100 §7| Current: §eFloor " + (inTowerSession ? currentFloor : 1), centerX, centerY - 38, 0xFFEEEEEE);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, "§fCo-op Duo 3+3 Merged Double Battle", centerX, centerY - 52, 0xFFFFFFFF);
            if (hasParty) {
                context.drawCenteredTextWithShadow(this.textRenderer, "§7Leader: §e" + leaderName + " §7| Partner: §e" + memberName + " §7| Record: §6Floor " + displayMax, centerX, centerY - 38, 0xFFEEEEEE);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, "§cNo active party. §7Shift + Right-Click a player to invite!", centerX, centerY - 38, 0xFFFFAAAA);
            }
        }

        // Starting Floor & Level Cap Indicator
        if (inTowerSession) {
            int floorCap = LevelCapManager.getMaxLevelCapForFloor(currentFloor);
            context.drawCenteredTextWithShadow(this.textRenderer, "§6⚔ §eCurrent Floor: §f" + currentFloor + " §7(Level Cap: §aLv." + floorCap + "§7)", centerX, centerY + 12, 0xFFFFD700);
            context.drawCenteredTextWithShadow(this.textRenderer, "§aTower Session Active: Right-click Trainer NPC to battle!", centerX, centerY + 24, 0xFF55FF55);
        } else {
            int maxCap = LevelCapManager.getMaxLevelCapForFloor(selectedCheckpoint);
            boolean isTrue = (selectedCheckpoint == 1);
            String runTypeDesc = isTrue
                    ? "§a★ True Run: 100% BP"
                    : "§e⚡ Checkpoint: 50% BP";

            context.drawCenteredTextWithShadow(this.textRenderer, "§fStart: §bFloor " + selectedCheckpoint + " §7(Cap: Lv." + maxCap + ") • " + runTypeDesc, centerX, centerY + 12, 0xFF0FD9C2);
            context.drawCenteredTextWithShadow(this.textRenderer, "§8[Gimmicks: Mega / Z-Moves / Dynamax / Tera Active]", centerX, centerY + 24, 0xFF888888);
        }

        // Authors Credit
        context.drawCenteredTextWithShadow(this.textRenderer, "§7CobbleTower — Developed for CobbleVerse Modpack", centerX, centerY + 106, 0xFF888888);
    }

    private void renderLeaderboardView(DrawContext context, int centerX, int centerY, int mouseX, int mouseY, float delta) {
        int panelW = 360;
        int panelH = 240;
        this.renderPanelBackground(context, centerX - panelW / 2, centerY - panelH / 2, panelW, panelH);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, "§6§l🏆 TOWER LEADERBOARD 🏆", centerX, centerY - 105, TowerTheme.SECONDARY_GOLD);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Top 10 Greatest Tower Champions", centerX, centerY - 92, 0xFFCCCCCC);

        int startY = centerY - 75;
        int rowH = 15;

        // Table Header
        context.drawTextWithShadow(this.textRenderer, "§8RANK", centerX - 160, startY, 0xFF888888);
        context.drawTextWithShadow(this.textRenderer, "§8CHALLENGER", centerX - 120, startY, 0xFF888888);
        context.drawTextWithShadow(this.textRenderer, "§8FLOOR", centerX - 10, startY, 0xFF888888);
        context.drawTextWithShadow(this.textRenderer, "§8TIME", centerX + 35, startY, 0xFF888888);
        context.drawTextWithShadow(this.textRenderer, "§8TURNS", centerX + 90, startY, 0xFF888888);
        context.drawTextWithShadow(this.textRenderer, "§8FAINTS", centerX + 135, startY, 0xFF888888);

        context.fill(centerX - 165, startY + 11, centerX + 165, startY + 12, 0xFF333333);

        if (cachedLeaderboard.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§7No records recorded yet. Be the first to conquer the Tower!", centerX, centerY, 0xFF888888);
        } else {
            for (int i = 0; i < Math.min(10, cachedLeaderboard.size()); i++) {
                LeaderboardEntry entry = cachedLeaderboard.get(i);
                int rowY = startY + 16 + (i * rowH);

                String rankStr = switch (entry.rank()) {
                    case 1 -> "§6🥇 1st";
                    case 2 -> "§f🥈 2nd";
                    case 3 -> "§c🥉 3rd";
                    default -> "§7#" + entry.rank();
                };

                int min = entry.durationSeconds() / 60;
                int sec = entry.durationSeconds() % 60;
                String timeStr = String.format("%02d:%02d", min, sec);

                String name = entry.playerNames();
                if (this.textRenderer.getWidth(name) > 100) {
                    name = this.textRenderer.trimToWidth(name, 95) + "…";
                }

                int nameColor = entry.rank() == 1 ? 0xFFFFD700 : 0xFFFFFFFF;
                String floorStr = entry.highestFloor() >= 100 ? "§6F.100★" : "§bF." + entry.highestFloor();

                context.drawTextWithShadow(this.textRenderer, rankStr, centerX - 160, rowY, 0xFFFFFFFF);
                context.drawTextWithShadow(this.textRenderer, (entry.isSolo() ? "§b[S] " : "§d[D] ") + "§f" + name, centerX - 120, rowY, nameColor);
                context.drawTextWithShadow(this.textRenderer, floorStr, centerX - 10, rowY, 0xFF55FFFF);
                context.drawTextWithShadow(this.textRenderer, "§e" + timeStr, centerX + 35, rowY, 0xFFFFFF55);
                context.drawTextWithShadow(this.textRenderer, "§7" + entry.totalTurns(), centerX + 95, rowY, 0xFFCCCCCC);
                context.drawTextWithShadow(this.textRenderer, (entry.faints() == 0 ? "§a0" : "§c" + entry.faints()), centerX + 140, rowY, 0xFFCCCCCC);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
