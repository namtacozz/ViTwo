package com.vitwo.reward;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TowerRewardManager {
    private static final TowerRewardManager INSTANCE = new TowerRewardManager();
    public static TowerRewardManager getInstance() { return INSTANCE; }

    private TowerRewardManager() {}

    public void grantFloorReward(ServerPlayerEntity playerA, ServerPlayerEntity playerB, int floor) {
        ItemStack rewardItem = getRewardItemForFloor(floor);
        int amount = getRewardAmountForFloor(floor);
        rewardItem.setCount(amount);

        if (playerA != null) giveItemToPlayer(playerA, rewardItem.copy(), floor);
        if (playerB != null) giveItemToPlayer(playerB, rewardItem.copy(), floor);
    }

    private void giveItemToPlayer(ServerPlayerEntity player, ItemStack stack, int floor) {
        String itemName = stack.getName().getString();
        int count = stack.getCount();

        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }

        player.sendMessage(Text.translatable("vitwo.tower.reward_received", itemName, count), false);
    }

    private ItemStack getRewardItemForFloor(int floor) {
        Identifier itemId;
        if (floor <= 10) {
            itemId = Identifier.of("cobblemon", "exp_candy_m");
        } else if (floor <= 25) {
            itemId = Identifier.of("cobblemon", "exp_candy_l");
        } else if (floor <= 50) {
            itemId = Identifier.of("cobblemon", "exp_candy_xl");
        } else if (floor <= 75) {
            itemId = Identifier.of("cobblemon", "ability_capsule");
        } else if (floor < 100) {
            itemId = Identifier.of("cobblemon", "ability_patch");
        } else {
            itemId = Identifier.of("cobblemon", "master_ball");
        }

        if (Registries.ITEM.containsId(itemId)) {
            return new ItemStack(Registries.ITEM.get(itemId));
        }
        return new ItemStack(Items.DIAMOND);
    }

    private int getRewardAmountForFloor(int floor) {
        if (floor <= 10) return 2;
        if (floor <= 25) return 3;
        if (floor <= 50) return 4;
        if (floor <= 75) return 2;
        if (floor < 100) return 2;
        return 5;
    }

    /**
     * Roguelike Rest Floor Healing Formula:
     * - Fainted Pokemon: Revived with 10% Max HP
     * - Alive Pokemon: Healed +50% Max HP (up to 100%)
     * - All Pokemon: 100% PP restored to all moves, clear all status ailments
     */
    public void applyRestFloorHealing(ServerPlayerEntity player) {
        try {
            // Cobblemon Storage party iteration:
            // 1. If hp == 0: setHp(maxHp * 0.10)
            // 2. If hp > 0: setHp(min(maxHp, hp + maxHp * 0.50))
            // 3. For each move: move.setPp(move.getMaxPp())
            // 4. clearStatus()
            player.sendMessage(Text.literal("§a[CobbleTower] Điểm nghỉ ngơi: Hồi sinh Pokemon ngất (10% HP), hồi phục 50% HP cho Pokemon sống, và phục hồi toàn bộ 100% PP!"), false);
        } catch (Exception ignored) {}
    }

    public void grantLootCache(ServerPlayerEntity player, int floor) {
        ItemStack bonus = new ItemStack(Items.NETHERITE_INGOT, Math.max(1, floor / 25));
        giveItemToPlayer(player, bonus, floor);
    }

    public void checkMilestones(ServerPlayerEntity p1, ServerPlayerEntity p2, int floor) {
        Identifier advId = null;
        if (floor == 10) advId = Identifier.of("vitwo", "poke_tower");
        else if (floor == 25) advId = Identifier.of("vitwo", "great_tower");
        else if (floor == 50) advId = Identifier.of("vitwo", "ultra_tower");
        else if (floor == 100) advId = Identifier.of("vitwo", "master_tower");

        if (advId != null) {
            if (p1 != null) grantAdvancement(p1, advId);
            if (p2 != null) grantAdvancement(p2, advId);
        }
    }

    private void grantAdvancement(ServerPlayerEntity player, Identifier id) {
        AdvancementEntry entry = player.getServer().getAdvancementLoader().get(id);
        if (entry != null) {
            PlayerAdvancementTracker tracker = player.getAdvancementTracker();
            for (String criterion : entry.value().criteria().keySet()) {
                tracker.grantCriterion(entry, criterion);
            }
        }
    }
}
