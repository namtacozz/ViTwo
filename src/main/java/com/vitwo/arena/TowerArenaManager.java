package com.vitwo.arena;

import com.vitwo.party.TowerParty;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TowerArenaManager {
    private static final TowerArenaManager INSTANCE = new TowerArenaManager();
    public static TowerArenaManager getInstance() { return INSTANCE; }

    private TowerArenaManager() {}

    public void teleportPartyToArena(TowerParty party, ServerPlayerEntity leader, ServerPlayerEntity member) {
        ServerWorld world = leader.getServerWorld();
        BlockPos basePos = leader.getBlockPos().up(50);

        if (party.isSolo() || member == null) {
            // Solo Mode: Player in center spot
            leader.teleport(world, basePos.getX() + 0.5, basePos.getY(), basePos.getZ() - 6.0, 0.0f, 0.0f);
        } else {
            // Duo Mode: Leader on left, Member on right
            leader.teleport(world, basePos.getX() - 3.5, basePos.getY(), basePos.getZ() - 6.0, 0.0f, 0.0f);
            member.teleport(world, basePos.getX() + 3.5, basePos.getY(), basePos.getZ() - 6.0, 0.0f, 0.0f);
        }
    }

    public void returnPlayerToOriginalPos(ServerPlayerEntity player, BlockPos originalPos) {
        if (originalPos != null) {
            player.teleport(player.getServerWorld(), originalPos.getX() + 0.5, originalPos.getY(), originalPos.getZ() + 0.5, player.getYaw(), player.getPitch());
        }
    }
}
