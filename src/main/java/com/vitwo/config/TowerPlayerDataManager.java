package com.vitwo.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TowerPlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final TowerPlayerDataManager INSTANCE = new TowerPlayerDataManager();
    public static TowerPlayerDataManager getInstance() { return INSTANCE; }

    private final Map<UUID, PlayerProfile> cache = new HashMap<>();

    public static class PlayerProfile {
        public int battlePoints = 0;
        public int soloCheckpoint = 1;
        public int duoCheckpoint = 1;
        public int highestFloorTrueRun = 0;
        public int totalRunsAttempted = 0;
        public int totalRunsCompleted = 0;
        public int bestTimeSeconds = 0;
        public int bestTurnsTotal = 0;

        // v1.2 Economy & Progression
        public int weeklyCheckpointRuns = 0;
        public long lastWeeklyResetTimestamp = System.currentTimeMillis();
        public int prestigeLevel = 0;
        public Map<String, Integer> weeklyPurchasedStock = new HashMap<>();
    }

    private TowerPlayerDataManager() {}

    private void checkWeeklyReset(PlayerProfile profile) {
        if (profile == null) return;
        long now = System.currentTimeMillis();
        long weekMillis = 7L * 24L * 60L * 60L * 1000L;
        if (now - profile.lastWeeklyResetTimestamp >= weekMillis) {
            profile.weeklyCheckpointRuns = 0;
            profile.weeklyPurchasedStock.clear();
            profile.lastWeeklyResetTimestamp = now;
        }
    }

    private File getPlayerFile(UUID uuid) {
        Path baseDir = FabricLoader.getInstance().getGameDir().resolve("cobbletower_data").resolve("players");
        File dir = baseDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return baseDir.resolve(uuid.toString() + ".json").toFile();
    }

    public synchronized PlayerProfile getProfile(UUID uuid) {
        if (cache.containsKey(uuid)) {
            PlayerProfile p = cache.get(uuid);
            checkWeeklyReset(p);
            return p;
        }

        File file = getPlayerFile(uuid);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                PlayerProfile profile = GSON.fromJson(reader, PlayerProfile.class);
                if (profile != null) {
                    if (profile.weeklyPurchasedStock == null) {
                        profile.weeklyPurchasedStock = new HashMap<>();
                    }
                    checkWeeklyReset(profile);
                    cache.put(uuid, profile);
                    return profile;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        PlayerProfile newProfile = new PlayerProfile();
        cache.put(uuid, newProfile);
        saveProfile(uuid);
        return newProfile;
    }

    public synchronized void saveProfile(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) return;

        File file = getPlayerFile(uuid);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(profile, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void addBp(UUID uuid, int amount) {
        PlayerProfile profile = getProfile(uuid);
        profile.battlePoints = Math.max(0, profile.battlePoints + amount);
        saveProfile(uuid);
    }

    public synchronized boolean spendBp(UUID uuid, int amount) {
        PlayerProfile profile = getProfile(uuid);
        if (profile.battlePoints >= amount) {
            profile.battlePoints -= amount;
            saveProfile(uuid);
            return true;
        }
        return false;
    }

    public synchronized int getBp(UUID uuid) {
        return getProfile(uuid).battlePoints;
    }

    public synchronized void updateSoloCheckpoint(UUID uuid, int floor) {
        PlayerProfile profile = getProfile(uuid);
        if (floor > profile.soloCheckpoint) {
            profile.soloCheckpoint = floor;
            saveProfile(uuid);
        }
    }

    public synchronized void updateDuoCheckpoint(UUID uuid, int floor) {
        PlayerProfile profile = getProfile(uuid);
        if (floor > profile.duoCheckpoint) {
            profile.duoCheckpoint = floor;
            saveProfile(uuid);
        }
    }

    public synchronized void recordRunResult(UUID uuid, int floor, boolean isTrueRun, int turns, int timeSec, boolean completed) {
        PlayerProfile profile = getProfile(uuid);
        profile.totalRunsAttempted++;
        if (completed) {
            profile.totalRunsCompleted++;
        }
        if (isTrueRun && floor > profile.highestFloorTrueRun) {
            profile.highestFloorTrueRun = floor;
        }
        if (completed && (profile.bestTurnsTotal == 0 || turns < profile.bestTurnsTotal)) {
            profile.bestTurnsTotal = turns;
        }
        if (completed && (profile.bestTimeSeconds == 0 || timeSec < profile.bestTimeSeconds)) {
            profile.bestTimeSeconds = timeSec;
        }
        saveProfile(uuid);
    }

    public synchronized float getCheckpointBpMultiplier(UUID uuid) {
        PlayerProfile profile = getProfile(uuid);
        int runs = profile.weeklyCheckpointRuns;
        if (runs <= 3) return 0.50f;
        if (runs <= 8) return 0.30f;
        if (runs <= 15) return 0.15f;
        return 0.05f;
    }

    public synchronized void incrementWeeklyCheckpointRuns(UUID uuid) {
        PlayerProfile profile = getProfile(uuid);
        profile.weeklyCheckpointRuns++;
        saveProfile(uuid);
    }

    public synchronized int getWeeklyPurchasedStock(UUID uuid, String itemId) {
        PlayerProfile profile = getProfile(uuid);
        return profile.weeklyPurchasedStock.getOrDefault(itemId, 0);
    }

    public synchronized void recordStockPurchase(UUID uuid, String itemId, int amount) {
        PlayerProfile profile = getProfile(uuid);
        profile.weeklyPurchasedStock.put(itemId, getWeeklyPurchasedStock(uuid, itemId) + amount);
        saveProfile(uuid);
    }

    public synchronized int getPrestigeLevel(UUID uuid) {
        return getProfile(uuid).prestigeLevel;
    }

    public synchronized void addPrestige(UUID uuid) {
        PlayerProfile profile = getProfile(uuid);
        if (profile.prestigeLevel < 10) {
            profile.prestigeLevel++;
            saveProfile(uuid);
        }
    }

    public synchronized float getPrestigeBpMultiplier(UUID uuid) {
        int prestige = getPrestigeLevel(uuid);
        return 1.0f + (prestige * 0.05f);
    }
}
