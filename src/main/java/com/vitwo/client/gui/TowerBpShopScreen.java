package com.vitwo.client.gui;

import com.vitwo.network.c2s.BuyBpItemC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TowerBpShopScreen extends Screen {
    public static int currentBpBalance = 0;
    private int selectedTier = 0; // 0 = Bronze, 1 = Silver, 2 = Gold, 3 = Platinum

    public record ShopEntry(String id, String displayName, int price, String category, int tier, String weeklyLimit) {}

    private static final List<ShopEntry> ALL_ENTRIES = List.of(
            // Bronze Tier (Tier 0)
            new ShopEntry("rare_candy", "Rare Candy", 30, "EXP", 0, "Unlimited"),
            new ShopEntry("adamant_mint", "Adamant Mint (+Atk, -SpAtk)", 100, "Mints", 0, "Unlimited"),
            new ShopEntry("modest_mint", "Modest Mint (+SpAtk, -Atk)", 100, "Mints", 0, "Unlimited"),
            new ShopEntry("jolly_mint", "Jolly Mint (+Spe, -SpAtk)", 100, "Mints", 0, "Unlimited"),
            new ShopEntry("timid_mint", "Timid Mint (+Spe, -Atk)", 100, "Mints", 0, "Unlimited"),
            new ShopEntry("bold_mint", "Bold Mint (+Def, -Atk)", 100, "Mints", 0, "Unlimited"),
            new ShopEntry("calm_mint", "Calm Mint (+SpDef, -Atk)", 100, "Mints", 0, "Unlimited"),
            new ShopEntry("focus_sash", "Focus Sash", 200, "Items", 0, "Unlimited"),
            new ShopEntry("choice_scarf", "Choice Scarf", 200, "Items", 0, "Unlimited"),
            new ShopEntry("choice_band", "Choice Band", 200, "Items", 0, "Unlimited"),
            new ShopEntry("choice_specs", "Choice Specs", 200, "Items", 0, "Unlimited"),
            new ShopEntry("life_orb", "Life Orb", 250, "Items", 0, "Unlimited"),
            new ShopEntry("assault_vest", "Assault Vest", 200, "Items", 0, "Unlimited"),
            new ShopEntry("heavy_duty_boots", "Heavy-Duty Boots", 150, "Items", 0, "Unlimited"),
            new ShopEntry("leftovers", "Leftovers", 150, "Items", 0, "Unlimited"),
            new ShopEntry("booster_energy", "Booster Energy", 300, "Items", 0, "Unlimited"),

            // Silver Tier (Tier 1 - Unlocks at F50)
            new ShopEntry("ability_capsule", "Ability Capsule", 150, "Abilities", 1, "Max 5/wk"),
            new ShopEntry("bottle_cap", "Bottle Cap (Silver)", 100, "IVs", 1, "Max 10/wk"),
            new ShopEntry("toxic_orb", "Toxic Orb", 150, "Items", 1, "Unlimited"),
            new ShopEntry("flame_orb", "Flame Orb", 150, "Items", 1, "Unlimited"),
            new ShopEntry("eviolite", "Eviolite", 250, "Items", 1, "Unlimited"),

            // Gold Tier (Tier 2 - Unlocks at F100)
            new ShopEntry("ability_patch", "Ability Patch (Hidden)", 400, "Abilities", 2, "Max 2/wk"),
            new ShopEntry("gold_bottle_cap", "Gold Bottle Cap (6x31)", 500, "IVs", 2, "Max 2/wk"),
            new ShopEntry("tera_shard_stellar", "Stellar Tera Shard", 50, "Gimmicks", 2, "Unlimited"),
            new ShopEntry("master_ball", "Master Ball (100% Catch)", 1500, "Pokéballs", 2, "Max 1/wk"),
            new ShopEntry("title_tower_champion", "« Tower Champion » Title", 5000, "Cosmetic", 2, "1-time"),

            // Platinum Tier (Tier 3 - Unlocks at Prestige 3+)
            new ShopEntry("cosmetic_shiny_aura", "Shiny Aura (Aesthetic)", 3000, "Cosmetic", 3, "1-time"),
            new ShopEntry("cosmetic_particle_trail", "Tower Particle Trail", 5000, "Cosmetic", 3, "1-time"),
            new ShopEntry("cosmetic_victory_fanfare", "Custom Victory Fanfare", 2000, "Cosmetic", 3, "1-time"),
            new ShopEntry("weekly_challenge_reroll", "Re-roll Weekly Challenge", 500, "Utility", 3, "Max 1/wk"),
            new ShopEntry("title_tower_legend", "« Tower Legend » Title", 10000, "Cosmetic", 3, "Prestige 5")
    );

    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 5;
    private final Map<String, ItemStack> itemStackCache = new HashMap<>();

    public TowerBpShopScreen() {
        super(Text.literal("Battle Point Exchange"));
    }

    private List<ShopEntry> getFilteredEntries() {
        return ALL_ENTRIES.stream().filter(e -> e.tier() == selectedTier).toList();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Tier Tab Switchers (Bronze, Silver, Gold, Platinum)
        this.addDrawableChild(ButtonWidget.builder(Text.literal(selectedTier == 0 ? "§6§l[ BRONZE ]" : "§7Bronze"), btn -> {
            selectedTier = 0;
            scrollOffset = 0;
            this.clearAndInit();
        }).dimensions(centerX - 180, centerY - 88, 85, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(selectedTier == 1 ? "§f§l[ SILVER ]" : "§7Silver (F50)"), btn -> {
            selectedTier = 1;
            scrollOffset = 0;
            this.clearAndInit();
        }).dimensions(centerX - 90, centerY - 88, 85, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(selectedTier == 2 ? "§e§l[ GOLD ]" : "§7Gold (F100)"), btn -> {
            selectedTier = 2;
            scrollOffset = 0;
            this.clearAndInit();
        }).dimensions(centerX + 0, centerY - 88, 85, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(selectedTier == 3 ? "§b§l[ PLATINUM ]" : "§7Platinum (P3)"), btn -> {
            selectedTier = 3;
            scrollOffset = 0;
            this.clearAndInit();
        }).dimensions(centerX + 90, centerY - 88, 90, 20).build());

        List<ShopEntry> currentList = getFilteredEntries();
        int startY = centerY - 60;
        int maxIndex = Math.min(scrollOffset + ITEMS_PER_PAGE, currentList.size());

        for (int i = scrollOffset; i < maxIndex; i++) {
            ShopEntry entry = currentList.get(i);
            int rowY = startY + (i - scrollOffset) * 26;

            ButtonWidget buyBtn = ButtonWidget.builder(Text.literal("§aBuy §e(" + entry.price() + " BP)"), btn -> {
                ClientPlayNetworking.send(new BuyBpItemC2SPacket(entry.id()));
            }).dimensions(centerX + 60, rowY, 110, 22).build();

            buyBtn.active = (currentBpBalance >= entry.price());
            this.addDrawableChild(buyBtn);
        }

        // Pagination Buttons
        ButtonWidget prevBtn = ButtonWidget.builder(Text.literal("▲ Prev"), btn -> {
            if (scrollOffset > 0) {
                scrollOffset -= ITEMS_PER_PAGE;
                if (scrollOffset < 0) scrollOffset = 0;
                this.clearAndInit();
            }
        }).dimensions(centerX - 180, centerY + 85, 80, 20).build();
        prevBtn.active = (scrollOffset > 0);
        this.addDrawableChild(prevBtn);

        ButtonWidget nextBtn = ButtonWidget.builder(Text.literal("▼ Next"), btn -> {
            if (scrollOffset + ITEMS_PER_PAGE < currentList.size()) {
                scrollOffset += ITEMS_PER_PAGE;
                this.clearAndInit();
            }
        }).dimensions(centerX - 95, centerY + 85, 80, 20).build();
        nextBtn.active = (scrollOffset + ITEMS_PER_PAGE < currentList.size());
        this.addDrawableChild(nextBtn);

        // Back / Close Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§fBack to Hub"), btn -> {
            this.client.setScreen(new TowerHubScreen());
        }).dimensions(centerX + 60, centerY + 85, 110, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Container Window (Slate with Golden Cyan accent)
        context.fill(centerX - 190, centerY - 120, centerX + 190, centerY + 115, 0xF012171E);
        context.drawBorder(centerX - 190, centerY - 120, 380, 235, 0xFF0FD9C2);
        context.fill(centerX - 189, centerY - 119, centerX + 189, centerY - 116, 0xFF0FD9C2);

        super.render(context, mouseX, mouseY, delta);

        // Header Title & Balance
        context.drawCenteredTextWithShadow(this.textRenderer, "§6§l❖ BATTLE POINT EXCHANGE ❖", centerX - 60, centerY - 108, 0xFFD700);
        context.drawTextWithShadow(this.textRenderer, "§6Balance: §e" + currentBpBalance + " BP", centerX + 50, centerY - 108, 0xFFFFFF);

        List<ShopEntry> currentList = getFilteredEntries();
        int startY = centerY - 60;
        int maxIndex = Math.min(scrollOffset + ITEMS_PER_PAGE, currentList.size());

        for (int i = scrollOffset; i < maxIndex; i++) {
            ShopEntry entry = currentList.get(i);
            int rowY = startY + (i - scrollOffset) * 26;

            // Row background
            context.fill(centerX - 180, rowY - 1, centerX + 55, rowY + 21, 0x50000000);
            context.drawBorder(centerX - 180, rowY - 1, 235, 22, 0x30FFFFFF);

            // Draw Item Icon
            ItemStack stack = getItemStackForId(entry.id());
            context.drawItem(stack, centerX - 176, rowY + 2);
            context.drawItemInSlot(this.textRenderer, stack, centerX - 176, rowY + 2);

            // Item Name & Category / Limits
            context.drawTextWithShadow(this.textRenderer, "§e" + entry.displayName(), centerX - 154, rowY + 3, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "§8[" + entry.category() + "] §7" + entry.weeklyLimit(), centerX - 154, rowY + 12, 0xAAAAAA);
        }
    }

    private ItemStack getItemStackForId(String id) {
        if (id == null) return new ItemStack(Items.BARRIER);
        return itemStackCache.computeIfAbsent(id, k -> {
            Identifier cobblemonId = Identifier.of("cobblemon", k);
            if (Registries.ITEM.containsId(cobblemonId)) {
                return new ItemStack(Registries.ITEM.get(cobblemonId));
            }

            Identifier mcId = Identifier.of("minecraft", k);
            if (Registries.ITEM.containsId(mcId)) {
                return new ItemStack(Registries.ITEM.get(mcId));
            }

            return switch (k) {
                case "rare_candy" -> new ItemStack(Items.EXPERIENCE_BOTTLE);
                case "gold_bottle_cap" -> new ItemStack(Items.GOLD_INGOT);
                case "bottle_cap" -> new ItemStack(Items.IRON_NUGGET);
                case "master_ball" -> new ItemStack(Items.ENDER_EYE);
                case "tera_shard_stellar" -> new ItemStack(Items.AMETHYST_SHARD);
                case "cosmetic_shiny_aura", "cosmetic_particle_trail" -> new ItemStack(Items.NETHER_STAR);
                case "cosmetic_victory_fanfare" -> new ItemStack(Items.JUKEBOX);
                case "weekly_challenge_reroll" -> new ItemStack(Items.CLOCK);
                case "title_tower_champion", "title_tower_legend" -> new ItemStack(Items.NAME_TAG);
                default -> new ItemStack(Items.EMERALD);
            };
        });
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
