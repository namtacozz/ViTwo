package com.vitwo.arena;

import java.util.List;
import java.util.Random;

public class ArenaTemplatePool {
    private static final List<String> ARENA_TEMPLATES = List.of(
            "vitwo:dark_marshlight_tavern",
            "vitwo:dark_haunted_church",
            "vitwo:dark_ravenspire_manor",
            "vitwo:default_academy_gym",
            "vitwo:distortion_origin_shrine",
            "vitwo:legendary_ruins_arena",
            "vitwo:celestial_tower_apex"
    );

    private static final Random RANDOM = new Random();

    public static String getRandomArena(int floor) {
        if (floor == 100) {
            return "vitwo:celestial_tower_apex";
        }
        int index = RANDOM.nextInt(ARENA_TEMPLATES.size() - 1);
        return ARENA_TEMPLATES.get(index);
    }
}
