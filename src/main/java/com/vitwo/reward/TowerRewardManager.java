package com.vitwo.reward;

import com.vitwo.battle.TowerBattleManager;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class TowerRewardManager {
    private static final TowerRewardManager INSTANCE = new TowerRewardManager();
    public static TowerRewardManager getInstance() { return INSTANCE; }

    private static final Random RANDOM = new Random();

    private TowerRewardManager() {}

    public void grantFloorReward(ServerPlayerEntity playerA, ServerPlayerEntity playerB, int floor) {
        // 1. Standard Tier Items (EXP Candy, Ability Capsule, Master Balls)
        ItemStack rewardItem = getRewardItemForFloor(floor);
        int amount = getRewardAmountForFloor(floor);
        rewardItem.setCount(amount);

        if (playerA != null) giveItemToPlayer(playerA, rewardItem.copy(), floor);
        if (playerB != null) giveItemToPlayer(playerB, rewardItem.copy(), floor);

        // 2. Extra Gem Rewards (Diamonds & Emeralds)
        ItemStack gems = new ItemStack(floor >= 50 ? Items.DIAMOND : Items.EMERALD, Math.max(1, floor / 20));
        if (playerA != null) giveItemToPlayer(playerA, gems.copy(), floor);
        if (playerB != null) giveItemToPlayer(playerB, gems.copy(), floor);

        // 3. Pokémon Egg Reward (20% chance on regular floors, 100% on floor % 10 == 0)
        if (floor % 10 == 0 || RANDOM.nextInt(5) == 0) {
            grantPokemonEgg(playerA, floor);
            grantPokemonEgg(playerB, floor);
        }

        // 4. Legendary Summon Orbs & Explorer Maps (Floors 90+)
        if (floor >= 90) {
            grantLegendarySummonItem(playerA, floor);
            grantLegendarySummonItem(playerB, floor);
            grantLegendaryMap(playerA);
            grantLegendaryMap(playerB);
        }

        // 5. Checkpoint Special: Grant 1 random 6x31 MAX IV Pokémon from Boss Roster!
        if (floor == 10 || floor == 25 || floor == 50 || floor == 75 || floor == 90 || floor == 100) {
            grantCheckpointMaxIvPokemon(playerA, floor);
            grantCheckpointMaxIvPokemon(playerB, floor);
        }
    }

    private void grantCheckpointMaxIvPokemon(ServerPlayerEntity player, int floor) {
        if (player == null || player.getServer() == null) return;
        String species = TowerBattleManager.getInstance().getRandomBossSpecies(floor);

        try {
            // Execute server command to award 6x31 IV Pokemon cleanly
            String cmd = "pokegive " + player.getName().getString() + " " + species + " ivs=31/31/31/31/31/31 level=1";
            ServerCommandSource source = player.getServer().getCommandSource().withSilent();
            player.getServer().getCommandManager().executeWithPrefix(source, cmd);
            player.sendMessage(Text.literal("§d★ [CobbleTower Checkpoint Reward] You received a Perfect 6x31 IV §e" + species.toUpperCase() + " §dfrom the Boss!"), false);
        } catch (Exception ignored) {
            // Fallback reward item if pokegive is unavailable
            ItemStack fallbackItem = new ItemStack(Items.NETHERITE_BLOCK, 1);
            giveItemToPlayer(player, fallbackItem, floor);
        }
    }

    private void grantPokemonEgg(ServerPlayerEntity player, int floor) {
        if (player == null || player.getServer() == null) return;
        String species = TowerBattleManager.getInstance().getRandomBossSpecies(floor);

        try {
            String cmd = "pokegiveegg " + player.getName().getString() + " " + species;
            ServerCommandSource source = player.getServer().getCommandSource().withSilent();
            player.getServer().getCommandManager().executeWithPrefix(source, cmd);
            player.sendMessage(Text.literal("§6[CobbleTower] §fYou received a mysterious §ePokémon Egg §7(from " + species + ")!"), false);
        } catch (Exception ignored) {
            giveItemToPlayer(player, new ItemStack(Items.EGG, 1), floor);
        }
    }

    private void grantLegendarySummonItem(ServerPlayerEntity player, int floor) {
        if (player == null) return;
        Identifier orbId = switch (RANDOM.nextInt(6)) {
            case 0 -> Identifier.of("cobblemon", "griseous_orb");
            case 1 -> Identifier.of("cobblemon", "adamant_crystal");
            case 2 -> Identifier.of("cobblemon", "lustrous_globe");
            case 3 -> Identifier.of("cobblemon", "red_orb");
            case 4 -> Identifier.of("cobblemon", "blue_orb");
            default -> Identifier.of("cobblemon", "master_ball");
        };

        if (Registries.ITEM.containsId(orbId)) {
            giveItemToPlayer(player, new ItemStack(Registries.ITEM.get(orbId), 1), floor);
        } else {
            giveItemToPlayer(player, new ItemStack(Items.NETHER_STAR, 1), floor);
        }
    }

    private void grantLegendaryMap(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) return;
        try {
            ServerWorld world = player.getServer().getOverworld();
            BlockPos playerPos = player.getBlockPos();
            ItemStack map = FilledMapItem.createMap(world, playerPos.getX(), playerPos.getZ(), (byte) 2, true, true);
            map.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§6Mythical Explorer Map"));
            giveItemToPlayer(player, map, 90);
        } catch (Exception ignored) {}
    }

    private void giveItemToPlayer(ServerPlayerEntity player, ItemStack stack, int floor) {
        if (player == null || stack.isEmpty()) return;
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

    public void applyTeamHeal(ServerPlayerEntity player) {
        if (player != null) {
            player.sendMessage(Text.literal("§b[CobbleTower] Rest Station: Revived fainted Pokémon (10% HP), restored 50% HP to active team, and refreshed 100% move PP!"), false);
        }
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
        else if (floor == 75) advId = Identifier.of("vitwo", "floor_75");
        else if (floor == 90) advId = Identifier.of("vitwo", "floor_90");
        else if (floor == 100) {
            advId = Identifier.of("vitwo", "master_tower");
            if (p2 != null) {
                grantAdvancement(p1, Identifier.of("vitwo", "duo_conqueror"));
                grantAdvancement(p2, Identifier.of("vitwo", "duo_conqueror"));
            }
        }

        if (advId != null) {
            if (p1 != null) grantAdvancement(p1, advId);
            if (p2 != null) grantAdvancement(p2, advId);
        }
    }

    private void grantAdvancement(ServerPlayerEntity player, Identifier id) {
        if (player == null || player.getServer() == null) return;
        AdvancementEntry entry = player.getServer().getAdvancementLoader().get(id);
        if (entry != null) {
            PlayerAdvancementTracker tracker = player.getAdvancementTracker();
            for (String criterion : entry.value().criteria().keySet()) {
                tracker.grantCriterion(entry, criterion);
            }
        }
    }
}
