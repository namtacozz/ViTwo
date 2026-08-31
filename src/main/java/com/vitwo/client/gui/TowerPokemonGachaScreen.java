package com.vitwo.client.gui;

import com.vitwo.client.gui.widget.TowerButton;
import com.vitwo.network.c2s.ClaimGachaPokemonC2SPacket;
import com.vitwo.reward.GachaPokemonCandidate;
import com.vitwo.reward.PokemonRarity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class TowerPokemonGachaScreen extends AbstractTowerScreen {

    public enum GachaStage {
        STAGE_1_ROULETTE,
        STAGE_2_SHINY_SPIN,
        STAGE_3_IV_MATRIX,
        STAGE_4_SUMMARY
    }

    private final int floor;
    private final String bossName;
    private final List<GachaPokemonCandidate> candidates;
    private final int winningIndex;
    private final boolean isShinyWinner;
    private final int[] targetIvs;

    private GachaStage currentStage = GachaStage.STAGE_1_ROULETTE;

    // --- STAGE 1: CS:GO ROULETTE STATE ---
    private long stage1StartTime = 0;
    private final long STAGE1_DURATION_MS = 5500L;
    private final int CARD_WIDTH = 92;
    private final int CARD_HEIGHT = 110;
    private final int CARD_SPACING = 6;
    private final int CARD_STEP = CARD_WIDTH + CARD_SPACING;
    private int lastTickCardIndex = -1;
    private boolean stage1Finished = false;

    // --- STAGE 2: SHINY WHEEL STATE ---
    private long stage2StartTime = 0;
    private final long STAGE2_DURATION_MS = 3200L;
    private boolean stage2Spinning = false;
    private boolean stage2Finished = false;
    private int lastShinyTick = -1;

    // --- STAGE 3: 6x IV VERTICAL WHEELS STATE ---
    private final int[] currentIvDisplay = new int[6];
    private final boolean[] ivSpinning = new boolean[6];
    private final boolean[] ivFinished = new boolean[6];
    private final long[] ivStartTimes = new long[6];
    private final long IV_SPIN_DURATION_MS = 2500L;
    private static final String[] STAT_NAMES = {"HP", "TẤN CÔNG (ATK)", "PHÒNG THỦ (DEF)", "SP. ATK", "SP. DEF", "TỐC ĐỘ (SPE)"};
    private static final int[] STAT_COLORS = {0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FFFF, 0xFF55FF55, 0xFFFF55FF};

    public TowerPokemonGachaScreen(
            int floor,
            String bossName,
            List<GachaPokemonCandidate> candidates,
            int winningIndex,
            boolean isShinyWinner,
            int[] rolledIvs
    ) {
        super(Text.literal("CobbleTower Pokemon Gacha"));
        this.floor = floor;
        this.bossName = bossName;
        this.candidates = candidates != null ? candidates : new ArrayList<>();
        this.winningIndex = MathHelper.clamp(winningIndex, 0, Math.max(0, this.candidates.size() - 1));
        this.isShinyWinner = isShinyWinner;
        this.targetIvs = rolledIvs != null && rolledIvs.length == 6 ? rolledIvs : new int[]{31, 31, 31, 31, 31, 31};

        for (int i = 0; i < 6; i++) {
            currentIvDisplay[i] = 0;
            ivSpinning[i] = false;
            ivFinished[i] = false;
        }
    }

    @Override
    protected void init() {
        this.clearChildren();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (stage1StartTime == 0) {
            stage1StartTime = System.currentTimeMillis();
        }

        if (currentStage == GachaStage.STAGE_1_ROULETTE) {
            if (stage1Finished) {
                this.addDrawableChild(TowerButton.towerBuilder(
                        Text.literal("§e§lQUAY TIẾP: VÒNG SHINY (1%) ►"),
                        btn -> advanceToStage2()
                ).dimensions(centerX - 110, centerY + 85, 220, 22).style(TowerButton.ButtonStyle.GOLD).build());
            } else {
                // Skip Button
                this.addDrawableChild(TowerButton.towerBuilder(
                        Text.literal("§7Bỏ qua hoạt ảnh (Skip)"),
                        btn -> {
                            stage1Finished = true;
                            advanceToStage2();
                        }
                ).dimensions(centerX - 70, centerY + 85, 140, 18).style(TowerButton.ButtonStyle.SECONDARY).build());
            }
        } else if (currentStage == GachaStage.STAGE_2_SHINY_SPIN) {
            if (!stage2Spinning && !stage2Finished) {
                this.addDrawableChild(TowerButton.towerBuilder(
                        Text.literal("§6§l✨ QUAY THỬ VẬN MAY SHINY (1%)"),
                        btn -> {
                            stage2Spinning = true;
                            stage2StartTime = System.currentTimeMillis();
                            this.clearChildren();
                        }
                ).dimensions(centerX - 120, centerY + 70, 240, 24).style(TowerButton.ButtonStyle.GOLD).build());
            } else if (stage2Finished) {
                this.addDrawableChild(TowerButton.towerBuilder(
                        Text.literal("§b§lQUAY BẢNG 6 DÒNG CHỈ SỐ IVS ►"),
                        btn -> advanceToStage3()
                ).dimensions(centerX - 120, centerY + 75, 240, 22).style(TowerButton.ButtonStyle.DEFAULT).build());
            }
        } else if (currentStage == GachaStage.STAGE_3_IV_MATRIX) {
            // Spin All Button
            boolean allFinished = true;
            for (boolean f : ivFinished) {
                if (!f) { allFinished = false; break; }
            }

            if (!allFinished) {
                this.addDrawableChild(TowerButton.towerBuilder(
                        Text.literal("§6§l🎲 QUAY TẤT CẢ 6 DÒNG (SPIN ALL)"),
                        btn -> spinAllIvs()
                ).dimensions(centerX - 120, centerY - 80, 240, 20).style(TowerButton.ButtonStyle.GOLD).build());

                // Individual spin buttons for each of the 6 stats
                int startY = centerY - 50;
                int rowH = 22;
                for (int i = 0; i < 6; i++) {
                    final int statIdx = i;
                    if (!ivFinished[i] && !ivSpinning[i]) {
                        this.addDrawableChild(TowerButton.towerBuilder(
                                Text.literal("§eQuay"),
                                btn -> spinSingleIv(statIdx)
                        ).dimensions(centerX + 115, startY + i * rowH, 50, 18).style(TowerButton.ButtonStyle.DEFAULT).build());
                    }
                }
            } else {
                this.addDrawableChild(TowerButton.towerBuilder(
                        Text.literal("§a§lXÁC NHẬN KẾT QUẢ IVS & TIẾP TỤC ►"),
                        btn -> advanceToStage4()
                ).dimensions(centerX - 120, centerY + 85, 240, 22).style(TowerButton.ButtonStyle.GREEN).build());
            }
        } else if (currentStage == GachaStage.STAGE_4_SUMMARY) {
            this.addDrawableChild(TowerButton.towerBuilder(
                    Text.literal("§a§l✔ NHẬN POKÉMON VÀO ĐỘI HÌNH / PC"),
                    btn -> claimPokemonAndClose()
            ).dimensions(centerX - 130, centerY + 80, 260, 26).style(TowerButton.ButtonStyle.GREEN).build());
        }
    }

    private void advanceToStage2() {
        this.currentStage = GachaStage.STAGE_2_SHINY_SPIN;
        this.stage2StartTime = 0;
        this.stage2Spinning = false;
        this.stage2Finished = false;
        this.init();
    }

    private void advanceToStage3() {
        this.currentStage = GachaStage.STAGE_3_IV_MATRIX;
        this.init();
    }

    private void advanceToStage4() {
        this.currentStage = GachaStage.STAGE_4_SUMMARY;
        this.init();
    }

    private void spinSingleIv(int statIdx) {
        if (statIdx >= 0 && statIdx < 6) {
            ivSpinning[statIdx] = true;
            ivStartTimes[statIdx] = System.currentTimeMillis() + (statIdx * 100L);
            this.init();
        }
    }

    private void spinAllIvs() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) {
            if (!ivFinished[i]) {
                ivSpinning[i] = true;
                ivStartTimes[i] = now + (i * 200L);
            }
        }
        this.init();
    }

    private void claimPokemonAndClose() {
        ClientPlayNetworking.send(new ClaimGachaPokemonC2SPacket(floor, winningIndex, isShinyWinner, targetIvs));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int panelW = 420;
        int panelH = 260;
        this.renderPanelBackground(context, centerX - panelW / 2, centerY - panelH / 2, panelW, panelH);

        // Header Title
        context.drawCenteredTextWithShadow(this.textRenderer, "§6§l🎰 VÒNG QUAY POKÉMON CS:GO (TẦNG " + floor + ") 🎰", centerX, centerY - 120, 0xFFFFD700);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Đối đầu: §e" + bossName + " §7• Tích luỹ Pokémon các tầng trước", centerX, centerY - 108, 0xFFCCCCCC);

        switch (currentStage) {
            case STAGE_1_ROULETTE -> renderStage1Roulette(context, mouseX, mouseY, delta, centerX, centerY);
            case STAGE_2_SHINY_SPIN -> renderStage2Shiny(context, mouseX, mouseY, delta, centerX, centerY);
            case STAGE_3_IV_MATRIX -> renderStage3Ivs(context, mouseX, mouseY, delta, centerX, centerY);
            case STAGE_4_SUMMARY -> renderStage4Summary(context, mouseX, mouseY, delta, centerX, centerY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    // =========================================================================
    // STAGE 1: CS:GO HORIZONTAL ROULETTE CAROUSEL
    // =========================================================================
    private void renderStage1Roulette(DrawContext context, int mouseX, int mouseY, float delta, int centerX, int centerY) {
        long elapsed = System.currentTimeMillis() - stage1StartTime;
        float progress = MathHelper.clamp((float) elapsed / (float) STAGE1_DURATION_MS, 0.0f, 1.0f);

        // Deceleration Easing (Ease-Out Quart)
        float eased = 1.0f - (float) Math.pow(1.0f - progress, 4.0);

        int targetPixelOffset = (winningIndex * CARD_STEP) + (CARD_WIDTH / 2);
        float currentPixelOffset = targetPixelOffset * eased;

        // Sound Ticker on each card passage
        int currentCardPassed = (int) (currentPixelOffset / CARD_STEP);
        if (currentCardPassed != lastTickCardIndex && currentCardPassed <= winningIndex) {
            lastTickCardIndex = currentCardPassed;
            if (this.client != null && this.client.getSoundManager() != null) {
                float pitch = 0.8f + (progress * 0.4f);
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, pitch));
            }
        }

        if (progress >= 1.0f && !stage1Finished) {
            stage1Finished = true;
            if (this.client != null && this.client.getSoundManager() != null) {
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
            }
            this.init();
        }

        // Viewport boundaries for the horizontal track
        int viewW = 390;
        int viewH = CARD_HEIGHT + 10;
        int viewX = centerX - (viewW / 2);
        int viewY = centerY - 55;

        // Clip / Viewport Dark Background
        context.fill(viewX, viewY, viewX + viewW, viewY + viewH, 0xFF0D0D14);
        context.fill(viewX, viewY, viewX + viewW, viewY + 1, 0xFF353545);
        context.fill(viewX, viewY + viewH - 1, viewX + viewW, viewY + viewH, 0xFF353545);

        context.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);

        // Render visible cards
        for (int i = 0; i < candidates.size(); i++) {
            GachaPokemonCandidate c = candidates.get(i);
            int cardX = (int) (centerX + (i * CARD_STEP) - currentPixelOffset);
            int cardY = viewY + 5;

            if (cardX + CARD_WIDTH < viewX - 20 || cardX > viewX + viewW + 20) {
                continue; // Cull off-screen cards
            }

            renderGachaCard(context, cardX, cardY, CARD_WIDTH, CARD_HEIGHT, c, i == winningIndex && stage1Finished);
        }

        context.disableScissor();

        // Center Red Needle Marker
        int needleX = centerX;
        context.fill(needleX - 1, viewY - 4, needleX + 2, viewY + viewH + 4, 0xFFFF2233);
        context.fill(needleX - 3, viewY - 6, needleX + 4, viewY - 2, 0xFFFF2233);
        context.fill(needleX - 3, viewY + viewH + 2, needleX + 4, viewY + viewH + 6, 0xFFFF2233);

        // Winner Announcement if finished
        if (stage1Finished && winningIndex >= 0 && winningIndex < candidates.size()) {
            GachaPokemonCandidate winner = candidates.get(winningIndex);
            String title = "§e★ TRÚNG THƯỞNG: §a" + winner.displayName().toUpperCase() + " §7(" + winner.rarity().getDisplayName() + ") ★";
            context.drawCenteredTextWithShadow(this.textRenderer, title, centerX, viewY + viewH + 12, 0xFFFFFF55);
        }
    }

    private void renderGachaCard(DrawContext context, int x, int y, int w, int h, GachaPokemonCandidate c, boolean isWinnerGlow) {
        PokemonRarity rarity = c.rarity() != null ? c.rarity() : PokemonRarity.COMMON;

        int borderColor = isWinnerGlow ? 0xFFFFD700 : 0xFF2A2A38;
        int bgColor = isWinnerGlow ? 0xFF202636 : 0xFF14141E;

        // Card Frame
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, borderColor);
        context.fill(x, y, x + w, y + h, bgColor);

        // Top CS:GO Rarity Header Bar
        context.fill(x, y, x + w, y + 4, rarity.getHeaderColor());

        // Rarity Tag text
        String rarityLabel = truncate(rarity.getDisplayName(), 12);
        context.drawCenteredTextWithShadow(this.textRenderer, rarityLabel, x + (w / 2), y + 7, rarity.getGlowColor());

        // Icon
        ItemStack iconStack;
        if (c.isLegendary()) {
            Identifier mbId = Identifier.of("cobblemon", "master_ball");
            iconStack = Registries.ITEM.containsId(mbId) ? new ItemStack(Registries.ITEM.get(mbId)) : new ItemStack(Items.NETHER_STAR);
        } else {
            Identifier eggId = Identifier.of("cobblemon", "pokemon_egg");
            iconStack = Registries.ITEM.containsId(eggId) ? new ItemStack(Registries.ITEM.get(eggId)) : new ItemStack(Items.EGG);
        }
        context.drawItem(iconStack, x + (w / 2) - 8, y + 22);

        // Species Name
        String name = truncate(c.displayName(), 11);
        context.drawCenteredTextWithShadow(this.textRenderer, name, x + (w / 2), y + 42, 0xFFFFFFFF);

        // Base Species
        String base = "↳ " + truncate(c.baseSpecies(), 10);
        context.drawCenteredTextWithShadow(this.textRenderer, base, x + (w / 2), y + 54, 0xFFAAAAAA);

        // Types
        String types = c.primaryType() + (c.secondaryType().isEmpty() ? "" : ("/" + c.secondaryType()));
        context.drawCenteredTextWithShadow(this.textRenderer, truncate(types, 12), x + (w / 2), y + 68, 0xFF88CCFF);

        // Form tag if any
        if (c.formAspect() != null && !c.formAspect().isBlank()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§6[" + capitalize(c.formAspect()) + "]", x + (w / 2), y + 80, 0xFFFFAA00);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, "§8Chuẩn gen", x + (w / 2), y + 80, 0xFF666666);
        }

        if (c.isShiny()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§6✨ SHINY", x + (w / 2), y + 94, 0xFFFFD700);
        }
    }

    // =========================================================================
    // STAGE 2: SHINY WHEEL (1% CHANCE)
    // =========================================================================
    private void renderStage2Shiny(DrawContext context, int mouseX, int mouseY, float delta, int centerX, int centerY) {
        GachaPokemonCandidate winner = (winningIndex >= 0 && winningIndex < candidates.size()) ? candidates.get(winningIndex) : null;
        if (winner != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§fLoài Pokémon đã quay: §e§l" + winner.displayName().toUpperCase(), centerX, centerY - 85, 0xFFFFFF55);
        }

        if (!stage2Spinning && !stage2Finished) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§6★ BƯỚC 2: QUAY TỈ LỆ SHINY (1% CƠ HỘI) ★", centerX, centerY - 60, 0xFFFFD700);
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Bấm nút bên dưới để khởi động vòng quay Shiny độc quyền!", centerX, centerY - 45, 0xFFCCCCCC);

            // Preview Box
            context.fill(centerX - 100, centerY - 30, centerX + 100, centerY + 50, 0xFF14141E);
            context.fill(centerX - 100, centerY - 30, centerX + 100, centerY - 28, 0xFFFFD700);
            context.drawCenteredTextWithShadow(this.textRenderer, "§6✨ SHINY POKÉMON ✨", centerX, centerY - 15, 0xFFFFD700);
            context.drawCenteredTextWithShadow(this.textRenderer, "§7Tỉ lệ xuất hiện: §a1.00%", centerX, centerY + 2, 0xFF55FF55);
            context.drawCenteredTextWithShadow(this.textRenderer, "§8(Lấp lánh hiệu ứng ánh kim)", centerX, centerY + 18, 0xFF888888);
            return;
        }

        long elapsed = System.currentTimeMillis() - stage2StartTime;
        float progress = MathHelper.clamp((float) elapsed / (float) STAGE2_DURATION_MS, 0.0f, 1.0f);
        float eased = 1.0f - (float) Math.pow(1.0f - progress, 3.0);

        int totalShinyCards = 100;
        int targetSlot = isShinyWinner ? 80 : 81; // Slot 80 is the Golden Shiny slot
        int slotWidth = 70;
        float currentOffset = (targetSlot * slotWidth) * eased;

        int passedSlot = (int) (currentOffset / slotWidth);
        if (passedSlot != lastShinyTick && passedSlot <= targetSlot) {
            lastShinyTick = passedSlot;
            if (this.client != null && this.client.getSoundManager() != null) {
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_HAT, 1.2f));
            }
        }

        if (progress >= 1.0f && !stage2Finished) {
            stage2Finished = true;
            if (this.client != null && this.client.getSoundManager() != null) {
                if (isShinyWinner) {
                    this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f));
                } else {
                    this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f));
                }
            }
            this.init();
        }

        // Viewport
        int viewW = 340;
        int viewH = 65;
        int viewX = centerX - (viewW / 2);
        int viewY = centerY - 25;

        context.fill(viewX, viewY, viewX + viewW, viewY + viewH, 0xFF0D0D14);
        context.fill(viewX, viewY, viewX + viewW, viewY + 1, 0xFF353545);
        context.fill(viewX, viewY + viewH - 1, viewX + viewW, viewY + viewH, 0xFF353545);

        context.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);

        for (int i = 0; i < totalShinyCards; i++) {
            int cardX = (int) (centerX + (i * slotWidth) - currentOffset);
            int cardY = viewY + 5;

            if (cardX + slotWidth < viewX - 20 || cardX > viewX + viewW + 20) continue;

            boolean isGolden = (i == 80); // Slot 80 is the 1% Golden Shiny card
            int border = isGolden ? 0xFFFFD700 : 0xFF2A2A38;
            int bg = isGolden ? 0xFF3A2A10 : 0xFF14141E;

            context.fill(cardX, cardY, cardX + slotWidth - 4, cardY + 55, border);
            context.fill(cardX + 1, cardY + 1, cardX + slotWidth - 5, cardY + 54, bg);

            if (isGolden) {
                context.drawCenteredTextWithShadow(this.textRenderer, "§6§l✨ SHINY", cardX + (slotWidth / 2) - 2, cardY + 15, 0xFFFFD700);
                context.drawCenteredTextWithShadow(this.textRenderer, "§e1.00%", cardX + (slotWidth / 2) - 2, cardY + 30, 0xFFFFFF55);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, "§7NORMAL", cardX + (slotWidth / 2) - 2, cardY + 15, 0xFFAAAAAA);
                context.drawCenteredTextWithShadow(this.textRenderer, "§8Thường", cardX + (slotWidth / 2) - 2, cardY + 30, 0xFF666666);
            }
        }

        context.disableScissor();

        // Center Marker
        context.fill(centerX - 1, viewY - 3, centerX + 1, viewY + viewH + 3, 0xFFFF2233);

        if (stage2Finished) {
            if (isShinyWinner) {
                context.drawCenteredTextWithShadow(this.textRenderer, "§6§l★★★ TUYỆT VỜI! BẠN ĐÃ TRÚNG POKÉMON SHINY (1%)! ★★★", centerX, viewY + viewH + 12, 0xFFFFD700);
            } else {
                context.drawCenteredTextWithShadow(this.textRenderer, "§7Kết quả: Pokémon dạng Thường (Standard Form)", centerX, viewY + viewH + 12, 0xFFCCCCCC);
            }
        }
    }

    // =========================================================================
    // STAGE 3: 6x VERTICAL IV WHEELS MATRIX
    // =========================================================================
    private void renderStage3Ivs(DrawContext context, int mouseX, int mouseY, float delta, int centerX, int centerY) {
        context.drawCenteredTextWithShadow(this.textRenderer, "§6★ BƯỚC 3: QUAY BẢNG 6 DÒNG CHỈ SỐ IVS (0 - 31) ★", centerX, centerY - 100, 0xFFFFD700);

        long now = System.currentTimeMillis();
        int startY = centerY - 50;
        int rowH = 22;

        for (int i = 0; i < 6; i++) {
            int rowY = startY + (i * rowH);

            if (ivSpinning[i] && !ivFinished[i]) {
                long elapsed = now - ivStartTimes[i];
                if (elapsed > 0) {
                    float prog = MathHelper.clamp((float) elapsed / (float) IV_SPIN_DURATION_MS, 0.0f, 1.0f);
                    float eased = 1.0f - (float) Math.pow(1.0f - prog, 3.0);

                    // Rapid rolling number
                    if (prog < 1.0f) {
                        currentIvDisplay[i] = (int) ((now / 40) % 32);
                    } else {
                        currentIvDisplay[i] = targetIvs[i];
                        ivFinished[i] = true;
                        ivSpinning[i] = false;
                        if (this.client != null && this.client.getSoundManager() != null) {
                            if (targetIvs[i] == 31) {
                                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.2f));
                            } else {
                                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f));
                            }
                        }
                        this.init();
                    }
                }
            }

            // Stat Row Frame
            int rowW = 280;
            int rowX = centerX - 140;

            int val = currentIvDisplay[i];
            boolean isBest = ivFinished[i] && val == 31;
            int borderCol = isBest ? 0xFFFFD700 : 0xFF2A2A38;
            int bgCol = isBest ? 0xFF2B2810 : 0xFF14141E;

            context.fill(rowX, rowY, rowX + rowW, rowY + rowH - 4, borderCol);
            context.fill(rowX + 1, rowY + 1, rowX + rowW - 1, rowY + rowH - 5, bgCol);

            // Stat Name
            context.drawTextWithShadow(this.textRenderer, STAT_NAMES[i], rowX + 8, rowY + 4, STAT_COLORS[i]);

            // Stat Value / Roller
            String valStr;
            int valColor;
            if (!ivFinished[i] && !ivSpinning[i]) {
                valStr = "§8[Chưa quay]";
                valColor = 0xFF888888;
            } else if (ivSpinning[i]) {
                valStr = "§e" + val + " ↻";
                valColor = 0xFFFFFF55;
            } else {
                if (val == 31) {
                    valStr = "§6§l★ 31 (BEST!)";
                    valColor = 0xFFFFD700;
                } else if (val >= 25) {
                    valStr = "§a" + val + " (Fantastic)";
                    valColor = 0xFF55FF55;
                } else if (val >= 15) {
                    valStr = "§b" + val + " (Very Good)";
                    valColor = 0xFF55FFFF;
                } else {
                    valStr = "§f" + val + " (Decent)";
                    valColor = 0xFFCCCCCC;
                }
            }

            context.drawTextWithShadow(this.textRenderer, valStr, rowX + 160, rowY + 4, valColor);
        }
    }

    // =========================================================================
    // STAGE 4: SUMMARY & CLAIM POKEMON
    // =========================================================================
    private void renderStage4Summary(DrawContext context, int mouseX, int mouseY, float delta, int centerX, int centerY) {
        GachaPokemonCandidate winner = (winningIndex >= 0 && winningIndex < candidates.size()) ? candidates.get(winningIndex) : null;
        if (winner == null) return;

        context.drawCenteredTextWithShadow(this.textRenderer, "§6§l🏆 TỔNG KẾT PHẦN THƯỞNG GACHA POKÉMON 🏆", centerX, centerY - 105, 0xFFFFD700);

        int cardW = 320;
        int cardH = 160;
        int cardX = centerX - (cardW / 2);
        int cardY = centerY - 88;

        context.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xFFFFD700);
        context.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + cardH - 1, 0xFF14141E);

        // Header Title
        String formTag = (winner.formAspect() != null && !winner.formAspect().isBlank()) ? (" [" + capitalize(winner.formAspect()) + "]") : "";
        String shinyTag = isShinyWinner ? " §6✨ [SHINY]" : "";
        context.drawCenteredTextWithShadow(this.textRenderer, "§e§lPOKÉMON: §a" + winner.baseSpecies().toUpperCase() + formTag + shinyTag, centerX, cardY + 10, 0xFF55FF55);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Dạng sơ cấp từ: §f" + winner.displayName() + " §7(Cấp độ: §eLv.1 Sơ cấp§7)", centerX, cardY + 24, 0xFFCCCCCC);

        // Stats Summary (2 Columns x 3 Rows)
        int col1X = cardX + 25;
        int col2X = cardX + 175;
        int startStatY = cardY + 44;

        for (int i = 0; i < 6; i++) {
            int sx = (i < 3) ? col1X : col2X;
            int sy = startStatY + ((i % 3) * 20);

            int iv = targetIvs[i];
            String tag = (iv == 31) ? " §6★ 31 (BEST)" : (" §f" + iv);
            context.drawTextWithShadow(this.textRenderer, STAT_NAMES[i] + ": " + tag, sx, sy, STAT_COLORS[i]);
        }

        // Bottom Guarantee Note
        context.drawCenteredTextWithShadow(this.textRenderer, "§b✔ Tự động đưa vào Party (hoặc PC Box nếu đầy) • Đầy đủ chiêu thức & 100% PP", centerX, cardY + 135, 0xFF88CCFF);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

