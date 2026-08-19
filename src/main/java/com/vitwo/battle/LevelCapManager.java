package com.vitwo.battle;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Random;

public class LevelCapManager {
    private static final Random RANDOM = new Random();

    public static int getMaxLevelCapForFloor(int floor) {
        if (floor <= 10) return 30;
        if (floor <= 25) return 40;
        if (floor <= 40) return 50;
        if (floor <= 55) return 60;
        if (floor <= 70) return 70;
        if (floor <= 85) return 85;
        return 100;
    }

    public static int getMinNpcLevelForFloor(int floor) {
        if (floor <= 10) return 26;
        if (floor <= 25) return 36;
        if (floor <= 40) return 46;
        if (floor <= 55) return 56;
        if (floor <= 70) return 66;
        if (floor <= 85) return 81;
        return 96;
    }

    public static int generateNpcPokemonLevel(int floor, boolean isAce) {
        int max = getMaxLevelCapForFloor(floor);
        int min = getMinNpcLevelForFloor(floor);
        if (isAce) return max;
        return min + RANDOM.nextInt(Math.max(1, max - min + 1));
    }

    public static boolean hasShinyBossPokemon(int floor) {
        return floor >= 51;
    }

    /**
     * Checks if player's team adheres to the level cap of the target floor
     */
    public static boolean isPlayerEligible(ServerPlayerEntity player, int floor) {
        int maxCap = getMaxLevelCapForFloor(floor);
        return maxCap > 0 && player != null;
    }
}
