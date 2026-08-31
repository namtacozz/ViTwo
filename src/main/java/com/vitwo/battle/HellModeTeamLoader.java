package com.vitwo.battle;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vitwo.mod.ViTwoMod;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HellModeTeamLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(HellModeTeamLoader.class);
    private static final Map<String, JsonObject> TRAINER_JSON_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> TRAINER_ALIASES = new HashMap<>();

    static {
        // Kanto Gym Leaders
        TRAINER_ALIASES.put("brock", "kanto_brock");
        TRAINER_ALIASES.put("misty", "kanto_misty");
        TRAINER_ALIASES.put("ltsurge", "kanto_ltsurge");
        TRAINER_ALIASES.put("lt_surge", "kanto_ltsurge");
        TRAINER_ALIASES.put("lt surge", "kanto_ltsurge");
        TRAINER_ALIASES.put("surge", "kanto_ltsurge");
        TRAINER_ALIASES.put("erika", "kanto_erika");
        TRAINER_ALIASES.put("koga", "kanto_koga");
        TRAINER_ALIASES.put("sabrina", "kanto_sabrina");
        TRAINER_ALIASES.put("blaine", "kanto_blaine");
        TRAINER_ALIASES.put("giovanni", "kanto_giovanni");
        TRAINER_ALIASES.put("blue", "kanto_champion_blue");
        TRAINER_ALIASES.put("red", "trainer_red_0003");

        // Johto Gym Leaders
        TRAINER_ALIASES.put("falkner", "johto_valerio");
        TRAINER_ALIASES.put("valerio", "johto_valerio");
        TRAINER_ALIASES.put("bugsy", "johto_raffaello");
        TRAINER_ALIASES.put("raffaello", "johto_raffaello");
        TRAINER_ALIASES.put("whitney", "johto_chiara");
        TRAINER_ALIASES.put("chiara", "johto_chiara");
        TRAINER_ALIASES.put("morty", "johto_angelo");
        TRAINER_ALIASES.put("angelo", "johto_angelo");
        TRAINER_ALIASES.put("chuck", "johto_furio");
        TRAINER_ALIASES.put("furio", "johto_furio");
        TRAINER_ALIASES.put("jasmine", "johto_jasmine");
        TRAINER_ALIASES.put("pryce", "johto_alfredo");
        TRAINER_ALIASES.put("alfredo", "johto_alfredo");
        TRAINER_ALIASES.put("clair", "johto_sandra");
        TRAINER_ALIASES.put("sandra", "johto_sandra");

        // Hoenn Gym Leaders
        TRAINER_ALIASES.put("roxanne", "hoenn_petra");
        TRAINER_ALIASES.put("petra", "hoenn_petra");
        TRAINER_ALIASES.put("brawly", "hoenn_rudi");
        TRAINER_ALIASES.put("rudi", "hoenn_rudi");
        TRAINER_ALIASES.put("wattson", "hoenn_walter");
        TRAINER_ALIASES.put("walter", "hoenn_walter");
        TRAINER_ALIASES.put("flannery", "hoenn_fiammetta");
        TRAINER_ALIASES.put("fiammetta", "hoenn_fiammetta");
        TRAINER_ALIASES.put("norman", "hoenn_norman");
        TRAINER_ALIASES.put("winona", "hoenn_alice");
        TRAINER_ALIASES.put("tate", "hoenn_tell");
        TRAINER_ALIASES.put("liza", "hoenn_tell");
        TRAINER_ALIASES.put("tell", "hoenn_tell");
        TRAINER_ALIASES.put("wallace", "hoenn_adriano");
        TRAINER_ALIASES.put("adriano", "hoenn_adriano");
        TRAINER_ALIASES.put("steven", "hoenn_champion_rocco");
        TRAINER_ALIASES.put("rocco", "hoenn_champion_rocco");

        // Sinnoh Gym Leaders & Cynthia
        TRAINER_ALIASES.put("roark", "gym_leader_roark_058a");
        TRAINER_ALIASES.put("gardenia", "gym_leader_gardenia_058b");
        TRAINER_ALIASES.put("maylene", "gym_leader_maylene_058d");
        TRAINER_ALIASES.put("wake", "gym_leader_wake_058c");
        TRAINER_ALIASES.put("crasher_wake", "gym_leader_wake_058c");
        TRAINER_ALIASES.put("fantina", "gym_leader_fantina_058e");
        TRAINER_ALIASES.put("byron", "gym_leader_byron_0590");
        TRAINER_ALIASES.put("candice", "gym_leader_candice_058f");
        TRAINER_ALIASES.put("volkner", "gym_leader_volkner_0591");
        TRAINER_ALIASES.put("cynthia", "champion_cynthia_05a7");
        TRAINER_ALIASES.put("camilla", "sinnoh_champion_camilla");
    }

    public static JsonObject getTrainerJson(String trainerId) {
        if (trainerId == null || trainerId.isBlank()) return null;
        String cleanId = trainerId.toLowerCase(Locale.ROOT).trim();

        if (TRAINER_JSON_CACHE.containsKey(cleanId)) {
            return TRAINER_JSON_CACHE.get(cleanId);
        }

        // 1. Direct Alias Lookup
        if (TRAINER_ALIASES.containsKey(cleanId)) {
            String targetKey = TRAINER_ALIASES.get(cleanId);
            JsonObject obj = loadJsonFromResource("/data/rctmod/trainers/" + targetKey + ".json");
            if (obj != null) {
                TRAINER_JSON_CACHE.put(cleanId, obj);
                return obj;
            }
        }

        // 2. Try stripped prefix alias (e.g. "leader_clair_004a" -> "clair" -> "johto_sandra")
        String stripped = cleanId;
        for (String prefix : new String[]{"gym_leader_", "leader_", "champion_", "elite_four_", "boss_", "trainer_"}) {
            if (stripped.startsWith(prefix)) {
                stripped = stripped.substring(prefix.length());
                break;
            }
        }
        if (stripped.contains("_")) {
            int lastUnderscore = stripped.lastIndexOf('_');
            String potentialSuffix = stripped.substring(lastUnderscore + 1);
            if (potentialSuffix.length() <= 4) {
                stripped = stripped.substring(0, lastUnderscore);
            }
        }
        if (TRAINER_ALIASES.containsKey(stripped)) {
            String targetKey = TRAINER_ALIASES.get(stripped);
            JsonObject obj = loadJsonFromResource("/data/rctmod/trainers/" + targetKey + ".json");
            if (obj != null) {
                TRAINER_JSON_CACHE.put(cleanId, obj);
                return obj;
            }
        }

        // 3. Try exact path
        JsonObject obj = loadJsonFromResource("/data/rctmod/trainers/" + cleanId + ".json");
        if (obj == null) {
            // Try common prefix aliases
            for (String pfx : new String[]{"kanto_", "johto_", "hoenn_", "gym_leader_", "leader_", "champion_"}) {
                obj = loadJsonFromResource("/data/rctmod/trainers/" + pfx + cleanId + ".json");
                if (obj != null) break;
            }
        }

        if (obj != null) {
            TRAINER_JSON_CACHE.put(cleanId, obj);
        }
        return obj;
    }

    private static JsonObject loadJsonFromResource(String path) {
        try (InputStream in = ViTwoMod.class.getResourceAsStream(path)) {
            if (in == null) return null;
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public static List<Pokemon> createTeamFromTrainerId(String trainerId, Integer overrideLevel) {
        JsonObject trainerObj = getTrainerJson(trainerId);
        if (trainerObj == null || !trainerObj.has("team")) {
            return Collections.emptyList();
        }

        JsonArray teamArr = trainerObj.getAsJsonArray("team");
        List<Pokemon> result = new ArrayList<>();

        for (JsonElement el : teamArr) {
            if (!el.isJsonObject()) continue;
            JsonObject monObj = el.getAsJsonObject();
            try {
                Pokemon mon = parsePokemonFromJson(monObj, overrideLevel);
                if (mon != null) {
                    result.add(mon);
                }
            } catch (Throwable t) {
                LOGGER.warn("[HellMode] Failed to parse pokemon in trainer {}: {}", trainerId, t.getMessage());
            }
        }

        return result;
    }

    private static Pokemon parsePokemonFromJson(JsonObject obj, Integer overrideLevel) {
        String species = obj.has("species") ? obj.get("species").getAsString().toLowerCase(Locale.ROOT).trim() : "pikachu";
        int level = overrideLevel != null ? overrideLevel : (obj.has("level") ? obj.get("level").getAsInt() : 50);

        StringBuilder propStr = new StringBuilder(species).append(" level=").append(level);

        if (obj.has("gender")) {
            propStr.append(" gender=").append(obj.get("gender").getAsString().toLowerCase(Locale.ROOT));
        }
        if (obj.has("shiny") && obj.get("shiny").getAsBoolean()) {
            propStr.append(" shiny=true");
        }
        if (obj.has("nature")) {
            propStr.append(" nature=").append(obj.get("nature").getAsString().toLowerCase(Locale.ROOT));
        }
        if (obj.has("ability")) {
            propStr.append(" ability=").append(obj.get("ability").getAsString().toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_"));
        }
        if (obj.has("aspects") && obj.get("aspects").isJsonArray()) {
            for (JsonElement asp : obj.getAsJsonArray("aspects")) {
                propStr.append(" ").append(asp.getAsString().toLowerCase(Locale.ROOT));
            }
        }

        Pokemon mon = null;
        try {
            mon = PokemonProperties.Companion.parse(propStr.toString()).create();
        } catch (Throwable ignored) {}

        if (mon == null) {
            mon = PokemonProperties.Companion.parse(species + " level=" + level).create();
        }

        mon.setLevel(level);

        // 1. Held Item (Mega Stones, Z-Crystals, Items)
        if (obj.has("heldItem") && obj.get("heldItem").isJsonArray()) {
            JsonArray items = obj.getAsJsonArray("heldItem");
            if (!items.isEmpty()) {
                String item = items.get(0).getAsString().toLowerCase(Locale.ROOT).trim();
                try {
                    PokemonProperties.Companion.parse("item=" + item).apply(mon);
                } catch (Throwable ignored) {}

                // Robust fallback via ItemStack
                if (mon.heldItem().isEmpty()) {
                    try {
                        Identifier itemId = Identifier.tryParse(item.contains(":") ? item : "cobblemon:" + item);
                        if (itemId != null && Registries.ITEM.containsId(itemId)) {
                            mon.swapHeldItem(new ItemStack(Registries.ITEM.get(itemId)), false, false);
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }

        // 2. Gimmick: Terastallization (Tera Type)
        if (obj.has("teraType") || obj.has("tera_type")) {
            String tera = obj.has("teraType") ? obj.get("teraType").getAsString() : obj.get("tera_type").getAsString();
            try {
                PokemonProperties.Companion.parse("tera_type=" + tera.toLowerCase(Locale.ROOT)).apply(mon);
            } catch (Throwable ignored) {}
        }

        // 3. Gimmick: Dynamax / Gigantamax (Gmax Factor)
        if (obj.has("gmax") && obj.get("gmax").getAsBoolean()) {
            try {
                mon.setGmaxFactor(true);
            } catch (Throwable ignored) {}
        }

        // 4. Moveset
        if (obj.has("moveset") && obj.get("moveset").isJsonArray()) {
            List<Move> customMoves = new ArrayList<>();
            for (JsonElement mEl : obj.getAsJsonArray("moveset")) {
                String cleanMove = mEl.getAsString().toLowerCase(Locale.ROOT).trim().replace(" ", "_").replace("-", "_");
                try {
                    MoveTemplate template = Moves.getByName(cleanMove);
                    if (template == null) {
                        template = Moves.getByName(cleanMove.replace("_", ""));
                    }
                    if (template == null) {
                        template = Moves.getByName(cleanMove.replace("_", "-"));
                    }
                    if (template != null) {
                        customMoves.add(template.create());
                    }
                } catch (Throwable ignored) {}
            }
            if (!customMoves.isEmpty()) {
                mon.getMoveSet().clear();
                for (Move m : customMoves) {
                    mon.getMoveSet().add(m);
                }
            }
        }

        // 5. IVs
        if (obj.has("ivs") && obj.get("ivs").isJsonObject()) {
            JsonObject ivsObj = obj.getAsJsonObject("ivs");
            for (Stat stat : Stats.Companion.getPERMANENT()) {
                String key = getShortStatKey(stat);
                int val = ivsObj.has(key) ? ivsObj.get(key).getAsInt() : (ivsObj.has(stat.getIdentifier().getPath()) ? ivsObj.get(stat.getIdentifier().getPath()).getAsInt() : 31);
                mon.getIvs().set(stat, Math.min(31, Math.max(0, val)));
            }
        } else {
            for (Stat stat : Stats.Companion.getPERMANENT()) {
                mon.getIvs().set(stat, 31);
            }
        }

        // 6. EVs
        if (obj.has("evs") && obj.get("evs").isJsonObject()) {
            JsonObject evsObj = obj.getAsJsonObject("evs");
            for (Stat stat : Stats.Companion.getPERMANENT()) {
                String key = getShortStatKey(stat);
                int val = evsObj.has(key) ? evsObj.get(key).getAsInt() : (evsObj.has(stat.getIdentifier().getPath()) ? evsObj.get(stat.getIdentifier().getPath()).getAsInt() : 0);
                mon.getEvs().set(stat, Math.min(252, Math.max(0, val)));
            }
        }

        mon.heal();
        return mon;
    }

    private static String getShortStatKey(Stat stat) {
        String path = stat.getIdentifier().getPath().toLowerCase(Locale.ROOT);
        if (path.contains("hp")) return "hp";
        if (path.contains("special_attack") || path.contains("spatk")) return "spa";
        if (path.contains("special_defence") || path.contains("spdef")) return "spd";
        if (path.contains("attack")) return "atk";
        if (path.contains("defence")) return "def";
        if (path.contains("speed")) return "spe";
        return path;
    }

    public static boolean applyHellModeTeamToActor(BattleActor actor, String trainerId, Integer overrideLevel) {
        if (actor == null || trainerId == null || trainerId.isBlank()) return false;

        List<Pokemon> newTeam = createTeamFromTrainerId(trainerId, overrideLevel);
        if (newTeam == null || newTeam.isEmpty()) return false;

        List<BattlePokemon> battleTeam = actor.getPokemonList();

        // 1. Mutate existing BattlePokemon IN-PLACE to preserve Showdown references & prevent AI freezes / desyncs
        for (int i = 0; i < battleTeam.size() && i < newTeam.size(); i++) {
            BattlePokemon bp = battleTeam.get(i);
            if (bp == null) continue;
            Pokemon targetMon = bp.getEffectedPokemon() != null ? bp.getEffectedPokemon() : bp.getOriginalPokemon();
            Pokemon sourceMon = newTeam.get(i);
            if (targetMon != null && sourceMon != null) {
                copyPokemonData(sourceMon, targetMon);
            }
            if (bp.getOriginalPokemon() != null && bp.getOriginalPokemon() != targetMon && sourceMon != null) {
                copyPokemonData(sourceMon, bp.getOriginalPokemon());
            }
            bp.setActor(actor);
        }

        // 2. Append additional reserve Pokemon if newTeam is larger (up to 6)
        while (battleTeam.size() < newTeam.size()) {
            int idx = battleTeam.size();
            Pokemon mon = newTeam.get(idx);
            BattlePokemon bp = BattlePokemon.Companion.safeCopyOf(mon);
            bp.setActor(actor);
            battleTeam.add(bp);
        }

        return true;
    }

    public static void copyPokemonData(Pokemon source, Pokemon target) {
        if (source == null || target == null) return;
        try {
            target.setSpecies(source.getSpecies());
            target.setLevel(source.getLevel());
            target.setGender(source.getGender());
            target.setNature(source.getNature());
            target.setShiny(source.getShiny());

            if (source.getAbility() != null) {
                try {
                    target.updateAbility(source.getAbility());
                } catch (Throwable ignored) {}
            }

            try {
                ItemStack itemCopy = (source.heldItem() != null && !source.heldItem().isEmpty()) ? source.heldItem().copy() : ItemStack.EMPTY;
                target.swapHeldItem(itemCopy, false, false);
            } catch (Throwable ignored) {}

            try {
                target.getAspects().clear();
                target.getAspects().addAll(source.getAspects());
                target.updateAspects();
            } catch (Throwable ignored) {}

            try {
                target.setGmaxFactor(source.getGmaxFactor());
            } catch (Throwable ignored) {}

            try {
                if (source.getTeraType() != null) {
                    target.setTeraType(source.getTeraType());
                }
            } catch (Throwable ignored) {}

            if (source.getMoveSet() != null && !source.getMoveSet().getMoves().isEmpty()) {
                target.getMoveSet().clear();
                for (Move m : source.getMoveSet()) {
                    if (m != null) {
                        target.getMoveSet().add(m);
                    }
                }
            }

            for (Stat stat : Stats.Companion.getPERMANENT()) {
                target.getIvs().set(stat, source.getIvs().getOrDefault(stat));
            }

            for (Stat stat : Stats.Companion.getPERMANENT()) {
                target.getEvs().set(stat, source.getEvs().getOrDefault(stat));
            }

            target.heal();
        } catch (Throwable t) {
            LOGGER.warn("[HellMode] Error copying pokemon data: {}", t.getMessage());
        }
    }
}
