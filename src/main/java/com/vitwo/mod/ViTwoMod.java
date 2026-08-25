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
import java.util.Locale;
import java.util.Map;
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

        // 3. Register C2S Network Packet Receivers with Strict Server Validation
        ServerPlayNetworking.registerGlobalReceiver(InvitePlayerC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity inviter = context.player();
            if (inviter == null || !inviter.isAlive() || inviter.getServer() == null) return;
            ServerPlayerEntity target = inviter.getServer().getPlayerManager().getPlayer(payload.targetPlayerId());
            if (target != null && target.isAlive() && !target.getUuid().equals(inviter.getUuid())) {
                context.server().execute(() -> {
                    Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(inviter.getUuid());
                    if (partyOpt.isPresent() && !partyOpt.get().getLeaderId().equals(inviter.getUuid())) {
                        inviter.sendMessage(net.minecraft.text.Text.literal("§c[CobbleTower] Only the Party Leader can invite players!"), false);
                        return;
                    }
                    TowerPartyManager.getInstance().invitePlayer(inviter, target);
                });
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RespondInviteC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity target = context.player();
            if (target == null || !target.isAlive()) return;
            context.server().execute(() -> TowerPartyManager.getInstance().respondInvite(target, payload.accepted()));
        });

        ServerPlayNetworking.registerGlobalReceiver(StartTowerC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive() || player.getServer() == null) return;
            if (player.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
                player.sendMessage(net.minecraft.text.Text.literal("§c[CobbleTower] You are already inside the Tower!"), false);
                return;
            }
            final int safeCheckpoint = Math.max(1, Math.min(100, payload.checkpointFloor()));
            context.server().execute(() -> {
                if (payload.isSolo()) {
                    TowerParty soloParty = TowerPartyManager.getInstance().createSoloParty(player, safeCheckpoint);
                    TowerPartyManager.getInstance().startTowerSession(soloParty, true, safeCheckpoint, player.getServer());
                } else {
                    Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(player.getUuid());
                    if (partyOpt.isPresent()) {
                        TowerParty party = partyOpt.get();
                        if (party.getLeaderId().equals(player.getUuid())) {
                            TowerPartyManager.getInstance().startTowerSession(party, false, safeCheckpoint, player.getServer());
                        } else {
                            player.sendMessage(net.minecraft.text.Text.literal("§c[CobbleTower] Only the Party Leader can start the Tower Run!"), false);
                        }
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LeavePartyC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive()) return;
            context.server().execute(() -> TowerPartyManager.getInstance().leaveParty(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(RestChoiceC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive()) return;
            context.server().execute(() -> {
                Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(player.getUuid());
                if (partyOpt.isPresent() && (partyOpt.get().getState() == TowerParty.State.REST_FLOOR || partyOpt.get().getState() == TowerParty.State.LOBBY)) {
                    TowerPartyManager.getInstance().handleRestChoice(player, payload.choiceType());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ForfeitTowerC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive()) return;
            context.server().execute(() -> {
                Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(player.getUuid());
                if (partyOpt.isPresent()) {
                    TowerPartyManager.getInstance().handleForfeitVote(player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(BuyBpItemC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive() || payload.itemId() == null || payload.itemId().isBlank()) return;
            context.server().execute(() -> com.vitwo.reward.TowerRewardManager.getInstance().handleBpPurchase(player, payload.itemId(), payload.quantity()));
        });

        ServerPlayNetworking.registerGlobalReceiver(GhostSupportActionC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive() || payload.actionType() < 1 || payload.actionType() > 3) return;
            context.server().execute(() -> {
                Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(player.getUuid());
                if (partyOpt.isPresent() && partyOpt.get().isPlayerSpectating(player.getUuid())) {
                    com.vitwo.battle.TowerSpectatorManager.getInstance().handleGhostSupportAction(player, payload.actionType());
                }
            });
        });



        ServerPlayNetworking.registerGlobalReceiver(com.vitwo.network.c2s.DebugTowerActionC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive()) return;
            context.server().execute(() -> {
                boolean isOp = player.hasPermissionLevel(2) || (player.getServer() != null && player.getServer().getPlayerManager().isOperator(player.getGameProfile()));
                if (!isOp) {
                    player.sendMessage(net.minecraft.text.Text.literal("§c[CobbleTower] Access denied. Operator permission level 2 required for Dev Cheat Actions!"), false);
                    return;
                }
                com.vitwo.config.TowerPlayerDataManager.getInstance().handleDebugAction(player, payload.action(), payload.value());
            });
        });

        // 4. Register Dimension Block Interaction Restrictions
        com.vitwo.event.TowerBlockInteractionHandler.register();

        // 5. Register Trainer NPC Direct Battle Interaction (Triggering RCTMod Trainer Battle & Overworld Team Refresh)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && hand == net.minecraft.util.Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {
                String entityTypeId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
                if (entityTypeId.contains("trainer")) {
                    if (com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(serverPlayer) != null) {
                        return ActionResult.SUCCESS;
                    }

                    // Refresh trainer team in Overworld so old save entities get updated (only when not already in battle)
                    try {
                        Method isInBattleMethod = entity.getClass().getMethod("isInBattle");
                        boolean inBattle = (boolean) isInBattleMethod.invoke(entity);
                        if (!inBattle) {
                            Method getTrainerIdMethod = entity.getClass().getMethod("getTrainerId");
                            String trainerId = (String) getTrainerIdMethod.invoke(entity);
                            if (trainerId != null && !trainerId.isBlank()) {
                                Method setTrainerIdMethod = entity.getClass().getMethod("setTrainerId", String.class);
                                setTrainerIdMethod.invoke(entity, trainerId);
                            }
                        }
                    } catch (Throwable ignored) {}

                    if (world.getRegistryKey().getValue().getPath().contains("tower")) {
                        try {
                            // 1. Orient Trainer NPC to face player directly
                            float faceYaw = serverPlayer.getYaw() + 180.0f;
                            entity.setYaw(faceYaw);
                            entity.setHeadYaw(faceYaw);
                            entity.setBodyYaw(faceYaw);
                            entity.setPitch(0.0f);

                            // 2. Invoke makeBattle on RCTMod to bypass storyline/badge restrictions in tower
                            Class<?> rctModClass = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
                            Object rctModInstance = rctModClass.getMethod("getInstance").invoke(null);
                            Method makeBattleMethod = null;
                            for (Method m : rctModClass.getMethods()) {
                                if (m.getName().equals("makeBattle") && m.getParameterCount() == 2) {
                                    makeBattleMethod = m;
                                    break;
                                }
                            }
                            if (makeBattleMethod != null) {
                                makeBattleMethod.setAccessible(true);
                                boolean success = (boolean) makeBattleMethod.invoke(rctModInstance, entity, serverPlayer);
                                if (success) {
                                    for (Method m : entity.getClass().getMethods()) {
                                        if (m.getName().equals("setOpponent") && m.getParameterCount() == 1) {
                                            m.setAccessible(true);
                                            m.invoke(entity, serverPlayer);
                                            break;
                                        }
                                    }
                                    try {
                                        Object trainerManager = rctModClass.getMethod("getTrainerManager").invoke(rctModInstance);
                                        for (Method m : trainerManager.getClass().getMethods()) {
                                            if (m.getName().equals("addBattle") && m.getParameterCount() == 2) {
                                                m.setAccessible(true);
                                                m.invoke(trainerManager, serverPlayer, entity);
                                                break;
                                            }
                                        }
                                    } catch (Throwable ignored) {}
                                    return ActionResult.SUCCESS;
                                }
                            }
                        } catch (Throwable t) {
                            LOGGER.warn("[CobbleTower] Failed to trigger trainer battle: {}", t.getMessage());
                        }
                        return ActionResult.PASS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        // 6. Register Cobblemon Battle Victory/Defeat Lifecycle Hooks
        com.vitwo.battle.TowerBattleManager.getInstance().registerBattleEvents();

        // 7. Register Server Lifecycle, Tick & Reconnection Handlers
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TowerPartyManager.getInstance().setCurrentServer(server);
            com.vitwo.config.TowerPlayerDataManager.getInstance().clearCache();
            com.vitwo.config.TowerLeaderboardManager.getInstance().loadLeaderboard();
        });

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            com.vitwo.config.TowerPlayerDataManager.getInstance().clearCache();
            TowerPartyManager.getInstance().setCurrentServer(null);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> TowerPartyManager.getInstance().tick(server));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            TowerPartyManager.getInstance().handleDisconnect(handler.getPlayer());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            TowerPartyManager.getInstance().handleReconnect(handler.getPlayer());
        });

        // 8. Register Commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.vitwo.command.TowerCommands.register(dispatcher);
        });

        LOGGER.info("[ViTwo] CobbleTower Mod initialized successfully (Solo 6v6 + Duo Co-op)!");
    }

    public static com.cobblemon.mod.common.pokemon.Pokemon createTowerPokemon(com.vitwo.battle.TowerPokemon tp, int targetCap, int fallbackIndex, int floor) {
        String speciesName = "pikachu";
        if (tp != null && tp.getSpecies() != null && !tp.getSpecies().isBlank()) {
            speciesName = tp.getSpecies().toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9_\\-']", "_").replaceAll("__+", "_");
        } else {
            java.util.List<String> dynamicPool = com.vitwo.battle.TrainerPool.generateDynamicTeam(floor);
            if (dynamicPool != null && !dynamicPool.isEmpty()) {
                speciesName = dynamicPool.get(fallbackIndex % dynamicPool.size()).toLowerCase(Locale.ROOT);
            }
        }

        com.cobblemon.mod.common.pokemon.Pokemon mon = null;
        try {
            mon = com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse(speciesName + " level=" + targetCap).create();
        } catch (Throwable ignored) {}

        if (mon == null) {
            try {
                mon = com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse(speciesName).create();
            } catch (Throwable ignored) {}
        }

        if (mon == null) {
            mon = com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse("pikachu level=" + targetCap).create();
        }

        mon.setLevel(targetCap);
        applyTowerPropertiesToExisting(mon, tp);

        // Max 31 IVs
        for (com.cobblemon.mod.common.api.pokemon.stats.Stat stat : com.cobblemon.mod.common.api.pokemon.stats.Stats.Companion.getPERMANENT()) {
            mon.getIvs().set(stat, 31);
        }

        mon.heal();
        return mon;
    }

    public static void applyTowerPropertiesToExisting(com.cobblemon.mod.common.pokemon.Pokemon mon, com.vitwo.battle.TowerPokemon tp) {
        if (mon == null || tp == null) return;
        try {
            if (tp.isShiny()) {
                mon.setShiny(true);
            }
            if (tp.getNature() != null && !tp.getNature().isBlank()) {
                try {
                    com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse("nature=" + tp.getNature().toLowerCase(Locale.ROOT).trim()).apply(mon);
                } catch (Throwable ignored) {}
            }
            if (tp.getAbility() != null && !tp.getAbility().isBlank()) {
                try {
                    String cleanAbility = tp.getAbility().toLowerCase(Locale.ROOT).trim().replace(" ", "_").replace("-", "_");
                    com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse("ability=" + cleanAbility).apply(mon);
                } catch (Throwable ignored) {}
            }
            if (tp.getItem() != null && !tp.getItem().isBlank()) {
                try {
                    String cleanItem = tp.getItem().toLowerCase(Locale.ROOT).trim().replace(" ", "_").replace("-", "_");
                    com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse("item=" + cleanItem).apply(mon);
                } catch (Throwable ignored) {}
            }

            // Apply Moves individually
            if (tp.getMoves() != null && !tp.getMoves().isEmpty()) {
                try {
                    java.util.List<com.cobblemon.mod.common.api.moves.Move> customMoves = new java.util.ArrayList<>();
                    for (String moveName : tp.getMoves()) {
                        if (moveName == null || moveName.isBlank()) continue;
                        String cleanMove = moveName.toLowerCase(Locale.ROOT).trim().replace(" ", "_").replace("-", "_");
                        try {
                            com.cobblemon.mod.common.api.moves.MoveTemplate template = com.cobblemon.mod.common.api.moves.Moves.getByName(cleanMove);
                            if (template != null) {
                                customMoves.add(template.create());
                            }
                        } catch (Throwable ignored) {}
                    }
                    if (!customMoves.isEmpty()) {
                        mon.getMoveSet().clear();
                        for (com.cobblemon.mod.common.api.moves.Move m : customMoves) {
                            mon.getMoveSet().add(m);
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // Apply EVs
            if (tp.getEvs() != null && !tp.getEvs().isEmpty()) {
                for (com.cobblemon.mod.common.api.pokemon.stats.Stat stat : com.cobblemon.mod.common.api.pokemon.stats.Stats.Companion.getPERMANENT()) {
                    String statPath = stat.getIdentifier().getPath().toLowerCase(Locale.ROOT);
                    for (Map.Entry<String, Integer> ev : tp.getEvs().entrySet()) {
                        if (matchesStatName(statPath, ev.getKey().toLowerCase(Locale.ROOT))) {
                            mon.getEvs().set(stat, Math.min(252, Math.max(0, ev.getValue())));
                            break;
                        }
                    }
                }
            }
            mon.heal();
        } catch (Throwable ignored) {}
    }

    public static boolean matchesStatName(String statPath, String evKey) {
        if (statPath.equals(evKey)) return true;
        if (statPath.equals("hp") && evKey.equals("hp")) return true;
        if (statPath.equals("attack") && (evKey.equals("atk") || evKey.equals("attack"))) return true;
        if (statPath.equals("defence") && (evKey.equals("def") || evKey.equals("defense") || evKey.equals("defence"))) return true;
        if (statPath.equals("special_attack") && (evKey.equals("spa") || evKey.equals("spatk") || evKey.equals("special_attack"))) return true;
        if (statPath.equals("special_defence") && (evKey.equals("spd") || evKey.equals("spdef") || evKey.equals("special_defense") || evKey.equals("special_defence"))) return true;
        if (statPath.equals("speed") && (evKey.equals("spe") || evKey.equals("speed"))) return true;
        return false;
    }
}
