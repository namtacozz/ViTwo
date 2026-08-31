package com.vitwo.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TowerAugmentManager {
    private static final TowerAugmentManager INSTANCE = new TowerAugmentManager();
    public static TowerAugmentManager getInstance() { return INSTANCE; }

    public enum TowerAugment {
        SECOND_WIND("Second Wind", "§aRestores 10% Max HP when your Pokémon faints an opponent.", "§a🌬 Second Wind"),
        RESOLUTE_HEART("Resolute Heart", "§bTakes 15% reduced damage from Super-Effective moves.", "§b🛡 Resolute Heart"),
        ELEMENTAL_MASTERY("Elemental Mastery", "§6Increases power of Same-Type Attack Bonus (STAB) moves by 12%.", "§6⚡ Elemental Mastery"),
        TACTICAL_SURGE("Tactical Surge", "§eBoosts Speed by +1 stage when Pokémon HP drops below 50%.", "§e⚔ Tactical Surge"),
        IRON_WILL("Iron Will", "§dImmune to Freeze and Paralysis status conditions.", "§d✨ Iron Will"),
        VITAL_SIPHON("Vital Siphon", "§5Drains 10% of all direct damage inflicted as HP recovery.", "§5🩸 Vital Siphon");

        private final String displayName;
        private final String description;
        private final String hudBadge;

        TowerAugment(String displayName, String description, String hudBadge) {
            this.displayName = displayName;
            this.description = description;
            this.hudBadge = hudBadge;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getHudBadge() {
            return hudBadge;
        }
    }

    private TowerAugmentManager() {}

    public static boolean isAugmentFloor(int floor) {
        return floor == 15 || floor == 35 || floor == 65;
    }

    public List<TowerAugment> rollThreeAugments(List<TowerAugment> existingAugments) {
        List<TowerAugment> pool = new ArrayList<>(List.of(TowerAugment.values()));
        if (existingAugments != null) {
            pool.removeAll(existingAugments);
        }
        Collections.shuffle(pool, new Random());
        return pool.subList(0, Math.min(3, pool.size()));
    }
}

