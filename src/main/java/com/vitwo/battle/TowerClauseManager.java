package com.vitwo.battle;

import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.*;

public class TowerClauseManager {
    private static final TowerClauseManager INSTANCE = new TowerClauseManager();
    public static TowerClauseManager getInstance() { return INSTANCE; }

    // Box & Cover Legends (Restricted)
    public static final Set<String> RESTRICTED_LEGENDARIES = Set.of(
            "mewtwo", "lugia", "ho_oh", "kyogre", "groudon", "rayquaza",
            "dialga", "palkia", "giratina", "reshiram", "zekrom", "kyurem", "kyurem_white", "kyurem_black",
            "xerneas", "yveltal", "zygarde", "solgaleo", "lunala",
            "necrozma", "necrozma_dusk_mane", "necrozma_dawn_wings", "zacian", "zamazenta", "eternatus",
            "calyrex", "koraidon", "miraidon", "terapagos", "arceus"
    );

    // Sub-Legendaries & Mythicals (Non-Restricted)
    public static final Set<String> NON_RESTRICTED_LEGENDARIES = Set.of(
            "articuno", "zapdos", "moltres", "raikou", "entei", "suicune", "celebi",
            "regirock", "regice", "registeel", "latias", "latios", "jirachi", "deoxys",
            "uxie", "mesprit", "azelf", "heatran", "regigigas", "cresselia", "phione", "manaphy", "darkrai", "shaymin",
            "victini", "cobalion", "terrakion", "virizion", "tornadus", "thundurus", "landorus", "keldeo", "meloetta", "genesect",
            "diancie", "hoopa", "volcanion", "type_null", "silvally",
            "tapu_koko", "tapu_lele", "tapu_bulu", "tapu_fini", "nihilego", "buzzwole", "pheromosa", "xurkitree", "celesteela", "kartana", "guzzlord", "magearna", "marshadow", "poipole", "naganadel", "stakataka", "blacephalon", "zeraora", "meltan", "melmetal",
            "kubfu", "urshifu", "zarude", "regieleki", "regidrago", "glastrier", "spectrier", "enamorus",
            "wo_chien", "chien_pao", "ting_lu", "chi_yu", "roaring_moon", "iron_valiant", "walking_wake", "iron_leaves", "okidogi", "munkidori", "fezandipiti", "ogerpon", "gouging_fire", "raging_bolt", "iron_boulder", "iron_crown", "pecharunt", "mew"
    );

    private TowerClauseManager() {}

    public static class ValidationResult {
        public final boolean valid;
        public final String errorMessage;
        public final boolean speciesOk;
        public final boolean itemOk;
        public final boolean restrictedOk;
        public final boolean nonRestrictedOk;

