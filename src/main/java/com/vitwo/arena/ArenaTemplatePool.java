package com.vitwo.arena;

import java.util.List;
import java.util.Random;

public class ArenaTemplatePool {
    private static final Random RANDOM = new Random();

    // 1. All Regional Gyms (Gen 1-4) - Clean, Flat Arenas
    public static final List<String> ALL_REGIONAL_GYMS = List.of(
            "cobbleverse:brock", "cobbleverse:misty", "cobbleverse:ltsurge", "cobbleverse:erika",
            "cobbleverse:koga", "cobbleverse:sabrina", "cobbleverse:blaine", "cobbleverse:giovanni",
            "cobbleverse:valerio", "cobbleverse:raffaello", "cobbleverse:chiara", "cobbleverse:angelo",
            "cobbleverse:furio", "cobbleverse:jasmine", "cobbleverse:alfredo", "cobbleverse:sandra",
            "cobbleverse:petra", "cobbleverse:rudi", "cobbleverse:walter", "cobbleverse:fiammetta",
            "cobbleverse:norman", "cobbleverse:alice", "cobbleverse:tell", "cobbleverse:adriano",
            "cobbleverse:pedro", "cobbleverse:gardenia", "cobbleverse:marzia", "cobbleverse:omar",
            "cobbleverse:fannie", "cobbleverse:ferruccio", "cobbleverse:bianca", "cobbleverse:corrado"
    );

    // 2. Elite Four & Grand League Stadiums (Floors 51-75)
    public static final List<String> LEAGUE_STADIUMS = List.of(
            "cobbleverse:kanto_league",
            "cobbleverse:johto_league",
            "cobbleverse:hoenn_league",
            "cobbleverse:sinnoh_league"
    );

    // 3. Legendary & Mythical Shrines (Floors 76-99) - Clean open platforms without dimension portals
    public static final List<String> MYTHICAL_SHRINES = List.of(
            "cobbleverse:spear_pillar",
            "cobbleverse:snowpoint_temple",
            "cobbleverse:legendary/groudon",
            "cobbleverse:legendary/kyogre",
            "cobbleverse:legendary/articuno",
            "cobbleverse:legendary/zapdos",
            "cobbleverse:legendary/moltres",
            "cobbleverse:mythical/mew",
            "cobbleverse:celebi_shrine",
            "cobbleverse:mythical/deoxys",
            "cobbleverse:mythical/jirachi",
            "cobbleverse:mythical/manaphy"
    );

    // 4. Rest Station Pokecenters (Every 5 Floors, except 100)
    public static final List<String> POKECENTERS = List.of(
            "cobblemon:village_plains/village_plains_pokecenter",
            "cobblemon:village_desert/village_desert_pokecenter",
            "cobblemon:village_snowy/village_snowy_pokecenter",
            "cobblemon:village_taiga/village_taiga_pokecenter",
            "cobblemon:villages/cherry/pokecenter",
            "cobblemon:villages/dark_forest/pokecenter",
            "cobblemon:villages/mountains/pokecenter"
    );

    public static String getStructureForFloor(int floor) {
        if (floor >= 100) {
            return "cobbleverse:spear_pillar"; // Final Sovereign Boss Arena
        }
        if (floor % 5 == 0) {
            return getRandomFromList(POKECENTERS);
        }
        if (floor <= 50) {
            return getRandomFromList(ALL_REGIONAL_GYMS);
        } else if (floor <= 75) {
            return getRandomFromList(LEAGUE_STADIUMS);
        } else {
            return getRandomFromList(MYTHICAL_SHRINES);
        }
    }

    private static String getRandomFromList(List<String> list) {
        if (list == null || list.isEmpty()) return "cobbleverse:spear_pillar";
        return list.get(RANDOM.nextInt(list.size()));
    }
}
