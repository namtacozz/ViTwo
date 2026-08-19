package com.vitwo.arena;

import java.util.List;
import java.util.Random;

public class ArenaTemplatePool {
    private static final Random RANDOM = new Random();

    // 1. Kanto Gyms (Gen 1)
    public static final List<String> KANTO_GYMS = List.of(
            "cobbleverse:brock",
            "cobbleverse:misty",
            "cobbleverse:ltsurge",
            "cobbleverse:erika",
            "cobbleverse:koga",
            "cobbleverse:sabrina",
            "cobbleverse:blaine",
            "cobbleverse:giovanni"
    );

    // 2. Johto Gyms (Gen 2)
    public static final List<String> JOHTO_GYMS = List.of(
            "cobbleverse:valerio",
            "cobbleverse:raffaello",
            "cobbleverse:chiara",
            "cobbleverse:angelo",
            "cobbleverse:furio",
            "cobbleverse:jasmine",
            "cobbleverse:alfredo",
            "cobbleverse:sandra"
    );

    // 3. Hoenn Gyms (Gen 3)
    public static final List<String> HOENN_GYMS = List.of(
            "cobbleverse:petra",
            "cobbleverse:rudi",
            "cobbleverse:walter",
            "cobbleverse:fiammetta",
            "cobbleverse:norman",
            "cobbleverse:alice",
            "cobbleverse:tell",
            "cobbleverse:adriano"
    );

    // 4. Sinnoh Gyms (Gen 4)
    public static final List<String> SINNOH_GYMS = List.of(
            "cobbleverse:pedro",
            "cobbleverse:gardenia",
            "cobbleverse:marzia",
            "cobbleverse:omar",
            "cobbleverse:fannie",
            "cobbleverse:ferruccio",
            "cobbleverse:bianca",
            "cobbleverse:corrado"
    );

    // 5. Villain Towers & Sacred Towers (Floors 25-49)
    public static final List<String> VILLAIN_AND_ANCIENT_TOWERS = List.of(
            "cobbleverse:team_rocket_tower",
            "cobbleverse:rocket_radio_tower",
            "cobbleverse:team_galactic_hq",
            "cobbleverse:eterna_building",
            "cobbleverse:bell_tower",
            "cobbleverse:burned_tower",
            "cobbleverse:dawn_tower",
            "cobbleverse:dusk_tower"
    );

    // 6. Elite Four & Grand Leagues (Floors 50-74)
    public static final List<String> LEAGUE_TOWERS = List.of(
            "cobbleverse:kanto_league",
            "cobbleverse:johto_league",
            "cobbleverse:hoenn_league",
            "cobbleverse:sinnoh_league"
    );

    // 7. Legendary Shrines & Mythical Dimensions (Floors 75-99)
    public static final List<String> MYTHICAL_SHRINES = List.of(
            "cobbleverse:spear_pillar",
            "cobbleverse:snowpoint_temple",
            "legendarymonuments:distortion_portal",
            "legendarymonuments:giratina_island/main/111",
            "legendarymonuments:stark_mountain",
            "legendarymonuments:eternatus_cocoon",
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

    // 8. Rest Station Pokecenters (Every 5 Floors)
    public static final List<String> POKECENTERS = List.of(
            "cobblemon:village_plains/village_plains_pokecenter",
            "cobblemon:village_desert/village_desert_pokecenter",
            "cobblemon:village_snowy/village_snowy_pokecenter",
            "cobblemon:village_taiga/village_taiga_pokecenter",
            "cobblemon:villages/cherry/pokecenter",
            "cobblemon:villages/dark_forest/pokecenter",
            "cobblemon:villages/mountains/pokecenter"
    );

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

    public static String getStructureForFloor(int floor) {
        if (floor >= 100) {
            return "cobbleverse:spear_pillar";
        }
        if (floor % 5 == 0) {
            return getRandomFromList(POKECENTERS);
        }
        if (floor <= 25) {
            return getRandomFromList(ALL_REGIONAL_GYMS);
        } else if (floor <= 50) {
            return getRandomFromList(VILLAIN_AND_ANCIENT_TOWERS);
        } else if (floor <= 75) {
            return getRandomFromList(LEAGUE_TOWERS);
        } else {
            return getRandomFromList(MYTHICAL_SHRINES);
        }
    }

    private static String getRandomFromList(List<String> list) {
        if (list == null || list.isEmpty()) return "cobbleverse:spear_pillar";
        return list.get(RANDOM.nextInt(list.size()));
    }
}
