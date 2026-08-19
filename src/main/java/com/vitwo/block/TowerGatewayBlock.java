package com.vitwo.block;

import com.vitwo.network.s2c.OpenTowerEntryS2CPacket;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public class TowerGatewayBlock extends Block {
    public TowerGatewayBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            TowerPartyManager.getInstance().syncPlayerState(serverPlayer);

            Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(serverPlayer.getUuid());
            boolean hasParty = partyOpt.isPresent();
            boolean isLeader = hasParty && partyOpt.get().getLeaderId().equals(serverPlayer.getUuid());
            int soloCp = TowerPartyManager.getInstance().getSoloCheckpoint(serverPlayer.getUuid());

            String partnerName = "";
            if (hasParty) {
                TowerParty party = partyOpt.get();
                ServerPlayerEntity other = serverPlayer.getServer().getPlayerManager().getPlayer(party.getOtherPlayer(serverPlayer.getUuid()));
                if (other != null) partnerName = other.getName().getString();
            }

            ServerPlayNetworking.send(serverPlayer, new OpenTowerEntryS2CPacket(
                    soloCp,
                    isLeader,
                    partnerName
            ));
        }

        return ActionResult.SUCCESS;
    }
}
