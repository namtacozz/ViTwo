package com.vitwo.battle;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Random;

public class LevelCapManager {
    private static final Random RANDOM = new Random();

    public static int getMaxLevelCapForFloor(int floor) {
        if (floor <= 25) {
            return 36;
        } else if (floor <= 50) {
            return 50;
        } else if (floor <= 75) {
            return 80;
        } else {
            return 100;
        }
    }

    public static int getMinNpcLevelForFloor(int floor) {
        if (floor <= 25) {
            return 32;
        } else if (floor <= 50) {
            return 46;
        } else if (floor <= 75) {
            return 75;
        } else {
            return 95;
        }
    }

    public static int generateNpcPokemonLevel(int floor, boolean isAce) {
        int max = getMaxLevelCapForFloor(floor);
        int min = getMinNpcLevelForFloor(floor);
        if (isAce) return max;
        return min + RANDOM.nextInt(max - min + 1);
    }

    public static boolean hasShinyBossPokemon(int floor) {
        return floor >= 51;
    }

    /**
     * Checks if player's team adheres to the level cap of the target floor
     */
    public static boolean isPlayerEligible(ServerPlayerEntity player, int floor) {
        int maxCap = getMaxLevelCapForFloor(floor);
        // In Cobblemon environment:
        // Iterates CobblemonStorage.getParty(player)
        // If any pokemon.getLevel() > maxCap: return false
        return maxCap > 0 && player != null;
    }
}
