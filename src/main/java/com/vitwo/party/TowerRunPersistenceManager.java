package com.vitwo.party;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TowerRunPersistenceManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final TowerRunPersistenceManager INSTANCE = new TowerRunPersistenceManager();
    public static TowerRunPersistenceManager getInstance() { return INSTANCE; }

    public static class SavedPokemonState {
        public String species;
        public int currentHp;
        public int maxHp;
        public int[] ppRemaining = new int[4];
        public String statusCondition = "NONE";
        public boolean isFainted = false;
        public String heldItem = "";
    }

    public static class ActiveRunData {
        public int schema_version = 1;
        public UUID playerA;
        public UUID playerB;
        public boolean isSolo = true;
        public boolean isTrueRun = true;
        public int startFloor = 1;
        public int currentFloor = 1;
        public int bpEarned = 0;
        public int totalTurns = 0;
        public int totalFaints = 0;
        public boolean mercyUsed = false;
        public long startTimestamp = System.currentTimeMillis();
        public boolean isPaused = false;
        public long pauseTimestamp = 0;
        public String activeWarPrepBuff = "NONE";
        public int warPrepFloorsRemaining = 0;
        public String currentTrainerId = "";
        public String currentBossName = "";
        public String originalLeaderDim = "minecraft:overworld";
        public String originalMemberDim = "minecraft:overworld";
        public double originalLeaderX, originalLeaderY, originalLeaderZ;
        public double originalMemberX, originalMemberY, originalMemberZ;
        public float originalLeaderYaw, originalLeaderPitch;
        public float originalMemberYaw, originalMemberPitch;
        public List<SavedPokemonState> teamA = new ArrayList<>();
        public List<SavedPokemonState> teamB = new ArrayList<>();
        public Map<String, Map<String, Integer>> originalPokemonLevels = new ConcurrentHashMap<>();
        public Map<String, Map<String, Integer>> originalPokemonExperience = new ConcurrentHashMap<>();
    }

    private final Map<UUID, ActiveRunData> activeRuns = new ConcurrentHashMap<>();

    private TowerRunPersistenceManager() {}

    private File getRunFile(UUID uuid) {
        net.minecraft.server.MinecraftServer server = TowerPartyManager.getInstance().getCurrentServer();
        Path baseDir;
        if (server != null) {
            baseDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("cobbletower_data").resolve("runs");
        } else {
            baseDir = FabricLoader.getInstance().getGameDir().resolve("cobbletower_data").resolve("runs");
        }
        File dir = baseDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return baseDir.resolve(uuid.toString() + ".json").toFile();
    }

    public synchronized void saveRun(TowerParty party) {
        saveRun(party, 0);
    }

    public synchronized void saveRun(TowerParty party, int bpEarnedSoFar) {
        if (party == null) return;
        ActiveRunData data = new ActiveRunData();
        data.playerA = party.getLeaderId();
        data.playerB = party.getMemberId();
        data.isSolo = party.isSolo();
        data.isTrueRun = party.isTrueRun();
        data.startFloor = party.getHighestCheckpoint();
        data.currentFloor = party.getCurrentFloor();
        data.bpEarned = bpEarnedSoFar;
        data.totalTurns = party.getTurnsElapsed();
        data.totalFaints = party.getFaintsCount();
        data.mercyUsed = party.isMercyUsed();
        data.activeWarPrepBuff = party.getWarPrepBuff();
        data.warPrepFloorsRemaining = party.getWarPrepFloorsRemaining();
        data.currentTrainerId = party.getCurrentTrainerId();
        data.currentBossName = party.getCurrentBossName();
        data.originalLeaderDim = party.getOriginalLeaderDim();
        data.originalLeaderX = party.getOriginalLeaderX();
        data.originalLeaderY = party.getOriginalLeaderY();
        data.originalLeaderZ = party.getOriginalLeaderZ();
        data.originalLeaderYaw = party.getOriginalLeaderYaw();
        data.originalLeaderPitch = party.getOriginalLeaderPitch();
        data.originalMemberDim = party.getOriginalMemberDim();
        data.originalMemberX = party.getOriginalMemberX();
        data.originalMemberY = party.getOriginalMemberY();
        data.originalMemberZ = party.getOriginalMemberZ();
        data.originalMemberYaw = party.getOriginalMemberYaw();
        data.originalMemberPitch = party.getOriginalMemberPitch();

        // Save original pokemon levels and experience for safe restoration
        if (party.getAllOriginalPokemonLevels() != null) {
            data.originalPokemonLevels.putAll(party.getAllOriginalPokemonLevels());
        }
        if (party.getAllOriginalPokemonExperience() != null) {
            data.originalPokemonExperience.putAll(party.getAllOriginalPokemonExperience());
        }

        activeRuns.put(party.getLeaderId(), data);
        if (party.getMemberId() != null) {
            activeRuns.put(party.getMemberId(), data);
        }

        saveToFile(party.getLeaderId(), data);
    }

    private void saveToFile(UUID uuid, ActiveRunData data) {
        String json = GSON.toJson(data);
        File file = getRunFile(uuid);

        com.vitwo.config.TowerPersistenceService.getInstance().submitAsyncTask(() -> {
            File tmpFile = new File(file.getAbsolutePath() + ".tmp");
            File bakFile = new File(file.getAbsolutePath() + ".bak");

            try {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpFile);
                     java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8);
                     java.io.BufferedWriter writer = new java.io.BufferedWriter(osw)) {
                    writer.write(json);
                    writer.flush();
                    fos.getFD().sync();
                }

                if (file.exists()) {
                    try {
                        java.nio.file.Files.copy(file.toPath(), bakFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception ignored) {}
                }

                java.nio.file.Files.move(tmpFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Failed to asynchronously save run data for " + uuid, e);
            }
        });
    }

    public synchronized Optional<ActiveRunData> getActiveRun(UUID playerUuid) {
        if (activeRuns.containsKey(playerUuid)) {
            return Optional.of(activeRuns.get(playerUuid));
        }

        File file = getRunFile(playerUuid);
        File bakFile = new File(file.getAbsolutePath() + ".bak");

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ActiveRunData data = GSON.fromJson(reader, ActiveRunData.class);
                if (data != null) {
                    activeRuns.put(playerUuid, data);
                    return Optional.of(data);
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("vitwo").warn("[CobbleTower] Corrupted run data file for " + playerUuid + ", attempting backup recovery: " + e.getMessage());
            }
        }

        if (bakFile.exists()) {
            try (FileReader reader = new FileReader(bakFile)) {
                ActiveRunData data = GSON.fromJson(reader, ActiveRunData.class);
                if (data != null) {
                    org.slf4j.LoggerFactory.getLogger("vitwo").info("[CobbleTower] Successfully recovered run data from backup (.bak) for " + playerUuid);
                    activeRuns.put(playerUuid, data);
                    saveToFile(playerUuid, data);
                    return Optional.of(data);
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("vitwo").error("[CobbleTower] Failed to recover from backup file for " + playerUuid, e);
            }
        }

        return Optional.empty();
    }

    public synchronized void pauseRun(ServerPlayerEntity player) {
        Optional<ActiveRunData> runOpt = getActiveRun(player.getUuid());
        if (runOpt.isEmpty()) return;

        ActiveRunData data = runOpt.get();
        data.isPaused = true;
        data.pauseTimestamp = System.currentTimeMillis();
        saveToFile(data.playerA, data);
        if (data.playerB != null) {
            saveToFile(data.playerB, data);
        }
    }

    public synchronized void deleteRun(UUID playerUuid) {
        Optional<ActiveRunData> runOpt = getActiveRun(playerUuid);
        activeRuns.remove(playerUuid);
        File file = getRunFile(playerUuid);
        if (file.exists()) file.delete();
        File tmpFile = new File(file.getAbsolutePath() + ".tmp");
        if (tmpFile.exists()) tmpFile.delete();
        File bakFile = new File(file.getAbsolutePath() + ".bak");
        if (bakFile.exists()) bakFile.delete();

        if (runOpt.isPresent()) {
            ActiveRunData data = runOpt.get();
            UUID other = data.playerA.equals(playerUuid) ? data.playerB : data.playerA;
            if (other != null) {
                activeRuns.remove(other);
                File file2 = getRunFile(other);
                if (file2.exists()) file2.delete();
                File tmpFile2 = new File(file2.getAbsolutePath() + ".tmp");
                if (tmpFile2.exists()) tmpFile2.delete();
                File bakFile2 = new File(file2.getAbsolutePath() + ".bak");
                if (bakFile2.exists()) bakFile2.delete();
            }
        }
    }

    public synchronized boolean hasActiveRun(UUID playerUuid) {
        return getActiveRun(playerUuid).isPresent();
    }

    public synchronized boolean restoreRun(ServerPlayerEntity player, net.minecraft.server.MinecraftServer server) {
        if (player == null || server == null) return false;
        Optional<ActiveRunData> runOpt = getActiveRun(player.getUuid());
        if (runOpt.isEmpty()) return false;

        ActiveRunData data = runOpt.get();
        TowerParty party = new TowerParty(data.playerA, data.startFloor);
        party.setSolo(data.isSolo);
        party.setTrueRun(data.isTrueRun);
        party.setCurrentFloor(data.currentFloor);
        party.setMercyUsed(data.mercyUsed);
        party.setTurnsElapsed(data.totalTurns);
        party.setFaintsCount(data.totalFaints);
        if (data.activeWarPrepBuff != null && !data.activeWarPrepBuff.equals("NONE")) {
            party.setWarPrepBuff(data.activeWarPrepBuff, data.warPrepFloorsRemaining);
        }

        if (!data.isSolo && data.playerB != null) {
            party.setMemberId(data.playerB);
        }

        if (data.currentTrainerId != null && !data.currentTrainerId.isEmpty()) {
            party.setCurrentTrainerId(data.currentTrainerId);
            party.setCurrentBossName(data.currentBossName);
        }

        party.setOriginalLeaderExact(data.originalLeaderDim, data.originalLeaderX, data.originalLeaderY, data.originalLeaderZ, data.originalLeaderYaw, data.originalLeaderPitch);
        party.setOriginalMemberExact(data.originalMemberDim, data.originalMemberX, data.originalMemberY, data.originalMemberZ, data.originalMemberYaw, data.originalMemberPitch);

        // Restore original pokemon levels & experience
        if (data.originalPokemonLevels != null) {
            party.setAllOriginalPokemonLevels(data.originalPokemonLevels);
        }
        if (data.originalPokemonExperience != null) {
            party.setAllOriginalPokemonExperience(data.originalPokemonExperience);
        }

        TowerPartyManager.getInstance().registerRestoredParty(party, server);
        return true;
    }
}
