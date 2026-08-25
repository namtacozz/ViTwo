package com.vitwo.client.gui;

import com.vitwo.client.gui.widget.TowerButton;
import com.vitwo.network.c2s.RestChoiceC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;

import net.minecraft.text.Text;

public class RestFloorScreen extends AbstractTowerScreen {
    private final int floor;
    private int ticksLeft = 600; // 30 seconds

    public RestFloorScreen(int floor) {
        super(Text.translatable("vitwo.tower.rest_floor.title", floor));
        this.floor = floor;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int cardY = centerY - 50;

        // Card 1 Button: Full Team Rest (Choice 1)
        this.addDrawableChild(TowerButton.towerBuilder(
                Text.literal("§a§lSELECT REST"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(1));
                    this.close();
                }
        ).dimensions(centerX - 175, cardY + 145, 110, 24).build());

        // Card 2 Button: War Preparation (Choice 2)
        this.addDrawableChild(TowerButton.towerBuilder(
                Text.literal("§c§lSELECT PREP"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(2));
                    this.close();
                }
        ).dimensions(centerX - 55, cardY + 145, 110, 24).build());

        // Card 3 Button: Treasure Cache (Choice 3)
        this.addDrawableChild(TowerButton.towerBuilder(
                Text.literal("§6§lSELECT TREASURE"),
                btn -> {
                    ClientPlayNetworking.send(new RestChoiceC2SPacket(3));
                    this.close();
                }
        ).dimensions(centerX + 65, cardY + 145, 110, 24).build());
    }

    @Override
    public void tick() {
        if (ticksLeft > 0) {
            ticksLeft--;
        }
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int containerW = 400;
        int containerH = 240;
        int left = centerX - (containerW / 2);
        int top = centerY - 120;

        // Base Panel
        this.renderPanelBackground(context, left, top, containerW, containerH);

        // Title Header
        context.drawCenteredTextWithShadow(this.textRenderer, "§b§l❖ REST FLOOR STATION (F." + floor + ") ❖", centerX, top + 15, TowerTheme.PRIMARY_CYAN);
        context.drawCenteredTextWithShadow(this.textRenderer, "§aAuto Baseline Restored: +25% Max HP & +50% PP", centerX, top + 30, 0xCCCCCC);
        
        // Timer Bar
        int timerW = 200;
        int timerX = centerX - timerW / 2;
        int timerY = top + 45;
        context.fill(timerX, timerY, timerX + timerW, timerY + 4, 0xFF333333);
        int currentTimerW = (int) ((ticksLeft / 600f) * timerW);
        int timerColor = ticksLeft > 200 ? TowerTheme.PRIMARY_CYAN : TowerTheme.DANGER_RED;
        context.fill(timerX, timerY, timerX + currentTimerW, timerY + 4, timerColor);
        context.drawCenteredTextWithShadow(this.textRenderer, "Choose your boon: [" + (ticksLeft / 20) + "s]", centerX, timerY - 12, 0xFFFFFF);

        int cardY = centerY - 50;

        // Card 1: Full Restoration
        boolean hover1 = mouseX >= centerX - 175 && mouseX <= centerX - 65 && mouseY >= cardY && mouseY <= cardY + 140;
        drawRestCard(context, centerX - 175, cardY, 110, 140, hover1,
                "§a§lFULL RESTORATION",
                "§f* 100% HP & PP Heal",
                "§f* Cure All Statuses",
                "§f* Revive 1 Fainted",
                0xFF55FF55
        );

        // Card 2: War Preparation
        boolean hover2 = mouseX >= centerX - 55 && mouseX <= centerX + 55 && mouseY >= cardY && mouseY <= cardY + 140;
        drawRestCard(context, centerX - 55, cardY, 110, 140, hover2,
                "§c§lWAR PREPARATION",
                "§f* +20% Atk/SpAtk Buff",
                "§f  for next 3 Floors",
                "§f* +20% Def/SpDef Buff",
                0xFFFF5555
        );

        // Card 3: Tower Treasure
        boolean hover3 = mouseX >= centerX + 65 && mouseX <= centerX + 175 && mouseY >= cardY && mouseY <= cardY + 140;
        drawRestCard(context, centerX + 65, cardY, 110, 140, hover3,
                "§6§lTOWER TREASURE",
                "§f* Direct +150 BP",
                "§f* 1 Random Rare",
                "§f  Held Item",
                0xFFFFD700
        );

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawRestCard(DrawContext context, int x, int y, int w, int h, boolean hovered, String title, String l1, String l2, String l3, int accentColor) {
        int bg = hovered ? 0x9020252A : 0x7010151A;
        int border = hovered ? accentColor : TowerTheme.BEVEL_DARK;
        
        // Expand slightly if hovered
        int dx = hovered ? -2 : 0;
        int dy = hovered ? -2 : 0;
        int dw = hovered ? 4 : 0;
        int dh = hovered ? 4 : 0;

        context.fill(x + dx, y + dy, x + w + dw, y + h + dh, bg);
        context.drawBorder(x + dx, y + dy, w + dw, h + dh, border);

        // Inner glowing border
        if (hovered) {
            context.drawBorder(x + dx + 1, y + dy + 1, w + dw - 2, h + dh - 2, (accentColor & 0x00FFFFFF) | 0x55000000);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, title, x + w / 2, y + 10, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, l1, x + 8, y + 35, 0xEEEEEE);
        context.drawTextWithShadow(this.textRenderer, l2, x + 8, y + 55, 0xEEEEEE);
        context.drawTextWithShadow(this.textRenderer, l3, x + 8, y + 75, 0xEEEEEE);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
