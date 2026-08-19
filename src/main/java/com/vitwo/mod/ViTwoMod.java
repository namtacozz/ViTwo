package com.vitwo.mod;

import com.vitwo.block.ViTwoBlocks;
import com.vitwo.network.ViTwoPackets;
import com.vitwo.network.c2s.*;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ViTwoMod implements ModInitializer {
    public static final String MOD_ID = "vitwo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[ViTwo] Initializing CobbleTower (Solo 6v6 & Duo Co-op Architecture)...");

        // 1. Register Custom Blocks & Items
        ViTwoBlocks.registerBlocks();

        // 2. Register Network CustomPayloads
        ViTwoPackets.registerPayloads();

        // 3. Register C2S Network Packet Receivers
        ServerPlayNetworking.registerGlobalReceiver(InvitePlayerC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity inviter = context.player();
            if (inviter.getServer() == null) return;
            ServerPlayerEntity target = inviter.getServer().getPlayerManager().getPlayer(payload.targetPlayerId());
            if (target != null) {
                context.server().execute(() -> TowerPartyManager.getInstance().invitePlayer(inviter, target));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RespondInviteC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity target = context.player();
            context.server().execute(() -> TowerPartyManager.getInstance().respondInvite(target, payload.accepted()));
        });

        ServerPlayNetworking.registerGlobalReceiver(StartTowerC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (payload.isSolo()) {
                    // Solo Mode: create dedicated solo party session
                    TowerParty soloParty = TowerPartyManager.getInstance().createSoloParty(player, payload.checkpointFloor());
                    TowerPartyManager.getInstance().startTowerSession(soloParty, true, payload.checkpointFloor(), player.getServer());
                } else {
                    // Duo Mode: validate active party and leader role
                    Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(player.getUuid());
                    if (partyOpt.isPresent()) {
                        TowerParty party = partyOpt.get();
                        if (party.getLeaderId().equals(player.getUuid())) {
                            TowerPartyManager.getInstance().startTowerSession(party, false, payload.checkpointFloor(), player.getServer());
                        }
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LeavePartyC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> TowerPartyManager.getInstance().leaveParty(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(RestChoiceC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> TowerPartyManager.getInstance().handleRestChoice(player, payload.choiceType()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ForfeitTowerC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> TowerPartyManager.getInstance().handleForfeitVote(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(BuyBpItemC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> com.vitwo.reward.TowerRewardManager.getInstance().handleBpPurchase(player, payload.itemId()));
        });

        ServerPlayNetworking.registerGlobalReceiver(GhostSupportActionC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> com.vitwo.battle.TowerSpectatorManager.getInstance().handleGhostSupportAction(player, payload.actionType()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ReadyTeamPreviewC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> com.vitwo.battle.TowerBattleManager.getInstance().handleReadyTeamPreview(player, payload.slotOrder()));
        });

        // 4. Register Dimension Block Interaction Restrictions
        com.vitwo.event.TowerBlockInteractionHandler.register();

        // 5. Register Tick & Reconnection Handlers
        ServerTickEvents.END_SERVER_TICK.register(server -> TowerPartyManager.getInstance().tick(server));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            TowerPartyManager.getInstance().handleDisconnect(handler.getPlayer());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            TowerPartyManager.getInstance().handleReconnect(handler.getPlayer());
        });

        // 6. Register Commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.vitwo.command.TowerCommands.register(dispatcher);
        });

        LOGGER.info("[ViTwo] CobbleTower Mod initialized successfully (Solo 6v6 + Duo Co-op)!");
    }
}
