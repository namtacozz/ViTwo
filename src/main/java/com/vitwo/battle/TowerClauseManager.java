package com.vitwo.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

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

    // Anti-Cheese Banned Sets
    public static final Set<String> BANNED_EVASION_MOVES = Set.of("doubleteam", "minimize");
    public static final Set<String> BANNED_OHKO_MOVES = Set.of("sheercold", "fissure", "horndrill", "guillotine");
    public static final Set<String> BANNED_SWAGGER_MOVES = Set.of("swagger");
    public static final Set<String> BANNED_ABILITIES = Set.of("moody");

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
        String clean = species.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
        return RESTRICTED_LEGENDARIES.contains(clean);
    }

    public boolean isNonRestricted(String species) {
        if (species == null) return false;
        String clean = species.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
        return NON_RESTRICTED_LEGENDARIES.contains(clean);
    }

    /**
     * Validates that the player's team adheres to Species Clause, Item Clause, and Restricted/Non-Restricted Caps.
     */
    public ValidationResult validateTeam(ServerPlayerEntity player, int floor) {
        if (player == null) return ValidationResult.ok();

        try {
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (party == null) return ValidationResult.ok();

            List<Pokemon> pokemonList = new ArrayList<>();
            for (Pokemon pokemon : party) {
                if (pokemon != null) pokemonList.add(pokemon);
            }
            return validatePokemonList(pokemonList, floor);
        } catch (Exception e) {
            return ValidationResult.ok();
        }
    }

    /**
     * Validates a combined list of Pokémon (e.g. 6 Pokémon in Duo team).
     */
    public ValidationResult validatePokemonList(List<Pokemon> pokemonList, int floor) {
        Set<String> speciesSet = new HashSet<>();
        Set<String> itemSet = new HashSet<>();
        List<String> foundRestricted = new ArrayList<>();
        List<String> foundNonRestricted = new ArrayList<>();
        int maxRestricted = getMaxRestricted(floor);
        int maxNonRestricted = getMaxNonRestricted(floor);

        for (Pokemon pokemon : pokemonList) {
            if (pokemon == null || pokemon.getSpecies() == null) continue;

            try {
                // 1. Species Clause
                String speciesName = pokemon.getSpecies().getName().toLowerCase(Locale.ROOT);
                String baseSpecies = speciesName.split("-")[0].replace(" ", "_");
                if (!speciesSet.add(baseSpecies)) {
                    return ValidationResult.fail("§c§l[SPECIES CLAUSE] §fDuplicate Pokémon: §e" + capitalize(baseSpecies) + " §7(Only 1 of each species allowed)!", false, true, true, true);
                }

                // 2. Anti-Cheese: Ability Check (Moody Clause)
                if (pokemon.getAbility() != null && pokemon.getAbility().getName() != null) {
                    String abName = pokemon.getAbility().getName().toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
                    if (BANNED_ABILITIES.contains(abName)) {
                        return ValidationResult.fail("§c§l[BANNED ABILITY] §fBanned ability detected: §e" + abName.toUpperCase() + " §7on §e" + capitalize(baseSpecies) + " §7(Moody is banned)!", true, true, true, true);
                    }
                }

                // 3. Anti-Cheese: Moveset Check (Evasion, OHKO, Swagger Clauses)
                if (pokemon.getMoveSet() != null) {
                    for (Move move : pokemon.getMoveSet()) {
                        if (move == null || move.getName() == null) continue;
                        String moveName = move.getName().toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
                        if (BANNED_EVASION_MOVES.contains(moveName)) {
                            return ValidationResult.fail("§c§l[BANNED MOVE] §fBanned Evasion move detected: §e" + moveName.toUpperCase() + " §7on §e" + capitalize(baseSpecies), true, true, true, true);
                        }
                        if (BANNED_OHKO_MOVES.contains(moveName)) {
                            return ValidationResult.fail("§c§l[BANNED MOVE] §fBanned OHKO move detected: §e" + moveName.toUpperCase() + " §7on §e" + capitalize(baseSpecies), true, true, true, true);
                        }
                        if (BANNED_SWAGGER_MOVES.contains(moveName)) {
                            return ValidationResult.fail("§c§l[BANNED MOVE] §fBanned move Swagger detected on §e" + capitalize(baseSpecies), true, true, true, true);
                        }
                    }
                }

                // 4. Collect Restricted / Non-Restricted
                if (isRestricted(speciesName) || isRestricted(baseSpecies)) {
                    foundRestricted.add(capitalize(baseSpecies));
                } else if (isNonRestricted(speciesName) || isNonRestricted(baseSpecies)) {
                    foundNonRestricted.add(capitalize(baseSpecies));
                }

                // 5. Check Held Item Clause
                ItemStack itemStack = pokemon.heldItem();
                if (itemStack != null && !itemStack.isEmpty()) {
                    String itemName = itemStack.getItem().toString();
                    if (!itemSet.add(itemName)) {
                        return ValidationResult.fail("§c§l[ITEM CLAUSE] §fDuplicate held item: §e" + itemStack.getName().getString() + " §7(Only 1 of each item allowed)!", true, false, true, true);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Validate Restricted Legends Cap
        if (foundRestricted.size() > maxRestricted) {
            String foundList = String.join(", ", foundRestricted);
            String msg = "§c§l[RESTRICTED LEGENDARY CLAUSE] §fParty exceeds restricted legendary limit!\n" +
                    "§e► Floor " + floor + " allows a maximum of: §a" + maxRestricted + " Pokémon§e.\n" +
                    "§c► Detected in team (" + foundRestricted.size() + "): §6" + foundList + "\n" +
                    "§7💡 Sub-Legendaries (e.g. Zapdos, Raikou, Latios, Urshifu) are NOT restricted.";
            return ValidationResult.fail(msg, true, true, false, true);
        }

        // Validate Non-Restricted (Sub-Legendaries) Cap
        if (maxNonRestricted != -1 && foundNonRestricted.size() > maxNonRestricted) {
            String foundList = String.join(", ", foundNonRestricted);
            String msg = "§c§l[SUB-LEGENDARY CLAUSE] §fParty exceeds sub-legendary limit!\n" +
                    "§e► Floor " + floor + " allows a maximum of: §a" + maxNonRestricted + " Pokémon§e.\n" +
                    "§c► Detected in team (" + foundNonRestricted.size() + "): §6" + foundList + "\n" +
                    "§7💡 Please swap out some sub-legendaries to continue.";
            return ValidationResult.fail(msg, true, true, true, false);
        }

        return ValidationResult.ok();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }
}
