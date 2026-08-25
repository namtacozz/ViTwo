package com.vitwo.battle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TrainerPool {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-TrainerPool");
    private static final Random RANDOM = new Random();
    private static final Gson GSON = new Gson();

    private static final List<TowerTeam> ALL_TEAMS = new ArrayList<>();
    private static final Map<Integer, List<TowerTeam>> STAGE_TEAMS = new HashMap<>();
    private static final Map<String, TowerTeam> TEAM_BY_ID = new HashMap<>();

    // RCT Catalog Data (1,559 trainers)
    private static final Map<Integer, List<String>> RCT_STAGE_TRAINERS = new HashMap<>();
    private static final Map<String, List<String>> RCT_TRAINER_TEAMS = new HashMap<>();
    private static final Map<String, String> RCT_TRAINER_NAMES = new HashMap<>();

    static {
        loadTeamsFromJson();
        loadRctCatalog();
    }

    private static void loadTeamsFromJson() {
        try (InputStream is = TrainerPool.class.getResourceAsStream("/data/vitwo/tower_teams.json")) {
            if (is != null) {
                Type listType = new TypeToken<List<TowerTeam>>(){}.getType();
                List<TowerTeam> loaded = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), listType);
                if (loaded != null && !loaded.isEmpty()) {
                    ALL_TEAMS.addAll(loaded);
                    for (TowerTeam team : loaded) {
                        STAGE_TEAMS.computeIfAbsent(team.getStage(), k -> new ArrayList<>()).add(team);
                        TEAM_BY_ID.put(team.getId(), team);
                    }
                    LOGGER.info("[CobbleTower] Successfully registered {} official teams across {} stages from tower_teams.json",
                            ALL_TEAMS.size(), STAGE_TEAMS.size());
                }
            }
        } catch (Exception e) {
            LOGGER.error("[CobbleTower] Failed to load tower_teams.json", e);
        }
    }

    private static void loadRctCatalog() {
        try (InputStream is = TrainerPool.class.getResourceAsStream("/data/vitwo/rct_trainer_catalog.json")) {
            if (is != null) {
                JsonObject obj = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                if (obj.has("stages")) {
                    JsonObject stagesObj = obj.getAsJsonObject("stages");
                    for (int s = 1; s <= 6; s++) {
                        String key = String.valueOf(s);
                        if (stagesObj.has(key)) {
                            Type listType = new TypeToken<List<String>>(){}.getType();
                            List<String> list = GSON.fromJson(stagesObj.get(key), listType);
                            RCT_STAGE_TRAINERS.put(s, list);
                        }
                    }
                }
                if (obj.has("trainers")) {
                    JsonObject trainersObj = obj.getAsJsonObject("trainers");
                    for (String tid : trainersObj.keySet()) {
                        JsonObject tinfo = trainersObj.getAsJsonObject(tid);
                        String name = tinfo.has("name") ? tinfo.get("name").getAsString() : formatFallbackName(tid);
                        RCT_TRAINER_NAMES.put(tid, name);
                        if (tinfo.has("team")) {
                            Type listType = new TypeToken<List<String>>(){}.getType();
                            List<String> team = GSON.fromJson(tinfo.get("team"), listType);
                            RCT_TRAINER_TEAMS.put(tid, team);
                        }
                    }
                }
                LOGGER.info("[CobbleTower] Successfully registered {} diverse RCT trainers across 6 stages from catalog!",
                        RCT_TRAINER_NAMES.size());
            }
        } catch (Exception e) {
            LOGGER.error("[CobbleTower] Failed to load rct_trainer_catalog.json", e);
        }
    }

    public static List<TowerTeam> getAllTeams() {
        return Collections.unmodifiableList(ALL_TEAMS);
    }

    public static List<TowerTeam> getTeamsForStage(int stage) {
        return STAGE_TEAMS.getOrDefault(stage, Collections.emptyList());
    }

    public static TowerTeam getTeamById(String id) {
        return TEAM_BY_ID.get(id);
    }

    public static int getStageForFloor(int floor) {
        if (floor <= 10) return 1;
        if (floor <= 25) return 2;
        if (floor <= 50) return 3;
        if (floor <= 75) return 4;
        if (floor <= 90) return 5;
        return 6;
    }

    public static TowerTeam getTeamForFloor(int floor) {
        int stage = getStageForFloor(floor);

        if (stage == 6) {
            return switch (floor) {
                case 91 -> getTeamOrFallback("6-01", stage);
                case 92 -> getTeamOrFallback("6-02", stage);
                case 93 -> RANDOM.nextBoolean() ? getTeamOrFallback("6-11", stage) : getTeamOrFallback("6-03", stage);
                case 94 -> RANDOM.nextBoolean() ? getTeamOrFallback("6-12", stage) : getTeamOrFallback("6-04", stage);
                case 95 -> RANDOM.nextBoolean() ? getTeamOrFallback("6-13", stage) : getTeamOrFallback("6-05", stage);
                case 96 -> RANDOM.nextBoolean() ? getTeamOrFallback("6-14", stage) : getTeamOrFallback("6-06", stage);
                case 97 -> getTeamOrFallback("6-07", stage);
                case 98 -> getTeamOrFallback("6-08", stage);
                case 99 -> getTeamOrFallback("6-09", stage);
                case 100 -> RANDOM.nextBoolean() ? getTeamOrFallback("6-10", stage) : getTeamOrFallback("6-15", stage);
                default -> getRandomTeamFromStage(6);
            };
        }

        return getRandomTeamFromStage(stage);
    }

    private static TowerTeam getTeamOrFallback(String id, int fallbackStage) {
        TowerTeam team = TEAM_BY_ID.get(id);
        if (team != null) return team;
        return getRandomTeamFromStage(fallbackStage);
    }

    private static TowerTeam getRandomTeamFromStage(int stage) {
        List<TowerTeam> list = STAGE_TEAMS.get(stage);
        if (list != null && !list.isEmpty()) {
            return list.get(RANDOM.nextInt(list.size()));
        }
        if (!ALL_TEAMS.isEmpty()) {
            return ALL_TEAMS.get(RANDOM.nextInt(ALL_TEAMS.size()));
        }
        return createFallbackTeam(stage);
    }

    /**
     * Obtains a varied and valid RCT Trainer ID for this specific floor
     */
    public static String getRctTrainerIdForFloor(int floor) {
        if (floor >= 100) {
            return "champion_cynthia_05a7"; // Valid Final Boss Avatar in RCT
        }
        int stage = getStageForFloor(floor);
        List<String> list = RCT_STAGE_TRAINERS.get(stage);
        if (list != null && !list.isEmpty()) {
            return list.get(RANDOM.nextInt(list.size()));
        }
        return "youngster_joey_005d";
    }

    /**
     * Returns the authentic human-readable display name corresponding to the NPC avatar
     */
    public static String getTrainerDisplayName(int floor, String rctId) {
        if (floor >= 100) {
            return "§4§lGENESIS ARCEUS";
        }
        if (floor >= 91) {
            String[] mythicalTitles = {
                    "Apex Sovereign Kyogre", "Solar Sovereign Groudon", "Deluge Sovereign Kyogre",
                    "Apocalypse Sovereign Groudon", "Sky Sovereign Mega Rayquaza", "Hero Sovereign Crowned Zacian",
                    "Hearthflame Sovereign Ogerpon", "Leviathan Sovereign Dondozo", "Emperor Sovereign Calyrex Shadow"
            };
            return "§c" + mythicalTitles[(floor - 91) % mythicalTitles.length];
        }

        if (rctId != null) {
            String name = RCT_TRAINER_NAMES.get(rctId);
            if (name != null && !name.isEmpty()) {
                return name;
            }
            return formatFallbackName(rctId);
        }

        return "Tower Challenger";
    }

    public static String getTrainerDisplayName(int floor) {
        return getTrainerDisplayName(floor, getRctTrainerIdForFloor(floor));
    }

    private static String formatFallbackName(String rctId) {
        if (rctId == null || rctId.isEmpty()) return "Tower Challenger";
        String[] parts = rctId.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            // Skip trailing hex ID like '04a5'
            if (i == parts.length - 1 && parts[i].length() <= 4 && parts[i].matches("[0-9a-fA-F]+")) {
                continue;
            }
            if (parts[i].isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                sb.append(parts[i].substring(1).toLowerCase());
            }
        }
        return sb.length() > 0 ? sb.toString() : "Tower Challenger";
    }

    public static List<String> generateDynamicTeam(int floor) {
        TowerTeam team = getTeamForFloor(floor);
        if (team != null && team.getPokemon() != null && !team.getPokemon().isEmpty()) {
            return team.getSpeciesList();
        }

        String rctId = getRctTrainerIdForFloor(floor);
        List<String> rctTeam = RCT_TRAINER_TEAMS.get(rctId);
        if (rctTeam != null && !rctTeam.isEmpty()) {
            return rctTeam;
        }

        return List.of("pikachu", "charizard", "blastoise", "venusaur", "snorlax", "gengar");
    }

    private static TowerTeam createFallbackTeam(int stage) {
        TowerTeam fallback = new TowerTeam();
        fallback.setId(stage + "-01");
        fallback.setStage(stage);
        fallback.setName("Tower Challenger Core");
        fallback.setTrainerTitle("Tower Veteran");
        return fallback;
    }
}
