package com.vitwo.reward;

import com.vitwo.battle.TowerBattleManager;
import com.vitwo.config.TowerPlayerDataManager;
import com.vitwo.network.s2c.TowerBattleGradeS2CPacket;
import com.vitwo.party.TowerParty;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TowerRewardManager {
    private static final TowerRewardManager INSTANCE = new TowerRewardManager();
    public static TowerRewardManager getInstance() { return INSTANCE; }

    private static final Random RANDOM = new Random();

    // BP Shop 5-Category Price Table & Weekly Stock Limits
    public static final Map<String, Integer> BP_SHOP_PRICES = Map.ofEntries(
            // Held & Battle Items
            Map.entry("focus_sash", 200),
            Map.entry("choice_scarf", 200),
            Map.entry("choice_band", 200),
            Map.entry("choice_specs", 200),
            Map.entry("life_orb", 250),
            Map.entry("assault_vest", 200),
            Map.entry("heavy_duty_boots", 150),
            Map.entry("leftovers", 150),
            Map.entry("eviolite", 250),
            Map.entry("rocky_helmet", 150),
            Map.entry("expert_belt", 150),
            Map.entry("air_balloon", 150),
            Map.entry("weakness_policy", 200),
            Map.entry("toxic_orb", 150),
            Map.entry("flame_orb", 150),
            Map.entry("safety_goggles", 150),
            Map.entry("white_herb", 100),
            Map.entry("power_herb", 100),
            Map.entry("mental_herb", 100),
            Map.entry("mirror_herb", 150),
            Map.entry("black_sludge", 150),
            Map.entry("scope_lens", 100),
            Map.entry("wide_lens", 100),
            Map.entry("tera_shard_stellar", 500),

            // Mints & Training
            Map.entry("rare_candy", 30),
            Map.entry("exp_candy_xl", 50),
            Map.entry("exp_candy_l", 25),
            Map.entry("gold_bottle_cap", 350),
            Map.entry("bottle_cap", 100),
            Map.entry("pp_max", 200),
            Map.entry("pp_up", 80),
            Map.entry("adamant_mint", 100),
            Map.entry("modest_mint", 100),
            Map.entry("jolly_mint", 100),
            Map.entry("timid_mint", 100),
            Map.entry("bold_mint", 100),
            Map.entry("calm_mint", 100),
            Map.entry("brave_mint", 100),
            Map.entry("quiet_mint", 100),
            Map.entry("impish_mint", 100),
            Map.entry("careful_mint", 100),
            Map.entry("ability_capsule", 150),
            Map.entry("ability_patch", 400),
            Map.entry("power_bracer", 75),
            Map.entry("power_belt", 75),
            Map.entry("power_lens", 75),
            Map.entry("power_band", 75),
            Map.entry("power_anklet", 75),
            Map.entry("power_weight", 75),

            // Evolution Items
            Map.entry("fire_stone", 60),
            Map.entry("water_stone", 60),
            Map.entry("thunder_stone", 60),
            Map.entry("leaf_stone", 60),
            Map.entry("moon_stone", 80),
            Map.entry("sun_stone", 80),
            Map.entry("shiny_stone", 100),
            Map.entry("dusk_stone", 100),
            Map.entry("dawn_stone", 100),
            Map.entry("ice_stone", 80),
            Map.entry("electirizer", 150),
            Map.entry("magmarizer", 150),
            Map.entry("protector", 150),
            Map.entry("reaper_cloth", 150),
            Map.entry("dragon_scale", 150),
            Map.entry("prism_scale", 150),
            Map.entry("dubious_disc", 150),
            Map.entry("upgrade", 120),
            Map.entry("kings_rock", 120),
            Map.entry("metal_coat", 120),
            Map.entry("razor_fang", 120),
            Map.entry("razor_claw", 120),
            Map.entry("deep_sea_tooth", 120),
            Map.entry("deep_sea_scale", 120),
            Map.entry("oval_stone", 80),
            Map.entry("cracked_pot", 80),
            Map.entry("link_cable", 120),

            // Balls & Medicine
            Map.entry("master_ball", 1500),
            Map.entry("beast_ball", 300),
            Map.entry("cherish_ball", 250),
            Map.entry("luxury_ball", 100),
            Map.entry("heavy_ball", 100),
            Map.entry("fast_ball", 100),
            Map.entry("moon_ball", 100),
            Map.entry("dream_ball", 100),
            Map.entry("revival_herb", 80),
            Map.entry("max_elixir", 80),

            // Cosmetics & Prestige
            Map.entry("cosmetic_shiny_aura", 3000),
            Map.entry("cosmetic_particle_trail", 5000),
            Map.entry("cosmetic_victory_fanfare", 2000),
            Map.entry("weekly_challenge_reroll", 500),
            Map.entry("title_tower_champion", 5000),
            Map.entry("title_tower_legend", 10000)
    );

    public static final Map<String, Integer> WEEKLY_STOCK_LIMITS = Map.of(
            "ability_capsule", 5,
            "ability_patch", 2,
            "master_ball", 1,
            "gold_bottle_cap", 2,
            "weekly_challenge_reroll", 1
    );

    private TowerRewardManager() {}

    public void grantFloorReward(ServerPlayerEntity playerA, ServerPlayerEntity playerB, int floor, boolean isTrueRun, int turnsThisFloor, int faintsThisFloor) {
        var bpCfg = com.vitwo.config.TowerConfig.getInstance().bp;
        int baseBp = bpCfg.perFloor;
        if (floor % 10 == 0) baseBp += bpCfg.bossBonus;
        if (floor == 25) baseBp += bpCfg.checkpoint25Bonus;
        else if (floor == 50) baseBp += bpCfg.checkpoint50Bonus;
        else if (floor == 75) baseBp += bpCfg.checkpoint75Bonus;
        else if (floor == 100) baseBp += (isTrueRun ? bpCfg.clear100TrueRun : bpCfg.clear100CheckpointRun);

        // Calculate Battle Grade
        String grade;
        float gradeBonusMultiplier = 0.0f;
        if (faintsThisFloor == 0 && turnsThisFloor <= 6) {
            grade = "S";
            gradeBonusMultiplier = 0.25f; // +25% BP
        } else if (faintsThisFloor == 0 && turnsThisFloor <= 12) {
            grade = "A";
            gradeBonusMultiplier = 0.10f; // +10% BP
        } else if (faintsThisFloor <= 1) {
            grade = "B";
        } else {
            grade = "C";
        }

        if (playerA != null) {
            float mult = isTrueRun ? (float) bpCfg.trueRunMultiplier : TowerPlayerDataManager.getInstance().getCheckpointBpMultiplier(playerA.getUuid());
            float prestigeMult = TowerPlayerDataManager.getInstance().getPrestigeBpMultiplier(playerA.getUuid());
            int rankBonusBp = (int) Math.ceil(baseBp * gradeBonusMultiplier);
            int finalBp = Math.max(1, (int) Math.ceil((baseBp + rankBonusBp) * mult * prestigeMult));
            TowerPlayerDataManager.getInstance().addBp(playerA.getUuid(), finalBp);
            playerA.sendMessage(Text.literal("§6[CobbleTower] §a+" + finalBp + " BP §7(Total: " + TowerPlayerDataManager.getInstance().getBp(playerA.getUuid()) + " BP)"), false);
            ServerPlayNetworking.send(playerA, new TowerBattleGradeS2CPacket(floor, grade, rankBonusBp, turnsThisFloor, faintsThisFloor));
        }
        if (playerB != null) {
            float mult = isTrueRun ? (float) bpCfg.trueRunMultiplier : TowerPlayerDataManager.getInstance().getCheckpointBpMultiplier(playerB.getUuid());
            float prestigeMult = TowerPlayerDataManager.getInstance().getPrestigeBpMultiplier(playerB.getUuid());
            int rankBonusBp = (int) Math.ceil(baseBp * gradeBonusMultiplier);
            int finalBp = Math.max(1, (int) Math.ceil((baseBp + rankBonusBp) * mult * prestigeMult));
            TowerPlayerDataManager.getInstance().addBp(playerB.getUuid(), finalBp);
            playerB.sendMessage(Text.literal("§6[CobbleTower] §a+" + finalBp + " BP §7(Total: " + TowerPlayerDataManager.getInstance().getBp(playerB.getUuid()) + " BP)"), false);
            ServerPlayNetworking.send(playerB, new TowerBattleGradeS2CPacket(floor, grade, rankBonusBp, turnsThisFloor, faintsThisFloor));
        }

        // 2. Standard Items (Candies & Enhancements)
        ItemStack rewardItem = getRewardItemForFloor(floor);
        int amount = getRewardAmountForFloor(floor);
        rewardItem.setCount(amount);

        if (playerA != null) giveItemToPlayer(playerA, rewardItem.copy(), floor);
        if (playerB != null) giveItemToPlayer(playerB, rewardItem.copy(), floor);

        // 3. Pokémon Egg Reward (20% chance on regular floors, 100% on floor % 10 == 0)
        if (floor % 10 == 0 || RANDOM.nextInt(5) == 0) {
            grantPokemonEgg(playerA, floor);
            grantPokemonEgg(playerB, floor);
        }

        // 4. Checkpoint Special: Grant 1 random 6x31 MAX IV Pokémon from Boss Roster
        if (floor == 10 || floor == 25 || floor == 50 || floor == 75 || floor == 90 || floor == 100) {
            grantCheckpointMaxIvPokemon(playerA, floor);
            grantCheckpointMaxIvPokemon(playerB, floor);
        }
    }

    public void grantFloorReward(ServerPlayerEntity playerA, ServerPlayerEntity playerB, int floor, boolean isTrueRun) {
        grantFloorReward(playerA, playerB, floor, isTrueRun, 5, 0);
    }

    private void grantCheckpointMaxIvPokemon(ServerPlayerEntity player, int floor) {
        if (player == null || player.getServer() == null) return;
        String species = TowerBattleManager.getInstance().getRandomBossSpecies(floor);

        try {
            String cmd = "pokegive " + player.getName().getString() + " " + species + " ivs=31/31/31/31/31/31 level=1";
            ServerCommandSource source = player.getServer().getCommandSource().withSilent();
            player.getServer().getCommandManager().executeWithPrefix(source, cmd);
            player.sendMessage(Text.literal("§d★ [CobbleTower Checkpoint] You received a Perfect 6x31 IV §e" + species.toUpperCase() + " §dfrom the Boss!"), false);
        } catch (Exception ignored) {
            ItemStack fallbackItem = new ItemStack(Items.NETHER_STAR, 1);
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

    public void applyBaseHeal(ServerPlayerEntity player) {
        if (player == null) return;
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object cobblemonInst = cobblemonClass.getField("INSTANCE").get(null);
            Method getStorageMethod = cobblemonInst.getClass().getMethod("getStorage");
            Object storage = getStorageMethod.invoke(cobblemonInst);
            Method getPartyMethod = storage.getClass().getMethod("getParty", ServerPlayerEntity.class);
            Iterable<?> party = (Iterable<?>) getPartyMethod.invoke(storage, player);

            for (Object pokemon : party) {
                if (pokemon == null) continue;
                Method getCurrentHealth = pokemon.getClass().getMethod("getCurrentHealth");
                Method getMaxHealth = pokemon.getClass().getMethod("getMaxHealth");
                Method setCurrentHealth = pokemon.getClass().getMethod("setCurrentHealth", int.class);

                int currentHp = (int) getCurrentHealth.invoke(pokemon);
                int maxHp = (int) getMaxHealth.invoke(pokemon);

                if (currentHp > 0) {
                    int healHp = Math.min(maxHp, currentHp + (int) (maxHp * 0.25));
                    setCurrentHealth.invoke(pokemon, healHp);

                    try {
                        Method getMoveSetMethod = pokemon.getClass().getMethod("getMoveSet");
                        Object moveSet = getMoveSetMethod.invoke(pokemon);
                        Method getMovesMethod = moveSet.getClass().getMethod("getMoves");
                        Iterable<?> moves = (Iterable<?>) getMovesMethod.invoke(moveSet);
                        for (Object move : moves) {
                            if (move == null) continue;
                            Method getMaxPp = move.getClass().getMethod("getMaxPp");
                            Method getCurrentPp = move.getClass().getMethod("getCurrentPp");
                            Method setCurrentPp = move.getClass().getMethod("setCurrentPp", int.class);
                            int maxPp = (int) getMaxPp.invoke(move);
                            int currPp = (int) getCurrentPp.invoke(move);
                            int newPp = Math.min(maxPp, currPp + (int) (maxPp * 0.50));
                            setCurrentPp.invoke(move, newPp);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        player.sendMessage(Text.literal("§a[Rest Station] Base Refresh: Alive Pokémon received +25% HP and +50% PP!"), false);
    }

    public void applyFullTeamRest(ServerPlayerEntity player) {
        if (player == null) return;
        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object cobblemonInst = cobblemonClass.getField("INSTANCE").get(null);
            Method getStorageMethod = cobblemonInst.getClass().getMethod("getStorage");
            Object storage = getStorageMethod.invoke(cobblemonInst);
            Method getPartyMethod = storage.getClass().getMethod("getParty", ServerPlayerEntity.class);
            Iterable<?> party = (Iterable<?>) getPartyMethod.invoke(storage, player);

            for (Object pokemon : party) {
                if (pokemon == null) continue;
                Method getMaxHealth = pokemon.getClass().getMethod("getMaxHealth");
                Method setCurrentHealth = pokemon.getClass().getMethod("setCurrentHealth", int.class);
                int maxHp = (int) getMaxHealth.invoke(pokemon);
                setCurrentHealth.invoke(pokemon, maxHp);

                try {
                    Method getMoveSetMethod = pokemon.getClass().getMethod("getMoveSet");
                    Object moveSet = getMoveSetMethod.invoke(pokemon);
                    Method getMovesMethod = moveSet.getClass().getMethod("getMoves");
                    Iterable<?> moves = (Iterable<?>) getMovesMethod.invoke(moveSet);
                    for (Object move : moves) {
                        if (move == null) continue;
                        Method getMaxPp = move.getClass().getMethod("getMaxPp");
                        Method setCurrentPp = move.getClass().getMethod("setCurrentPp", int.class);
                        int maxPp = (int) getMaxPp.invoke(move);
                        setCurrentPp.invoke(move, maxPp);
                    }
                } catch (Exception ignored) {}

                try {
                    Method removeStatus = pokemon.getClass().getMethod("setStatus", Object.class);
                    removeStatus.invoke(pokemon, (Object) null);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        player.sendMessage(Text.literal("§b[Rest Station] Full Team Rest: Entire party revived to 100% HP, refreshed 100% PP, and cleared all status conditions!"), false);
    }

    public void applyWarPrep(ServerPlayerEntity player, TowerParty party, int buffType) {
        if (player == null || party == null) return;
        String buffName;
        if (buffType == 1) {
            party.setWarPrepBuff("ATTACK", 5);
            buffName = "§c+10% Attack & Sp. Atk";
        } else if (buffType == 2) {
            party.setWarPrepBuff("SPEED", 5);
            buffName = "§e+10% Speed";
        } else {
            party.setWarPrepBuff("DEFENSE", 5);
            buffName = "§9+10% Defense & Sp. Def";
        }
        player.sendMessage(Text.literal("§6[War Preparation] Activated " + buffName + " §6buff for the next 5 floors!"), false);
    }

    public void grantLootCache(ServerPlayerEntity player, int floor) {
        if (player == null) return;

        int bonusBp;
        Identifier lootId;
        int count = 1;

        if (floor <= 25) {
            bonusBp = 50;
            lootId = Identifier.of("cobblemon", "exp_candy_m");
            count = 2;
        } else if (floor <= 50) {
            bonusBp = 100;
            lootId = Identifier.of("cobblemon", "bottle_cap");
            count = 1;
        } else if (floor <= 75) {
            bonusBp = 200;
            lootId = Identifier.of("cobblemon", "ability_patch");
            count = 1;
        } else {
            bonusBp = 500;
            lootId = Identifier.of("cobblemon", "gold_bottle_cap");
            count = 1;
        }

        TowerPlayerDataManager.getInstance().addBp(player.getUuid(), bonusBp);
        player.sendMessage(Text.literal("§6[Treasure Cache] §a+" + bonusBp + " BP §7found in the treasure chest!"), false);

        if (Registries.ITEM.containsId(lootId)) {
            ItemStack stack = new ItemStack(Registries.ITEM.get(lootId), count);
            giveItemToPlayer(player, stack, floor);
        }
    }

    public void handleBpPurchase(ServerPlayerEntity player, String itemId, int quantity) {
        if (player == null || itemId == null) return;
        quantity = Math.max(1, Math.min(64, quantity));

        Integer unitPrice = BP_SHOP_PRICES.get(itemId);
        if (unitPrice == null) {
            player.sendMessage(Text.literal("§c[BP Shop] Invalid item selection."), false);
            return;
        }

        int totalPrice = unitPrice * quantity;
        var profile = TowerPlayerDataManager.getInstance().getProfile(player.getUuid());

        // 1. Tier Unlock Validation
        boolean isSilverTier = List.of("ability_capsule", "bottle_cap", "toxic_orb", "flame_orb", "eviolite", "pp_up").contains(itemId);
        boolean isGoldTier = List.of("ability_patch", "gold_bottle_cap", "tera_shard_stellar", "master_ball", "pp_max", "title_tower_champion").contains(itemId);
        boolean isPlatinumTier = List.of("cosmetic_shiny_aura", "cosmetic_particle_trail", "cosmetic_victory_fanfare", "weekly_challenge_reroll", "title_tower_legend").contains(itemId);

        if (isSilverTier && profile.highestFloorTrueRun < 50) {
            player.sendMessage(Text.literal("§c[BP Shop] Locked! Silver Tier unlocks after clearing Floor 50 in True Run."), false);
            return;
        }

        if (isGoldTier && profile.highestFloorTrueRun < 100) {
            player.sendMessage(Text.literal("§c[BP Shop] Locked! Gold Tier unlocks after conquering Floor 100 (Master Tower)."), false);
            return;
        }

        if (isPlatinumTier && profile.prestigeLevel < 3) {
            player.sendMessage(Text.literal("§c[BP Shop] Locked! Platinum Tier unlocks at Prestige Level 3."), false);
            return;
        }

        if (itemId.equals("title_tower_legend") && profile.prestigeLevel < 5) {
            player.sendMessage(Text.literal("§c[BP Shop] Locked! Title « Tower Legend » requires Prestige Level 5 (Paragon)."), false);
            return;
        }

        // 2. Weekly Stock Limit Validation
        if (WEEKLY_STOCK_LIMITS.containsKey(itemId)) {
            int limit = WEEKLY_STOCK_LIMITS.get(itemId);
            int purchased = TowerPlayerDataManager.getInstance().getWeeklyPurchasedStock(player.getUuid(), itemId);
            if (purchased + quantity > limit) {
                player.sendMessage(Text.literal("§c[BP Shop] Weekly stock limit reached (" + purchased + "/" + limit + ")! Max you can buy now is " + Math.max(0, limit - purchased) + "."), false);
                return;
            }
        }

        boolean success = TowerPlayerDataManager.getInstance().spendBp(player.getUuid(), totalPrice);
        if (!success) {
            player.sendMessage(Text.literal("§c[BP Shop] Not enough Battle Points! Required: §e" + totalPrice + " BP§c, Balance: §e" + profile.battlePoints + " BP"), false);
            return;
        }

        if (WEEKLY_STOCK_LIMITS.containsKey(itemId)) {
            TowerPlayerDataManager.getInstance().recordStockPurchase(player.getUuid(), itemId, quantity);
        }

        if (itemId.equals("title_tower_champion")) {
            TowerPlayerDataManager.getInstance().unlockCosmetic(player.getUuid(), itemId);
            player.sendMessage(Text.literal("§6★ [BP Shop] You unlocked the cosmetic title: §e« Tower Champion »§6!"), false);
            return;
        }

        if (itemId.equals("title_tower_legend")) {
            TowerPlayerDataManager.getInstance().unlockCosmetic(player.getUuid(), itemId);
            player.sendMessage(Text.literal("§d★ [BP Shop] You unlocked the supreme cosmetic title: §b« Tower Legend »§d!"), false);
            return;
        }

        if (itemId.startsWith("cosmetic_")) {
            TowerPlayerDataManager.getInstance().unlockCosmetic(player.getUuid(), itemId);
            player.sendMessage(Text.literal("§d★ [BP Shop] Unlocked & activated cosmetic: §b" + itemId.replace("cosmetic_", "").replace("_", " ") + "§d!"), false);
            return;
        }

        Identifier cobbleId = Identifier.of("cobblemon", itemId);
        Identifier mcId = Identifier.of("minecraft", itemId);
        ItemStack stack;
        if (Registries.ITEM.containsId(cobbleId)) {
            stack = new ItemStack(Registries.ITEM.get(cobbleId), quantity);
        } else if (Registries.ITEM.containsId(mcId)) {
            stack = new ItemStack(Registries.ITEM.get(mcId), quantity);
        } else {
            stack = new ItemStack(Items.DIAMOND, quantity);
        }

        giveItemToPlayer(player, stack, 1);
        player.sendMessage(Text.literal("§a[BP Shop] Purchased §e" + stack.getName().getString() + (quantity > 1 ? (" x" + quantity) : "") + " §afor §e" + totalPrice + " BP§a! Balance: §e" + TowerPlayerDataManager.getInstance().getBp(player.getUuid()) + " BP"), false);
    }

    public void handleBpPurchase(ServerPlayerEntity player, String itemId) {
        handleBpPurchase(player, itemId, 1);
    }

    private void giveItemToPlayer(ServerPlayerEntity player, ItemStack stack, int floor) {
        if (player == null || stack.isEmpty()) return;
        String itemName = stack.getName().getString();
        int count = stack.getCount();

        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }

        player.sendMessage(Text.literal("§6[CobbleTower] §aReceived: §e" + itemName + (count > 1 ? (" x" + count) : "")), false);
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

    public void checkMilestones(ServerPlayerEntity p1, ServerPlayerEntity p2, int floor, boolean isTrueRun) {
        Identifier advId = null;
        if (floor == 10) advId = Identifier.of("vitwo", "poke_tower");
        else if (floor == 25) advId = Identifier.of("vitwo", "great_tower");
        else if (floor == 50) advId = Identifier.of("vitwo", "ultra_tower");
        else if (floor == 75) advId = Identifier.of("vitwo", "floor_75");
        else if (floor == 90) advId = Identifier.of("vitwo", "floor_90");
        else if (floor == 100 && isTrueRun) {
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
