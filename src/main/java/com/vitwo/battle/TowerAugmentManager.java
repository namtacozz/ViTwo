package com.vitwo.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TowerAugmentManager {
    private static final TowerAugmentManager INSTANCE = new TowerAugmentManager();
    public static TowerAugmentManager getInstance() { return INSTANCE; }

    public enum TowerAugment {
        SECOND_WIND("Ngọn Gió Thứ Hai", "§aHồi phục 10% Max HP cho Pokémon khi hạ gục một đối thủ.", "§a🌬 Second Wind"),
        RESOLUTE_HEART("Trái Tim Kiên Cường", "§bGiảm 15% sát thương nhận vào từ các chiêu thức Siêu Hiệu Quả.", "§b🛡 Resolute Heart"),
        ELEMENTAL_MASTERY("Thấu Suốt Nguyên Tố", "§6Tăng 12% uy lực cho các chiêu thức trùng hệ (STAB).", "§6⚡ Elemental Mastery"),
        TACTICAL_SURGE("Đột Kích Chiến Thuật", "§eKhi HP dưới 50%, tăng 1 bậc Tốc độ (Speed +1).", "§e⚔ Tactical Surge"),
        IRON_WILL("Ý Chí Sắt Đá", "§dMiễn nhiễm với các hiệu ứng Đóng Băng và Tê Liệt.", "§d✨ Iron Will"),
        VITAL_SIPHON("Hấp Thụ Sinh Lực", "§5Hồi phục lại 10% lượng sát thương trực tiếp gây ra.", "§5🩸 Vital Siphon");

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

