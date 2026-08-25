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

    public static JsonObject getTrainerJson(String trainerId) {
        if (trainerId == null || trainerId.isBlank()) return null;
        String cleanId = trainerId.toLowerCase(Locale.ROOT).trim();

        if (TRAINER_JSON_CACHE.containsKey(cleanId)) {
            return TRAINER_JSON_CACHE.get(cleanId);
        }

        // Try exact path
        JsonObject obj = loadJsonFromResource("/data/rctmod/trainers/" + cleanId + ".json");
        if (obj == null) {
            // Try common prefix aliases (e.g. "brock" -> "kanto_brock", "misty" -> "kanto_misty")
            obj = loadJsonFromResource("/data/rctmod/trainers/kanto_" + cleanId + ".json");
        }
        if (obj == null && cleanId.contains("_")) {
            // Try matching in catalog
            for (String known : new String[]{"kanto_" + cleanId, "leader_" + cleanId, "champion_" + cleanId}) {
                obj = loadJsonFromResource("/data/rctmod/trainers/" + known + ".json");
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

        // Held Item
        if (obj.has("heldItem") && obj.get("heldItem").isJsonArray()) {
            JsonArray items = obj.getAsJsonArray("heldItem");
            if (!items.isEmpty()) {
                String item = items.get(0).getAsString().toLowerCase(Locale.ROOT).trim();
                try {
                    PokemonProperties.Companion.parse("item=" + item).apply(mon);
                } catch (Throwable ignored) {}
            }
        }

        // Moveset
        if (obj.has("moveset") && obj.get("moveset").isJsonArray()) {
            List<Move> customMoves = new ArrayList<>();
            for (JsonElement mEl : obj.getAsJsonArray("moveset")) {
                String cleanMove = mEl.getAsString().toLowerCase(Locale.ROOT).trim().replace(" ", "_").replace("-", "_");
                try {
                    MoveTemplate template = Moves.getByName(cleanMove);
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

        // IVs
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

        // EVs
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
        battleTeam.clear();

        for (Pokemon mon : newTeam) {
            BattlePokemon bp = BattlePokemon.Companion.safeCopyOf(mon);
            bp.setActor(actor);
            battleTeam.add(bp);
        }

        return true;
    }
}
