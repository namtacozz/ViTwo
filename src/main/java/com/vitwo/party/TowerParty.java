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

    private final Set<UUID> spectatingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> restChoices = new ConcurrentHashMap<>();
    private final Set<UUID> forfeitVotes = ConcurrentHashMap.newKeySet();

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
}
