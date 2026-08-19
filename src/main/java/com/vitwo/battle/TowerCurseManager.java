package com.vitwo.battle;

import java.util.List;

public class TowerCurseManager {
    private static final TowerCurseManager INSTANCE = new TowerCurseManager();
    public static TowerCurseManager getInstance() { return INSTANCE; }

    public enum TowerCurse {
        NONE("No Active Curse", "§7Arena is free of atmospheric curses.", "§7None"),
        IRONCLAD("Curse of the Ironclad", "§8Opponent Pokémon receive 20% reduced damage.", "§8🛡 Ironclad (-20% Dmg)"),
        INERTIA("Curse of Inertia", "§6Stat-boosting moves are strictly sealed.", "§6⚡ Inertia (No Stat Buffs)"),
        FATIGUE("Curse of Fatigue", "§cAll moves consume +1 extra PP per execution.", "§c⏳ Fatigue (+1 PP Cost)"),
        FOG("Curse of the Fog", "§eDense mist reduces accuracy of all moves by 15%.", "§e🌫 Fog (-15% Accuracy)"),
        RETALIATION("Curse of Retaliation", "§415% of direct damage inflicted is reflected back as recoil.", "§4🩸 Retaliation (15% Recoil)");

        public final String displayName;
        public final String description;
        public final String hudBadge;

        TowerCurse(String displayName, String description, String hudBadge) {
            this.displayName = displayName;
            this.description = description;
            this.hudBadge = hudBadge;
        }
    }

    private static final List<TowerCurse> CURSE_POOL = List.of(
            TowerCurse.IRONCLAD,
            TowerCurse.INERTIA,
            TowerCurse.FATIGUE,
            TowerCurse.FOG,
            TowerCurse.RETALIATION
    );

    private TowerCurseManager() {}

    /**
     * Determines the active floor curse based on the current floor bracket.
     * Floors 1-10: None.
     * Every subsequent 10-floor bracket activates a consistent, seeded curse.
     */
    public TowerCurse getCurseForFloor(int floor) {
        if (floor <= 10) {
            return TowerCurse.NONE;
        }
        int bracket = floor / 10;
        int idx = (bracket * 7 + 13) % CURSE_POOL.size();
        return CURSE_POOL.get(idx);
    }

    public String getCurseNotification(int floor) {
        TowerCurse curse = getCurseForFloor(floor);
        if (curse == TowerCurse.NONE) return "";
        return "§4§l⚠ ANCIENT CURSE ACTIVE: " + curse.displayName + " §r\n" + curse.description;
    }
}
