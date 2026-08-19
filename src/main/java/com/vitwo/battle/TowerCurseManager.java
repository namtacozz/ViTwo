package com.vitwo.battle;

import java.util.List;
import java.util.Set;

public class TowerCurseManager {
    private static final TowerCurseManager INSTANCE = new TowerCurseManager();
    public static TowerCurseManager getInstance() { return INSTANCE; }

    public enum TowerCurse {
        NONE("No Active Curse", "§7Arena is free of atmospheric curses.", "§7None"),
        IRONCLAD("Curse of the Ironclad", "§8Opponent Pokémon receive 15% reduced damage from all sources.", "§8🛡 Ironclad (-15% Dmg)"),
        INERTIA("Curse of Inertia", "§6Pure stat-boosting moves are strictly sealed.", "§6⚡ Inertia (No Stat Buffs)"),
        FATIGUE("Curse of Fatigue", "§cMoves with 8+ base PP consume +1 extra PP per execution.", "§c⏳ Fatigue (+1 PP Cost)"),
        FOG("Curse of the Fog", "§eDense ancient mist reduces accuracy of all moves by 10%.", "§e🌫 Fog (-10% Accuracy)"),
        TEMPORAL_DECAY("Curse of Temporal Decay", "§eProtective moves decay faster: 50% → 12.5% → 0%.", "§e⌛ Temporal Decay"),
        RETALIATION("Curse of Retaliation", "§410% of direct damage inflicted is reflected as recoil (capped at 20% Max HP).", "§4🩸 Retaliation (10% Recoil)"),
        SILENCE("Curse of Silence", "§dSound-based moves are disabled entirely.", "§d🔇 Silence (No Sound Moves)"),
        GRAVITY_WELL("Curse of the Gravity Well", "§5Ground immunities disabled; all moves gain +20% accuracy.", "§5🌑 Gravity Well"),
        FAMINE("Curse of Famine", "§6Active healing and recovery effects are reduced by 50%.", "§6🍂 Famine (-50% Heal)");

        public final String displayName;
        public final String description;
        public final String hudBadge;

        TowerCurse(String displayName, String description, String hudBadge) {
            this.displayName = displayName;
            this.description = description;
            this.hudBadge = hudBadge;
        }
    }

    public static final List<TowerCurse> CURSE_POOL = List.of(
            TowerCurse.IRONCLAD,
            TowerCurse.INERTIA,
            TowerCurse.FATIGUE,
            TowerCurse.FOG,
            TowerCurse.TEMPORAL_DECAY,
            TowerCurse.RETALIATION,
            TowerCurse.SILENCE,
            TowerCurse.GRAVITY_WELL,
            TowerCurse.FAMINE
    );

    public static List<TowerCurse> getCursePool() {
        return CURSE_POOL;
    }

    // Moves banned by Curse of Inertia (pure stat-boosting moves)
    public static final Set<String> INERTIA_BANNED_MOVES = Set.of(
            "swordsdance", "dragondance", "nastyplot", "calmmind", "quiverdance",
            "shellsmash", "bellydrum", "coil", "irondefense", "amnesia",
            "bulkup", "cosmicpower", "cottonguard", "shiftgear",
            "geomancy", "coaching", "tailglow", "acidarmor", "barrier",
            "agility", "rockpolish", "autotomize", "honeclaws", "growth"
    );

    // Moves banned by Curse of Silence (sound-based moves)
    public static final Set<String> SOUND_BANNED_MOVES = Set.of(
            "hypervoice", "boomburst", "bugbuzz", "snarl", "perishsong",
            "healbell", "disarmingvoice", "uproar", "partingshot", "overdrive",
            "torchsong", "alluringvoice", "relicsong", "screech", "metalsound",
            "sing", "grasswhistle", "supersonic", "growl", "howl",
            "echoedvoice", "round", "confide", "clangoroussoul", "clangingscales"
    );

    private TowerCurseManager() {}

    /**
     * Determines active floor curses based on the current floor bracket.
     * Floors 1-10: None.
     * Floors 11-25: Curse of the Ironclad (15%).
     * Floors 26-50: Curse of Inertia.
     * Floors 51-75: Curse of Fatigue.
     * Floors 76-90: Curse of the Fog.
     * Floors 91-100: Definitive Dual Curse Matrix (10 strategic pairs).
     */
    public List<TowerCurse> getActiveCursesForFloor(int floor) {
        if (floor <= 10) {
            return List.of();
        }

        if (floor >= 91) {
            switch (floor) {
                case 91: return List.of(TowerCurse.IRONCLAD, TowerCurse.FATIGUE);
                case 92: return List.of(TowerCurse.INERTIA, TowerCurse.RETALIATION);
                case 93: return List.of(TowerCurse.FOG, TowerCurse.GRAVITY_WELL);
                case 94: return List.of(TowerCurse.SILENCE, TowerCurse.FAMINE);
                case 95: return List.of(TowerCurse.FATIGUE, TowerCurse.TEMPORAL_DECAY);
                case 96: return List.of(TowerCurse.IRONCLAD, TowerCurse.SILENCE);
                case 97: return List.of(TowerCurse.RETALIATION, TowerCurse.FAMINE);
                case 98: return List.of(TowerCurse.FOG, TowerCurse.INERTIA);
                case 99: return List.of(TowerCurse.GRAVITY_WELL, TowerCurse.FATIGUE);
                case 100: default: return List.of(TowerCurse.RETALIATION, TowerCurse.TEMPORAL_DECAY);
            }
        }

        if (floor <= 25) return List.of(TowerCurse.IRONCLAD);
        if (floor <= 50) return List.of(TowerCurse.INERTIA);
        if (floor <= 75) return List.of(TowerCurse.FATIGUE);
        return List.of(TowerCurse.FOG);
    }

    public TowerCurse getPrimaryCurseForFloor(int floor) {
        List<TowerCurse> curses = getActiveCursesForFloor(floor);
        return curses.isEmpty() ? TowerCurse.NONE : curses.get(0);
    }

    public String getCurseNotification(int floor) {
        List<TowerCurse> curses = getActiveCursesForFloor(floor);
        if (curses.isEmpty()) return "";

        if (curses.size() == 1) {
            TowerCurse c = curses.get(0);
            return "§4§l⚠ ANCIENT CURSE ACTIVE: " + c.displayName + " §r\n" + c.description;
        }

        StringBuilder sb = new StringBuilder("§4§l⚠ DUAL ANCIENT CURSES ACTIVE (FLOOR " + floor + "): §r\n");
        for (TowerCurse c : curses) {
            sb.append("§c● ").append(c.displayName).append(" §7— ").append(c.description).append("\n");
        }
        return sb.toString().trim();
    }

    public boolean isMoveBannedByInertia(String moveName) {
        if (moveName == null) return false;
        String clean = moveName.toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
        return INERTIA_BANNED_MOVES.contains(clean);
    }

    public boolean isMoveBannedBySilence(String moveName) {
        if (moveName == null) return false;
        String clean = moveName.toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
        return SOUND_BANNED_MOVES.contains(clean);
    }
}
