package com.vitwo.mod;

import com.vitwo.block.ViTwoBlocks;
import com.vitwo.network.ViTwoPackets;
import com.vitwo.network.c2s.*;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
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

        ServerPlayNetworking.registerGlobalReceiver(com.vitwo.network.c2s.DebugTowerActionC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> com.vitwo.config.TowerPlayerDataManager.getInstance().handleDebugAction(player, payload.action(), payload.value()));
        });

        // 4. Register Dimension Block Interaction Restrictions
        com.vitwo.event.TowerBlockInteractionHandler.register();

        // 5. Register Trainer NPC Direct Battle Interaction Bypass
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                String entityTypeId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
                if (entityTypeId.contains("trainer") && world.getRegistryKey().getValue().getPath().contains("tower")) {
                    try {
                        Class<?> rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
                        Method getInstanceMethod = rctModClass.getMethod("getInstance");
                        Object rctInstance = getInstanceMethod.invoke(null);
                        Method makeBattleMethod = rctModClass.getMethod("makeBattle", entity.getClass(), net.minecraft.entity.player.PlayerEntity.class);
                        Object result = makeBattleMethod.invoke(rctInstance, entity, serverPlayer);
                        if (Boolean.TRUE.equals(result)) {
                            Method setOpponentMethod = entity.getClass().getMethod("setOpponent", net.minecraft.entity.player.PlayerEntity.class);
                            setOpponentMethod.invoke(entity, serverPlayer);
                            return ActionResult.SUCCESS;
                        }
                    } catch (Throwable t) {
                        LOGGER.warn("[CobbleTower] Direct battle trigger bypass note: {}", t.getMessage());
                    }
                }
            }
            return ActionResult.PASS;
        });

        // 6. Register Tick & Reconnection Handlers
        ServerTickEvents.END_SERVER_TICK.register(server -> TowerPartyManager.getInstance().tick(server));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            TowerPartyManager.getInstance().handleDisconnect(handler.getPlayer());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            TowerPartyManager.getInstance().handleReconnect(handler.getPlayer());
        });

        // 7. Register Commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.vitwo.command.TowerCommands.register(dispatcher);
        });

        LOGGER.info("[ViTwo] CobbleTower Mod initialized successfully (Solo 6v6 + Duo Co-op)!");
    }
}
