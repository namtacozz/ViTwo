package com.vitwo.battle;

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

    // Players have complete freedom to use whatever gimmicks they own
    public static boolean canPlayerUseGimmick() {
        return true;
    }

    public static String getFloorBattleRulesDescription(int floor) {
        int maxCap = LevelCapManager.getMaxLevelCapForFloor(floor);
        GimmickType type = getNpcGimmickForFloor(floor);
        String gimmickStr = switch (type) {
            case NONE -> "§7Không Gimmick";
            case TERA_AND_ZMOVE -> "§dTera §7& §bZ-Moves";
            case MEGA_AND_DYNAMAX -> "§cMega §7& §4Dynamax";
            case ALL_GIMMICKS -> "§6§lFULL GIMMICKS (Mega+Z+Dyna+Tera)";
        };
        return "§e[Quy Tắc: Max Lv." + maxCap + " §7| §cBoss Gimmick: " + gimmickStr + "§e]";
    }
}
