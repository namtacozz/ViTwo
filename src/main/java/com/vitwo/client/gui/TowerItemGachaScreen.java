package com.vitwo.client.gui;

import com.vitwo.client.gui.widget.TowerButton;
import com.vitwo.network.c2s.ClaimItemGachaC2SPacket;
import com.vitwo.reward.GachaItemCandidate;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

public class TowerItemGachaScreen extends AbstractTowerScreen {

    private final int floor;
    private final List<GachaItemCandidate> candidates;
    private final int winningIndex;

    private long startTime = 0;
    private final long DURATION_MS = 3800L;
    private final int CARD_WIDTH = 80;
    private final int CARD_HEIGHT = 90;
    private final int CARD_SPACING = 6;
    private final int CARD_STEP = CARD_WIDTH + CARD_SPACING;

    private int lastTickCard = -1;
    private boolean finished = false;

    public TowerItemGachaScreen(int floor, List<GachaItemCandidate> candidates, int winningIndex) {
        super(Text.literal("CobbleTower Item Gacha"));
        this.floor = floor;
        this.candidates = candidates != null ? candidates : new ArrayList<>();
        this.winningIndex = MathHelper.clamp(winningIndex, 0, Math.max(0, this.candidates.size() - 1));
    }

    @Override
    protected void init() {
        this.clearChildren();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }

        if (finished) {
            this.addDrawableChild(TowerButton.towerBuilder(
                    Text.literal("§a§l✔ NHẬN PHẦN THƯỞNG & TIẾP TỤC"),
                    btn -> claimAndClose()
            ).dimensions(centerX - 110, centerY + 65, 220, 24).style(TowerButton.ButtonStyle.GREEN).build());
        } else {
            this.addDrawableChild(TowerButton.towerBuilder(
                    Text.literal("§7Bỏ qua / Nhận ngay (Skip)"),
                    btn -> {
                        finished = true;
                        claimAndClose();
                    }
                ).dimensions(centerX - 90, centerY + 65, 180, 20).style(TowerButton.ButtonStyle.SECONDARY).build());
        }
    }

    private void claimAndClose() {
        ClientPlayNetworking.send(new ClaimItemGachaC2SPacket(floor, winningIndex));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int panelW = 390;
        int panelH = 190;
        this.renderPanelBackground(context, centerX - panelW / 2, centerY - panelH / 2, panelW, panelH);

        // Header Title
        context.drawCenteredTextWithShadow(this.textRenderer, "§6§l🎁 VÒNG QUAY PHẦN THƯỞNG TẦNG " + floor + " 🎁", centerX, centerY - 80, 0xFFFFD700);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Nhận Vật phẩm thi đấu & Điểm Battle Points (BP)", centerX, centerY - 68, 0xFFCCCCCC);

        // Carousel Calculation
        long elapsed = System.currentTimeMillis() - startTime;
        float progress = MathHelper.clamp((float) elapsed / (float) DURATION_MS, 0.0f, 1.0f);
        float eased = 1.0f - (float) Math.pow(1.0f - progress, 4.0);

        int targetPixelOffset = (winningIndex * CARD_STEP) + (CARD_WIDTH / 2);
        float currentPixelOffset = targetPixelOffset * eased;

        int currentCardPassed = (int) (currentPixelOffset / CARD_STEP);
        if (currentCardPassed != lastTickCard && currentCardPassed <= winningIndex) {
            lastTickCard = currentCardPassed;
            if (this.client != null && this.client.getSoundManager() != null) {
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 0.9f + (progress * 0.3f)));
            }
        }

        if (progress >= 1.0f && !finished) {
            finished = true;
            if (this.client != null && this.client.getSoundManager() != null) {
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f));
            }
            this.init();
        }

        // Viewport
        int viewW = 360;
        int viewH = CARD_HEIGHT + 8;
        int viewX = centerX - (viewW / 2);
        int viewY = centerY - 50;

        context.fill(viewX, viewY, viewX + viewW, viewY + viewH, 0xFF0D0D14);
        context.fill(viewX, viewY, viewX + viewW, viewY + 1, 0xFF353545);
        context.fill(viewX, viewY + viewH - 1, viewX + viewW, viewY + viewH, 0xFF353545);

        context.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);

        for (int i = 0; i < candidates.size(); i++) {
            GachaItemCandidate item = candidates.get(i);
            int cardX = (int) (centerX + (i * CARD_STEP) - currentPixelOffset);
            int cardY = viewY + 4;

            if (cardX + CARD_WIDTH < viewX - 20 || cardX > viewX + viewW + 20) continue;

            boolean isWinnerGlow = (i == winningIndex && finished);
            int border = isWinnerGlow ? 0xFFFFD700 : 0xFF2A2A38;
            int bg = isWinnerGlow ? 0xFF252835 : 0xFF14141E;

            context.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, border);
            context.fill(cardX + 1, cardY + 1, cardX + CARD_WIDTH - 1, cardY + CARD_HEIGHT - 1, bg);

            // CS:GO Top Header Bar
            context.fill(cardX + 1, cardY + 1, cardX + CARD_WIDTH - 1, cardY + 4, item.color());

            // Tier text
            String tierLabel = item.rarityTier() == 3 ? "★ JACKPOT" : (item.rarityTier() == 2 ? "RARE" : (item.rarityTier() == 1 ? "UNCOMMON" : "COMMON"));
            context.drawCenteredTextWithShadow(this.textRenderer, tierLabel, cardX + (CARD_WIDTH / 2), cardY + 7, item.color());

            // Item Icon
            ItemStack icon = getItemIcon(item);
            context.drawItem(icon, cardX + (CARD_WIDTH / 2) - 8, cardY + 20);

            // Item Name
            context.drawCenteredTextWithShadow(this.textRenderer, truncate(item.displayName(), 11), cardX + (CARD_WIDTH / 2), cardY + 42, 0xFFFFFFFF);

            // Category tag
            String cat = "bp".equalsIgnoreCase(item.category()) ? "§e+" + item.bpAmount() + " BP" : "§bVật phẩm";
            context.drawCenteredTextWithShadow(this.textRenderer, cat, cardX + (CARD_WIDTH / 2), cardY + 56, 0xFFCCCCCC);
        }

        context.disableScissor();

        // Center Red Needle Marker
        context.fill(centerX - 1, viewY - 3, centerX + 1, viewY + viewH + 3, 0xFFFF2233);

        if (finished && winningIndex >= 0 && winningIndex < candidates.size()) {
            GachaItemCandidate win = candidates.get(winningIndex);
            context.drawCenteredTextWithShadow(this.textRenderer, "§e★ TRÚNG THƯỞNG: §a" + win.displayName().toUpperCase() + " ★", centerX, centerY + 48, 0xFF55FF55);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private ItemStack getItemIcon(GachaItemCandidate item) {
        if ("bp".equalsIgnoreCase(item.category())) {
            return new ItemStack(Items.EMERALD);
        }
        Identifier id = Identifier.of("cobblemon", item.id());
        if (Registries.ITEM.containsId(id)) {
            return new ItemStack(Registries.ITEM.get(id), Math.max(1, item.quantity()));
        }
        return new ItemStack(Items.CHEST);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}