        public ValidationResult(boolean valid, String errorMessage, boolean speciesOk, boolean itemOk, boolean restrictedOk, boolean nonRestrictedOk) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.speciesOk = speciesOk;
            this.itemOk = itemOk;
            this.restrictedOk = restrictedOk;
            this.nonRestrictedOk = nonRestrictedOk;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null, true, true, true, true);
        }

        public static ValidationResult fail(String msg, boolean speciesOk, boolean itemOk, boolean restrictedOk, boolean nonRestrictedOk) {
            return new ValidationResult(false, msg, speciesOk, itemOk, restrictedOk, nonRestrictedOk);
        }
    }

    public int getMaxRestricted(int floor) {
        if (floor <= 75) return 1;
        return 2;
    }

    public int getMaxNonRestricted(int floor) {
        if (floor <= 75) return 2;
        return -1; // unlimited
    }

    public boolean isRestricted(String species) {
        if (species == null) return false;
        String clean = species.toLowerCase().replace(" ", "_").replace("-", "_");
        return RESTRICTED_LEGENDARIES.contains(clean);
    }

    public boolean isNonRestricted(String species) {
        if (species == null) return false;
        String clean = species.toLowerCase().replace(" ", "_").replace("-", "_");
        return NON_RESTRICTED_LEGENDARIES.contains(clean);
    }

    /**
     * Validates that the player's team adheres to Species Clause, Item Clause, and Restricted/Non-Restricted Caps.
     */
    public ValidationResult validateTeam(ServerPlayerEntity player, int floor) {
        if (player == null) return ValidationResult.ok();

        try {
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object cobblemonInst = cobblemonClass.getField("INSTANCE").get(null);
            Method getStorageMethod = cobblemonInst.getClass().getMethod("getStorage");
            Object storage = getStorageMethod.invoke(cobblemonInst);
            Method getPartyMethod = storage.getClass().getMethod("getParty", ServerPlayerEntity.class);
            Iterable<?> party = (Iterable<?>) getPartyMethod.invoke(storage, player);

            List<Object> pokemonList = new ArrayList<>();
            for (Object pokemon : party) {
                if (pokemon != null) pokemonList.add(pokemon);
            }
            return validatePokemonList(pokemonList, floor);
        } catch (Exception e) {
            return ValidationResult.ok();
        }
    }

    // Anti-Cheese Banned Sets
    public static final Set<String> BANNED_EVASION_MOVES = Set.of("doubleteam", "minimize");
    public static final Set<String> BANNED_OHKO_MOVES = Set.of("sheercold", "fissure", "horndrill", "guillotine");
    public static final Set<String> BANNED_SWAGGER_MOVES = Set.of("swagger");
    public static final Set<String> BANNED_ABILITIES = Set.of("moody");

    /**
     * Validates a combined list of Pokémon (e.g. 6 Pokémon in Duo team).
     */
    public ValidationResult validatePokemonList(List<Object> pokemonList, int floor) {
        Set<String> speciesSet = new HashSet<>();
        Set<String> itemSet = new HashSet<>();
        int restrictedCount = 0;
        int nonRestrictedCount = 0;

        int maxRestricted = getMaxRestricted(floor);
        int maxNonRestricted = getMaxNonRestricted(floor);

        for (Object pokemon : pokemonList) {
            if (pokemon == null) continue;

            try {
                // Check Species / Dex Number
                Method getSpeciesMethod = pokemon.getClass().getMethod("getSpecies");
                Object speciesObj = getSpeciesMethod.invoke(pokemon);
                Method getNameMethod = speciesObj.getClass().getMethod("getName");
                String speciesName = ((String) getNameMethod.invoke(speciesObj)).toLowerCase();

                // Species Clause (Checks Dex name base)
                String baseSpecies = speciesName.split("-")[0].replace(" ", "_");
                if (!speciesSet.add(baseSpecies)) {
                    return ValidationResult.fail("§c[Species Clause] Duplicate Pokémon: §e" + baseSpecies.toUpperCase(), false, true, true, true);
                }

                // Anti-Cheese: Ability Check (Moody Clause)
                try {
                    Method getAbilityMethod = pokemon.getClass().getMethod("getAbility");
                    Object abilityObj = getAbilityMethod.invoke(pokemon);
                    if (abilityObj != null) {
                        Method getAbNameMethod = abilityObj.getClass().getMethod("getName");
                        String abName = ((String) getAbNameMethod.invoke(abilityObj)).toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
                        if (BANNED_ABILITIES.contains(abName)) {
                            return ValidationResult.fail("§c[Moody Clause] Banned ability detected: §e" + abName.toUpperCase() + " §con " + baseSpecies, true, true, true, true);
                        }
                    }
                } catch (Exception ignored) {}

                // Anti-Cheese: Moveset Check (Evasion, OHKO, Swagger Clauses)
                try {
                    Method getMoveSetMethod = pokemon.getClass().getMethod("getMoveSet");
                    Object moveSet = getMoveSetMethod.invoke(pokemon);
                    if (moveSet instanceof Iterable<?> moves) {
                        for (Object move : moves) {
                            if (move == null) continue;
                            Method getMoveNameMethod = move.getClass().getMethod("getName");
                            String moveName = ((String) getMoveNameMethod.invoke(move)).toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
                            if (BANNED_EVASION_MOVES.contains(moveName)) {
                                return ValidationResult.fail("§c[Evasion Clause] Banned move detected: §e" + moveName.toUpperCase() + " §con " + baseSpecies, true, true, true, true);
                            }
                            if (BANNED_OHKO_MOVES.contains(moveName)) {
                                return ValidationResult.fail("§c[OHKO Clause] Banned OHKO move detected: §e" + moveName.toUpperCase() + " §con " + baseSpecies, true, true, true, true);
                            }
                            if (BANNED_SWAGGER_MOVES.contains(moveName)) {
                                return ValidationResult.fail("§c[Swagger Clause] Banned move detected: §eSWAGGER §con " + baseSpecies, true, true, true, true);
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // Check Restricted Cap
                if (isRestricted(speciesName) || isRestricted(baseSpecies)) {
                    restrictedCount++;
                    if (restrictedCount > maxRestricted) {
                        return ValidationResult.fail("§c[Restricted Cap] Max " + maxRestricted + " Restricted Legendary allowed for Floor " + floor + "! Found: §e" + restrictedCount, true, true, false, true);
                    }
                } else if (isNonRestricted(speciesName) || isNonRestricted(baseSpecies)) {
                    nonRestrictedCount++;
                    if (maxNonRestricted != -1 && nonRestrictedCount > maxNonRestricted) {
                        return ValidationResult.fail("§c[Legendary Cap] Max " + maxNonRestricted + " Sub-Legendaries allowed for Floor " + floor + "! Found: §e" + nonRestrictedCount, true, true, true, false);
                    }
                }

                // Check Held Item Clause
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
                            return ValidationResult.fail("§c[Item Clause] Duplicate held item across team: §e" + itemName, true, false, true, true);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return ValidationResult.ok();
    }
}
