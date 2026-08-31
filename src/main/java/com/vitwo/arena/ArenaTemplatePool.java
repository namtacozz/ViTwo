package com.vitwo.arena;

import java.util.List;
import java.util.Random;

public class ArenaTemplatePool {
    private static final Random RANDOM = new Random();

    // 1. Ancient Archaeological Battlegrounds & Open Pillar Arenas (Floors 1-50)
    public static final List<String> ANCIENT_ARENAS = List.of(
            "cobblemon:ruins/toppled_pillars_circle_layout",
            "cobblemon:ruins/toppled_pillars_square_layout",
            "legendarymonuments:outskirt_stand",
            "mega_showdown:archaeological_site/archaeological_site_a",
            "mega_showdown:archaeological_site/archaeological_site_b",
            "mega_showdown:archaeological_site/archaeological_site_c"
    );

    // 2. Grand Pyramids & League Astronomical Arenas (Floors 51-75)
    public static final List<String> GRAND_LEAGUE_PYRAMIDS = List.of(
            "mega_showdown:observatory",
            "mega_showdown:megaroid",
            "mega_showdown:wishing_weald/wishing_weald",
            "repurposed_structures:pyramids/flower_forest_body",
            "repurposed_structures:pyramids/dark_forest_body",
            "repurposed_structures:pyramids/icy_body",
            "repurposed_structures:pyramids/badlands_body"
    );

    // 3. Legendary & Mythical Summon Shrines (Floors 76-99)
    public static final List<String> MYTHICAL_SHRINES = List.of(
            "legendarymonuments:eternatus_cocoon",
            "legendarymonuments:firescourge_shrine",
            "legendarymonuments:grasswither_shrine",
            "legendarymonuments:groundblight_shrine",
            "legendarymonuments:icerend_shrine",
            "lumymon:newmoon_island",
            "mega_showdown:mega_site"
    );

    // 4. Rest Floor Clean Open Platform (Every 5 Floors, except 100)
    public static final List<String> REST_PLATFORMS = List.of(
            "mega_showdown:mega_site",
            "cobblemon:ruins/toppled_pillars_circle_layout"
    );

    public static String getStructureForFloor(int floor) {
        if (floor >= 100) {
            return "lumymon:temple_of_sinnoh"; // Authentic Spear Pillar Arceus Temple
        }
        if (floor <= 50) {
            return getRandomFromList(ANCIENT_ARENAS);
        } else if (floor <= 75) {
            return getRandomFromList(GRAND_LEAGUE_PYRAMIDS);
        } else {
            return getRandomFromList(MYTHICAL_SHRINES);
        }
    }

    private static String getRandomFromList(List<String> list) {
        if (list == null || list.isEmpty()) return "lumymon:temple_of_sinnoh";
        return list.get(RANDOM.nextInt(list.size()));
    }
}
