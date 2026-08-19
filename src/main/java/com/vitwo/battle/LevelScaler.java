package com.vitwo.battle;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelScaler {
    private static final LevelScaler INSTANCE = new LevelScaler();
    public static LevelScaler getInstance() { return INSTANCE; }

    // Map: Player UUID -> Map<Pokemon Slot Index, Original Level>
    private final Map<UUID, Map<Integer, Integer>> originalLevels = new ConcurrentHashMap<>();

    private LevelScaler() {}

    public static int getTargetLevelForFloor(int floor) {
        return (floor <= 50) ? 50 : 100;
    }

    public void scalePlayerTeam(ServerPlayerEntity player, int floor) {
        if (player == null) return;
        int targetLevel = getTargetLevelForFloor(floor);
        UUID uuid = player.getUuid();

        // In Cobblemon environment:
        // Reads CobblemonStorage.getParty(player)
        // Records original level for each slot, then adjusts battle level to targetLevel (50 or 100)
        originalLevels.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        if (targetLevel > 0) {
            // Prepared scale context
        }
    }

    public void restorePlayerTeam(ServerPlayerEntity player) {
        if (player == null) return;
        UUID uuid = player.getUuid();
        Map<Integer, Integer> levels = originalLevels.remove(uuid);
        if (levels != null) {
            // Reverts party to original levels
        }
    }
}
