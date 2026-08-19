package com.vitwo.battle;

import com.google.gson.Gson;
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

    public static final String[] TRAINER_FIRST_NAMES = {
        "Red", "Blue", "Green", "Lance", "Steven", "Cynthia", "Alder", "Iris", "Diantha", "Leon",
        "Geeta", "Kieran", "Carmine", "Crispin", "Amarys", "Drayton", "Lacey", "Grusha", "Rika", "Poppy",
        "Larry", "Hassel", "Tulip", "Iono", "Kofu", "Brassius", "Katy", "Raihan", "Bea", "Allister",
        "Piers", "Marnie", "Nessa", "Kabu", "Milo", "Gordie", "Melony", "Opal", "Bede", "Klara",
        "Avery", "Peony", "Mustard", "Volkner", "Flint", "Lucian", "Aaron", "Roark", "Gardenia", "Maylene",
        "Crasher Wake", "Fantina", "Byron", "Candice", "Riley", "Cheryl", "Mira", "Buck", "Marley", "Palmer",
        "Thorton", "Dahlia", "Darach", "Argenta", "Anabel", "Brandon", "Spenser", "Greta", "Lucy", "Noland",
        "Tucker", "Norman", "Winona", "Tate", "Liza", "Wallace", "Juan", "Brawly", "Wattson", "Flannery",
        "Roxanne", "Sidney", "Phoebe", "Glacia", "Drake", "Karen", "Will", "Koga", "Bruno", "Clair",
        "Jasmine", "Chuck", "Pryce", "Whitney", "Morty", "Bugsy", "Falkner", "Brock", "Misty", "Lt. Surge",
        "Erika", "Sabrina", "Blaine", "Giovanni", "Lorelei", "Agatha"
    };

    static {
        loadTeamsFromJson();
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
            } else {
                LOGGER.warn("[CobbleTower] tower_teams.json not found in resources! Initializing fallback teams.");
            }
        } catch (Exception e) {
            LOGGER.error("[CobbleTower] Failed to load tower_teams.json", e);
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

        // Sovereign Floors (91-100) have tailored boss team assignments
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

    public static String getRandomTrainerName(int floor) {
        TowerTeam team = getTeamForFloor(floor);
        String name = TRAINER_FIRST_NAMES[Math.abs((floor * 37 + (int)(System.currentTimeMillis() / 60000))) % TRAINER_FIRST_NAMES.length];

        if (floor >= 100) {
            return "§4§lGENESIS ARCEUS §7(Floor 100)";
        }
        if (floor >= 91) {
            String[] mythicalTitles = {
                    "Apex Sovereign Kyogre", "Solar Sovereign Groudon", "Deluge Sovereign Kyogre",
                    "Apocalypse Sovereign Groudon", "Sky Sovereign Mega Rayquaza", "Hero Sovereign Crowned Zacian",
                    "Hearthflame Sovereign Ogerpon", "Leviathan Sovereign Dondozo", "Emperor Sovereign Calyrex Shadow"
            };
            return "§c" + mythicalTitles[(floor - 91) % mythicalTitles.length];
        }

        String title = team != null ? team.getTrainerTitle() : "Tower Trainer";
        String teamName = team != null ? team.getName() : "Challenger";
        return title + " " + name + " §7[§e" + teamName + "§7]";
    }

    public static List<String> generateDynamicTeam(int floor) {
        TowerTeam team = getTeamForFloor(floor);
        if (team != null && team.getPokemon() != null && !team.getPokemon().isEmpty()) {
            return team.getSpeciesList();
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
