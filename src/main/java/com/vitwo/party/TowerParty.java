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
    private final UUID memberId; // null if isSolo
    private final boolean isSolo;
    private int currentFloor = 1;
    private int highestCheckpoint = 1;
    private State state = State.LOBBY;

    private BlockPos originalLeaderPos;
    private BlockPos originalMemberPos;

    private final Set<UUID> spectatingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> restChoices = new ConcurrentHashMap<>();

    private UUID disconnectedPlayerId = null;
    private long disconnectTimestamp = 0;
    private static final long DISCONNECT_GRACE_PERIOD_MS = 180_000L; // 3 minutes

    public TowerParty(UUID leaderId, int startingCheckpoint) {
        this(leaderId, null, startingCheckpoint);
    }

    public TowerParty(UUID leaderId, UUID memberId, int startingCheckpoint) {
        this.leaderId = leaderId;
        this.memberId = memberId;
        this.isSolo = (memberId == null);
        this.highestCheckpoint = startingCheckpoint;
        this.currentFloor = startingCheckpoint;
    }

    public boolean isSolo() {
        return isSolo;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public UUID getOtherPlayer(UUID playerId) {
        if (isSolo || memberId == null) return null;
        return playerId.equals(leaderId) ? memberId : leaderId;
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
        return floor == 1 || floor == 10 || floor == 25 || floor == 50 || floor == 75 || floor == 90;
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

    public void handlePlayerDisconnect(UUID playerId) {
        this.disconnectedPlayerId = playerId;
        this.disconnectTimestamp = System.currentTimeMillis();
    }

    public void handlePlayerReconnect(UUID playerId) {
        if (playerId.equals(disconnectedPlayerId)) {
            this.disconnectedPlayerId = null;
            this.disconnectTimestamp = 0;
        }
    }

    public boolean isPlayerDisconnected() {
        return disconnectedPlayerId != null;
    }

    public UUID getDisconnectedPlayerId() {
        return disconnectedPlayerId;
    }

    public boolean isDisconnectTimedOut() {
        if (!isPlayerDisconnected()) return false;
        return System.currentTimeMillis() - disconnectTimestamp >= DISCONNECT_GRACE_PERIOD_MS;
    }
}
