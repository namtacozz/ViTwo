package com.vitwo.client.gui;

import com.vitwo.battle.LevelCapManager;
import com.vitwo.network.c2s.LeavePartyC2SPacket;
import com.vitwo.network.c2s.RespondInviteC2SPacket;
import com.vitwo.network.c2s.StartTowerC2SPacket;
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

    private boolean isSoloModeTab = true;
    private int selectedCheckpoint = 1;

    private static final int[] CHECKPOINTS = {1, 10, 25, 50, 75, 90};

    public TowerHubScreen() {
        super(Text.literal("CobbleTower Hub"));
        this.isSoloModeTab = !hasParty;
        this.selectedCheckpoint = isSoloModeTab ? soloCheckpoint : duoCheckpoint;
    }

    @Override
    protected void init() {
        this.clearChildren();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Pending Invite Banner Buttons (If any)
        if (!pendingInviterName.isEmpty() && !hasParty) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§aĐồng Ý"),
                    btn -> {
                        ClientPlayNetworking.send(new RespondInviteC2SPacket(true));
                        pendingInviterName = "";
                        init();
                    }
            ).dimensions(centerX + 35, centerY - 95, 60, 18).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§cTừ Chối"),
                    btn -> {
                        ClientPlayNetworking.send(new RespondInviteC2SPacket(false));
                        pendingInviterName = "";
                        init();
                    }
            ).dimensions(centerX + 100, centerY - 95, 60, 18).build());
        }

        // Mode Tabs
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal((isSoloModeTab ? "§a§l▶ " : "§7") + "ĐẤU ĐƠN (SOLO 2-SLOT)"),
                btn -> {
                    this.isSoloModeTab = true;
                    this.selectedCheckpoint = soloCheckpoint;
                    init();
                }
        ).dimensions(centerX - 170, centerY - 70, 165, 22).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal((!isSoloModeTab ? "§a§l▶ " : "§7") + "ĐẤU ĐÔI (DUO CO-OP)"),
                btn -> {
                    this.isSoloModeTab = false;
                    this.selectedCheckpoint = duoCheckpoint;
                    init();
                }
        ).dimensions(centerX + 5, centerY - 70, 165, 22).build());

        // Checkpoint Selection Buttons
        int currentMaxCp = isSoloModeTab ? soloCheckpoint : duoCheckpoint;
        int startX = centerX - 165;
        for (int i = 0; i < CHECKPOINTS.length; i++) {
            int cp = CHECKPOINTS[i];
            boolean unlocked = cp <= currentMaxCp;
            boolean isSelected = (cp == selectedCheckpoint);

            String label = (unlocked ? (isSelected ? "§e§l" : "§a") : "§8🔒 ") + "T." + cp;
            ButtonWidget cpBtn = ButtonWidget.builder(
                    Text.literal(label),
                    btn -> this.selectedCheckpoint = cp
            ).dimensions(startX + (i * 56), centerY - 15, 52, 22).build();

            cpBtn.active = unlocked;
            this.addDrawableChild(cpBtn);
        }

        // Start Tower Button
        boolean canStart = isSoloModeTab || (hasParty && isLeader);
        String startText = isSoloModeTab ? "§a§lBẮT ĐẦU LEO THÁP SOLO (2-SLOT)" :
                (hasParty ? (isLeader ? "§a§lBẮT ĐẦU LEO THÁP ĐÔI (CO-OP)" : "§7ĐANG CHỜ ĐỘI TRƯỞNG...") : "§cCHƯA CÓ ĐỒNG ĐỘI (CẦN 2 NGƯỜI)");

        ButtonWidget startBtn = ButtonWidget.builder(
                Text.literal(startText),
                btn -> {
                    ClientPlayNetworking.send(new StartTowerC2SPacket(isSoloModeTab, selectedCheckpoint));
                    this.close();
                }
        ).dimensions(centerX - 130, centerY + 45, 260, 26).build();
        startBtn.active = canStart && (!inBattle);
        this.addDrawableChild(startBtn);

        // Leave Party Button (if in party)
        if (hasParty) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§cRời Đội"),
                    btn -> {
                        ClientPlayNetworking.send(new LeavePartyC2SPacket(true));
                        this.close();
                    }
            ).dimensions(centerX - 130, centerY + 76, 125, 20).build());
        }

        // Close Button
        int closeX = hasParty ? centerX + 5 : centerX - 50;
        int closeW = hasParty ? 125 : 100;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Đóng"),
                btn -> this.close()
        ).dimensions(closeX, centerY + 76, closeW, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Cobblemon-style dark slate container
        context.fill(centerX - 180, centerY - 105, centerX + 180, centerY + 105, 0xD0121722);
        context.drawBorder(centerX - 180, centerY - 105, 360, 210, 0xFF4B6080);

        // Header Title
        context.drawCenteredTextWithShadow(this.textRenderer, "§6§lCOBBLE TOWER HUB §7(Phím [Y])", centerX, centerY - 100, 0xFFAA00);

        // Pending Invite Banner
        if (!pendingInviterName.isEmpty() && !hasParty) {
            context.fill(centerX - 170, centerY - 96, centerX + 170, centerY - 76, 0xE0281F05);
            context.drawTextWithShadow(this.textRenderer, "§e✉ " + pendingInviterName + " mời bạn leo Tháp!", centerX - 165, centerY - 91, 0xFFEE55);
        }

        int maxCap = LevelCapManager.getMaxLevelCapForFloor(selectedCheckpoint);
        boolean hasShiny = LevelCapManager.hasShinyBossPokemon(selectedCheckpoint);

        // Mode Description
        if (isSoloModeTab) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§eChế Độ Đơn: §fĐấu Đôi 2-Slot (Bạn điều khiển 2 Pokemon ra trận)", centerX, centerY - 42, 0xFFFFFF);
            String shinyStr = hasShiny ? " §d(Boss có 1 Shiny ✨)" : "";
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Giới Hạn Cấp: §aMax Lv." + maxCap + " §7| §cBoss Full 6x31 IVs" + shinyStr, centerX, centerY - 30, 0xAAAAAA);
        } else {
            String partner = hasParty ? (isLeader ? memberName : leaderName) : "Chưa có (Shift + Chuột phải vào bạn bè để mời)";
            context.drawCenteredTextWithShadow(this.textRenderer, "§eĐồng đội: §f" + partner, centerX, centerY - 42, 0xFFFFFF);
            String shinyStr = hasShiny ? " §d(Boss có 1 Shiny ✨)" : "";
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Mỗi người 1 Slot | Giới Hạn Cấp: §aMax Lv." + maxCap + shinyStr, centerX, centerY - 30, 0xAAAAAA);
        }

        // Selected Checkpoint prompt
        context.drawCenteredTextWithShadow(this.textRenderer, "§fMốc xuất phát: §aTầng " + selectedCheckpoint + " §7(Yêu cầu Pokemon ≤ Lv." + maxCap + ")", centerX, centerY + 15, 0x55FF55);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
