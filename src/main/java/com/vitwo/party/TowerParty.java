package com.vitwo.party;

import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TowerParty {
    public enum State {
        LOBBY,
        PREPARING,
        IN_BATTLE,
        REST_FLOOR,
        COMPLETED
    }

    private final UUID leaderId;
    private UUID memberId; // null if isSolo
    private boolean isSolo;
    private boolean isTrueRun;
    private int currentFloor = 1;
    private int highestCheckpoint = 1;
    private State state = State.LOBBY;

    private BlockPos originalLeaderPos;
    private BlockPos originalMemberPos;
    private String originalLeaderDim = "minecraft:overworld";
    private String originalMemberDim = "minecraft:overworld";
    private double originalLeaderX, originalLeaderY, originalLeaderZ;
    private double originalMemberX, originalMemberY, originalMemberZ;
    private float originalLeaderYaw, originalLeaderPitch;
    private float originalMemberYaw, originalMemberPitch;

    private String currentTrainerId = "";
    private String currentBossName = "";

    private final Set<UUID> spectatingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> restChoices = new ConcurrentHashMap<>();
    private final Set<UUID> forfeitVotes = ConcurrentHashMap.newKeySet();
    private final Set<UUID> readyPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<String, Integer>> originalPokemonLevels = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> originalPokemonExperience = new ConcurrentHashMap<>();
    private int stallTicks = 0;
    private int hardStallTicks = 0;
    private int noBattleTicks = 0;

    private UUID disconnectedPlayerId = null;
    private long disconnectTimestamp = 0;
    private static final long DISCONNECT_GRACE_PERIOD_MS = 180_000L; // 3 minutes

    private int turnsElapsed = 0;
    private int faintsCount = 0;
    private final long startTimeMillis = System.currentTimeMillis();

    private boolean mercyUsed = false;
    private String warPrepBuff = "NONE";
    private int warPrepFloorsRemaining = 0;
    private int ghostSupportCharges = 0;
    private int turnsSinceLastSupportCharge = 0;
    private int attemptId = (int) (System.currentTimeMillis() % 100000);
    private int arenaSlot = -1;

    private final List<com.vitwo.reward.GachaPokemonCandidate> encounteredPokemonHistory = new java.util.concurrent.CopyOnWriteArrayList<>();
    private int lastGachaFloor = 0;

    public void recordEncounteredPokemon(com.cobblemon.mod.common.pokemon.Pokemon mon) {
        if (mon == null || mon.getSpecies() == null) return;
        String speciesName = mon.getSpecies().getName().toLowerCase(Locale.ROOT);
        String displayName = mon.getSpecies().getTranslatedName().getString();

        com.cobblemon.mod.common.pokemon.Species base = com.vitwo.reward.TowerRewardManager.getBaseSpecies(mon.getSpecies());
        String baseSpecies = base != null ? base.getName().toLowerCase(Locale.ROOT) : speciesName;
        String formAspect = com.vitwo.reward.TowerRewardManager.extractRegionalAspect(mon);

        String pType = mon.getPrimaryType() != null ? mon.getPrimaryType().getName() : "Normal";
        String sType = mon.getSecondaryType() != null ? mon.getSecondaryType().getName() : "";
        boolean isLegend = com.vitwo.reward.TowerRewardManager.isLegendaryOrMythical(mon.getSpecies());
        boolean isShiny = mon.getShiny();

        var candidate = com.vitwo.reward.GachaPokemonCandidate.of(
                encounteredPokemonHistory.size(),
                speciesName,
                displayName,
                baseSpecies,
                formAspect,
                pType,
                sType,
                isLegend,
                isShiny
        );

        boolean exists = encounteredPokemonHistory.stream().anyMatch(c ->
                c.speciesName().equalsIgnoreCase(speciesName) && c.formAspect().equalsIgnoreCase(formAspect));
        if (!exists) {
            encounteredPokemonHistory.add(candidate);
        }
    }

    public List<com.vitwo.reward.GachaPokemonCandidate> drainEncounteredCandidates(int floor) {
        List<com.vitwo.reward.GachaPokemonCandidate> pool = new ArrayList<>(encounteredPokemonHistory);
        encounteredPokemonHistory.clear();
        this.lastGachaFloor = floor;
        return pool;
    }

    public int getLastGachaFloor() {
        return lastGachaFloor;
    }

    public void setLastGachaFloor(int lastGachaFloor) {
        this.lastGachaFloor = lastGachaFloor;
    }

    public void rollFloorTrainer(int floor) {
        this.currentTrainerId = com.vitwo.battle.TrainerPool.getTrainerIdForFloor(this.attemptId, floor);
        this.currentBossName = com.vitwo.battle.TrainerPool.getTrainerDisplayName(this.attemptId, floor, this.currentTrainerId);
    }

    public String getCurrentTrainerId() {
        if (currentTrainerId == null || currentTrainerId.isEmpty()) {
            rollFloorTrainer(currentFloor);
        }
        return currentTrainerId;
    }

    public void setCurrentTrainerId(String trainerId) {
        this.currentTrainerId = trainerId;
    }

    public String getCurrentBossName() {
        if (currentBossName == null || currentBossName.isEmpty()) {
            rollFloorTrainer(currentFloor);
        }
        return currentBossName;
    }

    public void setCurrentBossName(String bossName) {
        this.currentBossName = bossName;
    }

    public String getOriginalLeaderDim() {
        return originalLeaderDim;
    }

    public void setOriginalLeaderDim(String dim) {
        this.originalLeaderDim = dim;
    }

    public String getOriginalMemberDim() {
        return originalMemberDim;
    }

    public void setOriginalMemberDim(String dim) {
        this.originalMemberDim = dim;
    }

    public double getOriginalLeaderX() { return originalLeaderX; }
    public double getOriginalLeaderY() { return originalLeaderY; }
    public double getOriginalLeaderZ() { return originalLeaderZ; }
    public float getOriginalLeaderYaw() { return originalLeaderYaw; }
    public float getOriginalLeaderPitch() { return originalLeaderPitch; }

    public void setOriginalLeaderExact(String dim, double x, double y, double z, float yaw, float pitch) {
        this.originalLeaderDim = dim != null ? dim : "minecraft:overworld";
        this.originalLeaderX = x;
        this.originalLeaderY = y;
        this.originalLeaderZ = z;
        this.originalLeaderYaw = yaw;
        this.originalLeaderPitch = pitch;
        this.originalLeaderPos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public double getOriginalMemberX() { return originalMemberX; }
    public double getOriginalMemberY() { return originalMemberY; }
    public double getOriginalMemberZ() { return originalMemberZ; }
    public float getOriginalMemberYaw() { return originalMemberYaw; }
    public float getOriginalMemberPitch() { return originalMemberPitch; }

    public void setOriginalMemberExact(String dim, double x, double y, double z, float yaw, float pitch) {
        this.originalMemberDim = dim != null ? dim : "minecraft:overworld";
        this.originalMemberX = x;
        this.originalMemberY = y;
        this.originalMemberZ = z;
        this.originalMemberYaw = yaw;
        this.originalMemberPitch = pitch;
        this.originalMemberPos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public int getArenaSlot() {
        return arenaSlot;
    }

    public int incrementHardStallTicks() {
        return ++hardStallTicks;
    }

    public void resetHardStallTicks() {
        hardStallTicks = 0;
    }

    public int incrementNoBattleTicks() {
        return ++noBattleTicks;
    }

    public void resetNoBattleTicks() {
        noBattleTicks = 0;
    }

    public void setArenaSlot(int arenaSlot) {
        this.arenaSlot = arenaSlot;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void rollNewAttempt() {
        this.attemptId = (int) ((System.currentTimeMillis() + (long)(Math.random() * 10000)) % 100000);
    }

    public TowerParty(UUID leaderId, int startingCheckpoint) {
        this(leaderId, null, startingCheckpoint);
    }

    public TowerParty(UUID leaderId, UUID memberId, int startingCheckpoint) {
        this.leaderId = leaderId;
        this.memberId = memberId;
        this.isSolo = (memberId == null);
        this.highestCheckpoint = startingCheckpoint;
        this.currentFloor = startingCheckpoint;
        this.isTrueRun = (startingCheckpoint == 1);
    }

    public boolean isSolo() {
        return isSolo;
    }

    public void setSolo(boolean solo) {
        this.isSolo = solo;
    }

    public boolean isTrueRun() {
        return isTrueRun;
    }

    public void setTrueRun(boolean trueRun) {
        this.isTrueRun = trueRun;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public void setMemberId(UUID memberId) {
        this.memberId = memberId;
        this.isSolo = (memberId == null);
    }

    public UUID getOtherPlayer(UUID playerId) {
        if (isSolo || memberId == null) return null;
        return playerId.equals(leaderId) ? memberId : leaderId;
    }

    public List<UUID> getAllMembers() {
        if (isSolo || memberId == null) {
            return List.of(leaderId);
        }
        return List.of(leaderId, memberId);
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int floor) {
        this.currentFloor = floor;
        if (floor > highestCheckpoint && isCheckpointFloor(floor)) {
            this.highestCheckpoint = floor;
        }
    }

    public static boolean isCheckpointFloor(int floor) {
        return floor == 1 || floor == 26 || floor == 51 || floor == 76;
    }

    public int getHighestCheckpoint() {
        return highestCheckpoint;
    }

    public void setHighestCheckpoint(int checkpoint) {
        this.highestCheckpoint = checkpoint;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public BlockPos getOriginalLeaderPos() {
        return originalLeaderPos;
    }

    public void setOriginalLeaderPos(BlockPos pos) {
        this.originalLeaderPos = pos;
    }

    public BlockPos getOriginalMemberPos() {
        return originalMemberPos;
    }

    public void setOriginalMemberPos(BlockPos pos) {
        this.originalMemberPos = pos;
    }

    public void setSpectating(UUID playerId, boolean spectating) {
        if (spectating) spectatingPlayers.add(playerId);
        else spectatingPlayers.remove(playerId);
    }

    public boolean isPlayerSpectating(UUID playerId) {
        return spectatingPlayers.contains(playerId);
    }

    public void clearSpectators() {
        spectatingPlayers.clear();
    }

    public void setRestChoice(UUID playerId, int choice) {
        restChoices.put(playerId, choice);
    }

    public boolean hasChosenRest(UUID playerId) {
        return restChoices.containsKey(playerId);
    }

    public boolean haveBothChosenRest() {
        if (isSolo || memberId == null) {
            return restChoices.containsKey(leaderId);
        }
        return restChoices.containsKey(leaderId) && restChoices.containsKey(memberId);
    }

    public void clearRestChoices() {
        restChoices.clear();
    }

    public boolean voteForfeit(UUID playerId) {
        forfeitVotes.add(playerId);
        return forfeitVotes.size() >= (isSolo ? 1 : 2);
    }

    public int getForfeitVoteCount() {
        return forfeitVotes.size();
    }

    public void clearForfeitVotes() {
        forfeitVotes.clear();
    }

    public void handlePlayerDisconnect(UUID playerId) {
        if (playerId.equals(leaderId) || playerId.equals(memberId)) {
            this.disconnectedPlayerId = playerId;
            this.disconnectTimestamp = System.currentTimeMillis();
        }
    }

    public boolean handlePlayerReconnect(UUID playerId) {
        if (playerId.equals(disconnectedPlayerId)) {
            this.disconnectedPlayerId = null;
            this.disconnectTimestamp = 0;
            return true;
        }
        return false;
    }

    public boolean isDisconnectGraceExpired() {
        if (disconnectedPlayerId == null) return false;
        return (System.currentTimeMillis() - disconnectTimestamp) > DISCONNECT_GRACE_PERIOD_MS;
    }

    public UUID getDisconnectedPlayerId() {
        return disconnectedPlayerId;
    }

    public void incrementTurns(int amount) {
        this.turnsElapsed += amount;
    }

    public int getTurnsElapsed() {
        return turnsElapsed;
    }

    public void setTurnsElapsed(int turnsElapsed) {
        this.turnsElapsed = turnsElapsed;
    }

    public void incrementFaints(int amount) {
        this.faintsCount += amount;
    }

    public int getFaintsCount() {
        return faintsCount;
    }

    public void setFaintsCount(int faintsCount) {
        this.faintsCount = faintsCount;
    }

    public int getDurationSeconds() {
        return (int) ((System.currentTimeMillis() - startTimeMillis) / 1000L);
    }

    private int bpEarnedInRun = 0;
    private int currentBattleTurns = 0;

    public int getBpEarnedInRun() {
        return bpEarnedInRun;
    }

    public void setBpEarnedInRun(int bpEarnedInRun) {
        this.bpEarnedInRun = bpEarnedInRun;
    }

    public void addBpEarnedInRun(int amount) {
        this.bpEarnedInRun += amount;
    }

    public int getCurrentBattleTurns() {
        return currentBattleTurns;
    }

    public void setCurrentBattleTurns(int currentBattleTurns) {
        this.currentBattleTurns = currentBattleTurns;
    }

    public boolean isMercyUsed() {
        return mercyUsed;
    }

    public void setMercyUsed(boolean mercyUsed) {
        this.mercyUsed = mercyUsed;
    }

    public String getWarPrepBuff() {
        return warPrepBuff;
    }

    public void setWarPrepBuff(String warPrepBuff, int floors) {
        this.warPrepBuff = warPrepBuff != null ? warPrepBuff : "NONE";
        this.warPrepFloorsRemaining = floors;
    }

    public int getWarPrepFloorsRemaining() {
        return warPrepFloorsRemaining;
    }

    public void decrementWarPrepFloor() {
        if (warPrepFloorsRemaining > 0) {
            warPrepFloorsRemaining--;
            if (warPrepFloorsRemaining <= 0) {
                warPrepBuff = "NONE";
            }
        }
    }

    public int getMaxGhostSupportCharges() {
        if (currentFloor <= 50) return 2;
        if (currentFloor <= 75) return 3;
        return 4;
    }

    public int getGhostSupportCharges() {
        return ghostSupportCharges;
    }

    public void resetGhostChargesForBattle() {
        this.ghostSupportCharges = getMaxGhostSupportCharges();
        this.turnsSinceLastSupportCharge = 0;
    }

    public void addSupportCharge() {
        if (ghostSupportCharges < getMaxGhostSupportCharges()) {
            ghostSupportCharges++;
        }
    }

    public boolean useSupportCharge(int amount) {
        if (ghostSupportCharges >= amount) {
            ghostSupportCharges -= amount;
            return true;
        }
        return false;
    }

    public void onTurnTickForSupportCharge() {
        turnsSinceLastSupportCharge++;
        if (turnsSinceLastSupportCharge >= 3) {
            turnsSinceLastSupportCharge = 0;
            addSupportCharge();
        }
    }

    public void setReady(UUID playerId, boolean ready) {
        if (ready) readyPlayers.add(playerId);
        else readyPlayers.remove(playerId);
    }

    public boolean isReady(UUID playerId) {
        return readyPlayers.contains(playerId);
    }

    public int getReadyCount() {
        return readyPlayers.size();
    }

    public boolean areBothReady() {
        if (isSolo || memberId == null) {
            return readyPlayers.contains(leaderId);
        }
        return readyPlayers.contains(leaderId) && readyPlayers.contains(memberId);
    }

    public void clearReady() {
        readyPlayers.clear();
    }

    public void recordOriginalPokemonLevel(UUID playerId, UUID pokemonId, int level) {
        originalPokemonLevels.computeIfAbsent(playerId.toString(), k -> new ConcurrentHashMap<>()).putIfAbsent(pokemonId.toString(), level);
    }

    public void recordOriginalPokemonState(UUID playerId, UUID pokemonId, int level, int experience) {
        originalPokemonLevels.computeIfAbsent(playerId.toString(), k -> new ConcurrentHashMap<>()).putIfAbsent(pokemonId.toString(), level);
        originalPokemonExperience.computeIfAbsent(playerId.toString(), k -> new ConcurrentHashMap<>()).putIfAbsent(pokemonId.toString(), experience);
    }

    public Integer getOriginalPokemonLevel(UUID playerId, UUID pokemonId) {
        Map<String, Integer> map = originalPokemonLevels.get(playerId.toString());
        return map != null ? map.get(pokemonId.toString()) : null;
    }

    public Integer getOriginalPokemonExperience(UUID playerId, UUID pokemonId) {
        Map<String, Integer> map = originalPokemonExperience.get(playerId.toString());
        return map != null ? map.get(pokemonId.toString()) : null;
    }

    /**
     * Returns the full map of originalPokemonUUID -> originalLevel for a specific player.
     * Used by LevelCapManager.restorePlayerLevels() for safe bulk restoration.
     */
    public Map<String, Integer> getOriginalPokemonLevelsForPlayer(UUID playerId) {
        return originalPokemonLevels.get(playerId.toString());
    }

    public Map<String, Integer> getOriginalPokemonExperienceForPlayer(UUID playerId) {
        return originalPokemonExperience.get(playerId.toString());
    }

    public Map<String, Map<String, Integer>> getAllOriginalPokemonLevels() {
        return originalPokemonLevels;
    }

    public Map<String, Map<String, Integer>> getAllOriginalPokemonExperience() {
        return originalPokemonExperience;
    }

    public void setAllOriginalPokemonLevels(Map<String, Map<String, Integer>> levels) {
        originalPokemonLevels.clear();
        if (levels != null) {
            originalPokemonLevels.putAll(levels);
        }
    }

    public void setAllOriginalPokemonExperience(Map<String, Map<String, Integer>> expMap) {
        originalPokemonExperience.clear();
        if (expMap != null) {
            originalPokemonExperience.putAll(expMap);
        }
    }

    public void clearOriginalPokemonLevels() {
        originalPokemonLevels.clear();
        originalPokemonExperience.clear();
    }

    public int incrementStallTicks() {
        return ++this.stallTicks;
    }

    public void resetStallTicks() {
        this.stallTicks = 0;
    }

    public int getStallTicks() {
        return this.stallTicks;
    }
}
