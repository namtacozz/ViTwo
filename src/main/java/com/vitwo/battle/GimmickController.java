package com.vitwo.battle;

import java.util.List;
import java.util.Random;

public class GimmickController {
    private static final Random RANDOM = new Random();

    public enum GimmickType {
        NONE,
        TERA_AND_ZMOVE,
        MEGA_AND_DYNAMAX,
        ALL_GIMMICKS
    }

    public static final List<String> TERA_TYPES = List.of(
            "normal", "fire", "water", "grass", "electric", "ice",
            "fighting", "poison", "ground", "flying", "psychic", "bug",
            "rock", "ghost", "dragon", "steel", "dark", "fairy", "stellar"
    );

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

    public static String getAdaptiveTeraType(String opponentPrimaryType) {
        if (opponentPrimaryType == null) return "stellar";
        // Defensive & Offensive Counter-Typing
        return switch (opponentPrimaryType.toLowerCase()) {
            case "water" -> "electric";
            case "fire" -> "water";
            case "grass" -> "fire";
            case "electric" -> "ground";
            case "dragon" -> "fairy";
            case "ghost" -> "normal";
            case "psychic" -> "dark";
            case "steel" -> "fire";
            case "fairy" -> "steel";
            case "fighting" -> "fairy";
            case "ground" -> "grass";
            case "rock" -> "water";
            case "flying" -> "electric";
            case "bug" -> "fire";
            case "poison" -> "ground";
            case "ice" -> "fire";
            case "dark" -> "fighting";
            case "normal" -> "ghost";
            default -> TERA_TYPES.get(RANDOM.nextInt(TERA_TYPES.size()));
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
