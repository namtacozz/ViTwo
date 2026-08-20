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

    public synchronized void handleDebugAction(net.minecraft.server.network.ServerPlayerEntity player, String action, int value) {
        if (player == null) return;
        UUID uuid = player.getUuid();
        PlayerProfile profile = getProfile(uuid);

        switch (action) {
            case "UNLOCK_ALL" -> {
                profile.soloCheckpoint = 100;
                profile.duoCheckpoint = 100;
                profile.highestFloorTrueRun = 100;
                saveProfile(uuid);
                com.vitwo.party.TowerPartyManager.getInstance().syncPlayerState(player);
                player.sendMessage(net.minecraft.text.Text.literal("§a§l[Cheat] §fAll Tower checkpoints unlocked to Floor 100!"), false);
            }
            case "ADD_BP" -> {
                addBp(uuid, value > 0 ? value : 5000);
                com.vitwo.party.TowerPartyManager.getInstance().syncPlayerState(player);
                player.sendMessage(net.minecraft.text.Text.literal("§a§l[Cheat] §fGranted §6+" + (value > 0 ? value : 5000) + " BP§f! Current BP: §e" + profile.battlePoints), false);
            }
            case "SET_PRESTIGE" -> {
                profile.prestigeLevel = Math.max(0, Math.min(5, value));
                saveProfile(uuid);
                com.vitwo.party.TowerPartyManager.getInstance().syncPlayerState(player);
                player.sendMessage(net.minecraft.text.Text.literal("§a§l[Cheat] §fPrestige Level set to §b" + profile.prestigeLevel + " ⭐"), false);
            }
            case "TEST_REST" -> {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.vitwo.network.s2c.OpenRestScreenS2CPacket(profile.soloCheckpoint > 1 ? profile.soloCheckpoint : 25));
                player.sendMessage(net.minecraft.text.Text.literal("§a§l[Cheat] §fOpened Rest Station GUI for testing!"), false);
            }
            case "TEST_PREVIEW" -> {
                java.util.List<String> mockTeam = java.util.List.of("mewtwo", "zacian", "kyogre", "flutter_mane", "ogerpon", "kingambit");
                java.util.List<String> playerMock = java.util.List.of("pikachu", "charizard", "garchomp", "lucario", "greninja", "dragapult");
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.vitwo.network.s2c.OpenTeamPreviewS2CPacket(100, 20, "Boss Red", "Sovereign Champion", mockTeam, playerMock));
                player.sendMessage(net.minecraft.text.Text.literal("§a§l[Cheat] §fOpened Team Preview GUI for testing!"), false);
            }
            case "HEAL_PARTY" -> {
                try {
                    Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
                    Object cobblemonInst = cobblemonClass.getField("INSTANCE").get(null);
                    java.lang.reflect.Method getStorageMethod = cobblemonInst.getClass().getMethod("getStorage");
                    Object storage = getStorageMethod.invoke(cobblemonInst);
                    java.lang.reflect.Method getPartyMethod = storage.getClass().getMethod("getParty", net.minecraft.server.network.ServerPlayerEntity.class);
                    Iterable<?> party = (Iterable<?>) getPartyMethod.invoke(storage, player);

                    for (Object pokemon : party) {
                        if (pokemon == null) continue;
                        java.lang.reflect.Method healMethod = pokemon.getClass().getMethod("heal");
                        healMethod.invoke(pokemon);
                    }
                    player.sendMessage(net.minecraft.text.Text.literal("§a§l[Cheat] §fParty fully healed (HP, PP & Status cured)!"), false);
                } catch (Exception e) {
                    player.sendMessage(net.minecraft.text.Text.literal("§e[Cheat] Party healed!"), false);
                }
            }
            case "RESET_DATA" -> {
                profile.soloCheckpoint = 1;
                profile.duoCheckpoint = 1;
                profile.highestFloorTrueRun = 1;
                profile.battlePoints = 0;
                profile.prestigeLevel = 0;
                saveProfile(uuid);
                com.vitwo.party.TowerPartyManager.getInstance().syncPlayerState(player);
                player.sendMessage(net.minecraft.text.Text.literal("§c§l[Cheat] §fTower save data reset to initial Floor 1."), false);
            }
            case "START_FLOOR" -> {
                com.vitwo.party.TowerParty soloParty = com.vitwo.party.TowerPartyManager.getInstance().createSoloParty(player, value);
                com.vitwo.party.TowerPartyManager.getInstance().startTowerSession(soloParty, true, value, player.getServer());
                player.sendMessage(net.minecraft.text.Text.literal("§a§l[Cheat] §fLaunching Tower Session directly on Floor " + value + "!"), false);
            }
        }
    }
}
