package com.vitwo.battle;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Random;

public class LevelCapManager {
    private static final Random RANDOM = new Random();

    public static int getMaxLevelCapForFloor(int floor) {
        if (floor <= 10) {
            return 20;
        } else if (floor <= 25) {
            return 35;
        } else if (floor <= 50) {
            return 55;
        } else if (floor <= 75) {
            return 75;
        } else {
            return 100;
        }
    }

    public static int getMinNpcLevelForFloor(int floor) {
        if (floor <= 10) {
            return 14;
        } else if (floor <= 25) {
            return 26;
        } else if (floor <= 50) {
            return 45;
        } else if (floor <= 75) {
            return 68;
        } else {
            return 90;
        }
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
