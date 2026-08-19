package com.vitwo.battle;

import com.vitwo.config.TowerTeraPresetConfig;

import java.util.List;

public class GimmickController {

    public enum GimmickType {
        NONE,
        TERA_AND_ZMOVE,
        MEGA_AND_DYNAMAX,
        ALL_GIMMICKS
    }

    public static GimmickType getNpcGimmickForFloor(int floor) {
        if (floor <= 25) {
            return GimmickType.NONE;
        } else if (floor <= 50) {
            return GimmickType.TERA_AND_ZMOVE;
        } else if (floor <= 75) {
            return GimmickType.MEGA_AND_DYNAMAX;
        } else {
            return GimmickType.ALL_GIMMICKS;
        }
    }

    public static boolean canNpcUseTera(int floor) {
        return floor > 25;
    }

    public static boolean canNpcUseZMove(int floor) {
        return floor > 25;
    }

    public static boolean canNpcUseMega(int floor) {
        return floor > 50;
    }

    public static boolean canNpcUseDynamax(int floor) {
        return floor > 50;
    }

    /**
     * Chooses the optimal Tera type for a given NPC Pokemon species strictly from its 2-3 valid presets.
     */
    public static String getPresetAdaptiveTeraType(String species, String opponentType) {
        List<String> presets = TowerTeraPresetConfig.getTeraPresetsForSpecies(species);
        if (presets.isEmpty()) return "normal";
        if (presets.size() == 1 || opponentType == null) return presets.get(0);

        String opp = opponentType.toLowerCase();

        // Search for a preset that counters the opponent type
        for (String preset : presets) {
            String p = preset.toLowerCase();
            if (isDefensivelyFavorable(p, opp) || isOffensivelyFavorable(p, opp)) {
                return preset;
            }
        }

        // Fallback to primary preset
        return presets.get(0);
    }

    private static boolean isDefensivelyFavorable(String teraType, String oppType) {
        return switch (oppType) {
            case "water" -> teraType.equals("grass") || teraType.equals("dragon") || teraType.equals("water");
            case "fire" -> teraType.equals("water") || teraType.equals("fire") || teraType.equals("dragon") || teraType.equals("rock");
            case "grass" -> teraType.equals("fire") || teraType.equals("steel") || teraType.equals("poison") || teraType.equals("flying");
            case "electric" -> teraType.equals("ground") || teraType.equals("dragon") || teraType.equals("grass");
            case "dragon" -> teraType.equals("fairy") || teraType.equals("steel");
            case "ghost" -> teraType.equals("normal") || teraType.equals("dark");
            case "psychic" -> teraType.equals("dark") || teraType.equals("steel");
            case "fighting" -> teraType.equals("ghost") || teraType.equals("fairy") || teraType.equals("flying") || teraType.equals("poison");
            case "ground" -> teraType.equals("flying") || teraType.equals("grass") || teraType.equals("bug");
            default -> false;
        };
    }

    private static boolean isOffensivelyFavorable(String teraType, String oppType) {
        return switch (oppType) {
            case "water" -> teraType.equals("electric") || teraType.equals("grass");
            case "fire" -> teraType.equals("water") || teraType.equals("ground") || teraType.equals("rock");
            case "grass" -> teraType.equals("fire") || teraType.equals("ice") || teraType.equals("flying");
            case "dragon" -> teraType.equals("fairy") || teraType.equals("ice") || teraType.equals("dragon");
            case "steel" -> teraType.equals("fire") || teraType.equals("fighting") || teraType.equals("ground");
            case "fairy" -> teraType.equals("steel") || teraType.equals("poison");
            default -> false;
        };
    }

    public static String getFloorBattleRulesDescription(int floor) {
        int maxCap = LevelCapManager.getMaxLevelCapForFloor(floor);
        GimmickType type = getNpcGimmickForFloor(floor);
        String gimmickStr = switch (type) {
            case NONE -> "§7Standard";
            case TERA_AND_ZMOVE -> "§dTera §7& §bZ-Moves";
            case MEGA_AND_DYNAMAX -> "§cMega Evolution §7& §4Dynamax";
            case ALL_GIMMICKS -> "§6§lFULL GIMMICKS (Mega + Z + Dyna + Tera)";
        };
        return "§e[Rules: Max Lv." + maxCap + " §7| §cBoss Gimmicks: " + gimmickStr + "§e]";
    }
}
