package com.vitwo.party;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
        public List<SavedPokemonState> teamA = new ArrayList<>();
        public List<SavedPokemonState> teamB = new ArrayList<>();
    }

    private final Map<UUID, ActiveRunData> activeRuns = new ConcurrentHashMap<>();

    private TowerRunPersistenceManager() {}

    private File getRunFile(UUID uuid) {
        Path baseDir = FabricLoader.getInstance().getGameDir().resolve("cobbletower_data").resolve("runs");
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

        activeRuns.put(party.getLeaderId(), data);
        if (party.getMemberId() != null) {
            activeRuns.put(party.getMemberId(), data);
        }

        saveToFile(party.getLeaderId(), data);
    }

    private void saveToFile(UUID uuid, ActiveRunData data) {
        File file = getRunFile(uuid);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized Optional<ActiveRunData> getActiveRun(UUID playerUuid) {
        if (activeRuns.containsKey(playerUuid)) {
            return Optional.of(activeRuns.get(playerUuid));
        }

        File file = getRunFile(playerUuid);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ActiveRunData data = GSON.fromJson(reader, ActiveRunData.class);
                if (data != null) {
                    activeRuns.put(playerUuid, data);
                    return Optional.of(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        if (file.exists()) {
            file.delete();
        }
        if (runOpt.isPresent()) {
            ActiveRunData data = runOpt.get();
            UUID other = data.playerA.equals(playerUuid) ? data.playerB : data.playerA;
            if (other != null) {
                activeRuns.remove(other);
                File file2 = getRunFile(other);
                if (file2.exists()) file2.delete();
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

        TowerPartyManager.getInstance().startTowerSession(party, data.isSolo, data.currentFloor, server);
        return true;
    }
}
