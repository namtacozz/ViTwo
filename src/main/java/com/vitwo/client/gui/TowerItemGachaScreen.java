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
    private final int CARD_WIDTH = 86;
    private final int CARD_HEIGHT = 96;
    private final int CARD_SPACING = 8;
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
                    Text.literal("§a§l✔ CLAIM REWARD & CONTINUE"),
                    btn -> claimAndClose()
            ).dimensions(centerX - 110, centerY + 68, 220, 24).style(TowerButton.ButtonStyle.GREEN).build());
        } else {
            this.addDrawableChild(TowerButton.towerBuilder(
                    Text.literal("§7Skip / Claim Immediately"),
                    btn -> {
                        finished = true;
                        claimAndClose();
                    }
                ).dimensions(centerX - 90, centerY + 68, 180, 20).style(TowerButton.ButtonStyle.SECONDARY).build());
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

        int panelW = 410;
        int panelH = 205;
        this.renderPanelBackground(context, centerX - panelW / 2, centerY - panelH / 2, panelW, panelH);

        // Header Title
        context.drawCenteredTextWithShadow(this.textRenderer, "§6§l🎁 FLOOR " + floor + " REWARD ROULETTE 🎁", centerX, centerY - 88, 0xFFFFD700);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Competitive Items & Battle Points (BP) Rewards", centerX, centerY - 76, 0xFFCCCCCC);

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
        int viewW = 380;
        int viewH = CARD_HEIGHT + 8;
        int viewX = centerX - (viewW / 2);
        int viewY = centerY - 55;

        context.fill(viewX, viewY, viewX + viewW, viewY + viewH, 0xFF0B0C12);
        context.fill(viewX, viewY, viewX + viewW, viewY + 1, 0xFF353548);
        context.fill(viewX, viewY + viewH - 1, viewX + viewW, viewY + viewH, 0xFF353548);

        context.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);

        for (int i = 0; i < candidates.size(); i++) {
            GachaItemCandidate item = candidates.get(i);
            int cardX = (int) (centerX + (i * CARD_STEP) - currentPixelOffset);
            int cardY = viewY + 4;

            if (cardX + CARD_WIDTH < viewX - 30 || cardX > viewX + viewW + 30) continue;

            boolean isWinnerGlow = (i == winningIndex && finished);
            int border = isWinnerGlow ? 0xFFFFD700 : 0xFF2D3142;
            int bg = isWinnerGlow ? 0xFF262A38 : 0xFF141620;

            // Card body & border
            context.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, border);
            context.fill(cardX + 1, cardY + 1, cardX + CARD_WIDTH - 1, cardY + CARD_HEIGHT - 1, bg);

            // CS:GO Top Header Bar
            context.fill(cardX + 1, cardY + 1, cardX + CARD_WIDTH - 1, cardY + 4, item.color());

            // Tier text
            String tierLabel = item.rarityTier() == 3 ? "★ JACKPOT" : (item.rarityTier() == 2 ? "RARE" : (item.rarityTier() == 1 ? "UNCOMMON" : "COMMON"));
            context.drawCenteredTextWithShadow(this.textRenderer, tierLabel, cardX + (CARD_WIDTH / 2), cardY + 7, item.color());

            // Dedicated Item Icon Socket
            context.fill(cardX + (CARD_WIDTH / 2) - 14, cardY + 18, cardX + (CARD_WIDTH / 2) + 14, cardY + 46, 0xFF0D0E16);
            context.drawBorder(cardX + (CARD_WIDTH / 2) - 14, cardY + 18, 28, 28, 0xFF252838);

            // Item Icon
            ItemStack icon = getItemIcon(item);
            context.drawItem(icon, cardX + (CARD_WIDTH / 2) - 8, cardY + 24);

            // Item Name & Quantity
            String mainTitle = getItemMainTitle(item);
            String subTitle = getItemSubTitle(item);
            context.drawCenteredTextWithShadow(this.textRenderer, mainTitle, cardX + (CARD_WIDTH / 2), cardY + 50, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, subTitle, cardX + (CARD_WIDTH / 2), cardY + 62, 0xFF00E5FF);

            // Winner Bottom Bar
            if (isWinnerGlow) {
                context.fill(cardX + 1, cardY + CARD_HEIGHT - 3, cardX + CARD_WIDTH - 1, cardY + CARD_HEIGHT - 1, 0xFFFFD700);
            }
        }

        context.disableScissor();

        // Center Red Needle Marker
        context.fill(centerX - 1, viewY - 3, centerX + 1, viewY + viewH + 3, 0xFFFF2233);
        context.fill(centerX - 3, viewY - 5, centerX + 3, viewY - 2, 0xFFFF2233);
        context.fill(centerX - 3, viewY + viewH + 2, centerX + 3, viewY + viewH + 5, 0xFFFF2233);

        if (finished && winningIndex >= 0 && winningIndex < candidates.size()) {
            GachaItemCandidate win = candidates.get(winningIndex);
            context.drawCenteredTextWithShadow(this.textRenderer, "§e★ REWARD WON: §a" + win.displayName().toUpperCase() + " ★", centerX, centerY + 52, 0xFF55FF55);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private String getItemMainTitle(GachaItemCandidate item) {
        if ("bp".equalsIgnoreCase(item.category())) {
            return "+" + item.bpAmount() + " BP";
        }
        String name = item.displayName();
        if (name.contains(" x")) {
            return name.substring(0, name.lastIndexOf(" x"));
        }
        return name;
    }

    private String getItemSubTitle(GachaItemCandidate item) {
        if ("bp".equalsIgnoreCase(item.category())) {
            return item.bpAmount() >= 1000 ? "§6Jackpot" : "§eBattle Points";
        }
        if (item.quantity() > 1) {
            return "§eAmount: x" + item.quantity();
        }
        return "§b" + capitalize(item.category());
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "Item";
        return s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1).toLowerCase(java.util.Locale.ROOT);
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
}

