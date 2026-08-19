package com.vitwo.battle;

import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.*;

public class TowerClauseManager {
    private static final TowerClauseManager INSTANCE = new TowerClauseManager();
    public static TowerClauseManager getInstance() { return INSTANCE; }

    public static final Set<String> LEGENDARY_AND_MYTHICAL = Set.of(
            "mewtwo", "mew", "articuno", "zapdos", "moltres",
            "raikou", "entei", "suicune", "lugia", "ho-oh", "celebi",
            "regirock", "regice", "registeel", "latias", "latios", "kyogre", "groudon", "rayquaza", "jirachi", "deoxys",
            "uxie", "mesprit", "azelf", "dialga", "palkia", "heatran", "regigigas", "giratina", "cresselia", "phione", "manaphy", "darkrai", "shaymin", "arceus",
            "victini", "cobalion", "terrakion", "virizion", "tornadus", "thundurus", "reshiram", "zekrom", "landorus", "kyurem", "keldeo", "meloetta", "genesect",
            "xerneas", "yveltal", "zygarde", "diancie", "hoopa", "volcanion",
            "type:_null", "silvally", "tapu_koko", "tapu_lele", "tapu_bulu", "tapu_fini", "cosmog", "cosmoem", "solgaleo", "lunala", "nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela", "kartana", "guzzlord", "necrozma", "magearna", "marshadow", "poipole", "naganadel", "stakataka", "blacephalon", "zeraora", "meltan", "melmetal",
            "zacian", "zamazenta", "eternatus", "kubfu", "urshifu", "zarude", "regieleki", "regidrago", "glastrier", "spectrier", "calyrex", "enamorus",
            "wo-chien", "chien-pao", "ting-lu", "chi-yu", "roaring_moon", "iron_valiant", "koraidon", "miraidon", "walking_wake", "iron_leaves", "okidogi", "munkidori", "fezandipiti", "ogerpon", "gouging_fire", "raging_bolt", "iron_boulder", "iron_crown", "terapagos", "pecharunt"
    );

    private TowerClauseManager() {}

    public static class ValidationResult {
        public final boolean valid;
        public final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String msg) { return new ValidationResult(false, msg); }
    }

    public int getMaxAllowedLegendaries(int floor) {
        if (floor <= 75) return 1;
        return 2;
    }

    /**
     * Validates that the player's team adheres to Species Clause, Item Clause, and Legendary Limits.
     */
    public ValidationResult validateTeam(ServerPlayerEntity player, int floor) {
        if (player == null) return ValidationResult.ok();

        try {
            // Use Cobblemon API via reflection to safely inspect party in runtime
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object cobblemonInst = cobblemonClass.getField("INSTANCE").get(null);
            Method getStorageMethod = cobblemonInst.getClass().getMethod("getStorage");
            Object storage = getStorageMethod.invoke(cobblemonInst);
            Method getPartyMethod = storage.getClass().getMethod("getParty", ServerPlayerEntity.class);
            Iterable<?> party = (Iterable<?>) getPartyMethod.invoke(storage, player);

            Set<String> speciesSet = new HashSet<>();
            Set<String> itemSet = new HashSet<>();
            int legendaryCount = 0;
            int maxLegendaries = getMaxAllowedLegendaries(floor);

            for (Object pokemon : party) {
                if (pokemon == null) continue;

                // Check Species
                Method getSpeciesMethod = pokemon.getClass().getMethod("getSpecies");
                Object speciesObj = getSpeciesMethod.invoke(pokemon);
                Method getNameMethod = speciesObj.getClass().getMethod("getName");
                String speciesName = ((String) getNameMethod.invoke(speciesObj)).toLowerCase();

                // Species Clause
                if (!speciesSet.add(speciesName)) {
                    return ValidationResult.fail("§c[Species Clause] Duplicate Pokémon not allowed: §e" + speciesName.toUpperCase());
                }

                // Legendary Limit
                if (isLegendaryOrMythical(speciesName)) {
                    legendaryCount++;
                    if (legendaryCount > maxLegendaries) {
                        return ValidationResult.fail("§c[Legendary Cap] Max " + maxLegendaries + " Legendary allowed for Floor " + floor + "! Found: §e" + legendaryCount);
                    }
                }

                // Item Clause
                try {
                    Method getHeldItemMethod = pokemon.getClass().getMethod("heldItem");
                    Object itemStack = getHeldItemMethod.invoke(pokemon);
                    if (itemStack != null) {
                        Method isEmptyMethod = itemStack.getClass().getMethod("isEmpty");
                        boolean isEmpty = (boolean) isEmptyMethod.invoke(itemStack);
                        if (!isEmpty) {
                            Method getItemMethod = itemStack.getClass().getMethod("getItem");
                            Object item = getItemMethod.invoke(itemStack);
                            String itemName = item.toString();
                            if (!itemSet.add(itemName)) {
                                return ValidationResult.fail("§c[Item Clause] Duplicate held item not allowed across party: §e" + itemName);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // Fallback if Cobblemon classes aren't in standard path
            return ValidationResult.ok();
        }

        return ValidationResult.ok();
    }

    public boolean isLegendaryOrMythical(String species) {
        if (species == null) return false;
        String clean = species.toLowerCase().replace(" ", "_").replace("-", "_");
        return LEGENDARY_AND_MYTHICAL.contains(clean);
    }
}
