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
import java.util.UUID;

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



        ServerPlayNetworking.registerGlobalReceiver(com.vitwo.network.c2s.ChooseDraftPokemonC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive()) return;
            context.server().execute(() -> {
                com.vitwo.reward.TowerRewardManager.getInstance().handleDraftChoice(player, payload.floor(), payload.chosenSlotIndex());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(com.vitwo.network.c2s.ClaimGachaPokemonC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive()) return;
            context.server().execute(() -> {
                com.vitwo.reward.TowerRewardManager.getInstance().handleGachaPokemonClaim(player, payload);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(com.vitwo.network.c2s.ClaimItemGachaC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive()) return;
            context.server().execute(() -> {
                com.vitwo.reward.TowerRewardManager.getInstance().handleGachaItemClaim(player, payload);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(com.vitwo.network.c2s.RequestHubSyncC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (player == null || !player.isAlive()) return;
            context.server().execute(() -> {
                TowerPartyManager.getInstance().syncPlayerState(player);
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



                    if (world.getRegistryKey().getValue().getPath().contains("tower")) {
                        try {
                            // Orient Trainer NPC to face player directly
                            float faceYaw = serverPlayer.getYaw() + 180.0f;
                            entity.setYaw(faceYaw);
                            entity.setHeadYaw(faceYaw);
                            entity.setBodyYaw(faceYaw);
                            entity.setPitch(0.0f);

                            Optional<TowerParty> partyOpt = TowerPartyManager.getInstance().getParty(serverPlayer.getUuid());
                            if (partyOpt.isPresent()) {
                                TowerParty party = partyOpt.get();
                                int floor = party.getCurrentFloor();

                                if (!party.isSolo() && party.getMemberId() != null) {
                                    // DUO CO-OP READY SYSTEM (1/2 -> 2/2)
                                    if (party.isReady(serverPlayer.getUuid())) {
                                        serverPlayer.sendMessage(net.minecraft.text.Text.literal("§b[CobbleTower] §aYou are ready (1/2)! §7Waiting for partner to right-click Trainer NPC..."), false);
                                        return ActionResult.SUCCESS;
                                    }

                                    party.setReady(serverPlayer.getUuid(), true);
                                    UUID otherId = party.getOtherPlayer(serverPlayer.getUuid());
                                    ServerPlayerEntity otherPlayer = serverPlayer.getServer() != null ? serverPlayer.getServer().getPlayerManager().getPlayer(otherId) : null;

                                    if (!party.areBothReady()) {
                                        serverPlayer.sendMessage(net.minecraft.text.Text.literal("§b[CobbleTower] §aReady! §7(1/2) §f— Waiting for partner to right-click NPC."), false);
                                        serverPlayer.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0f, 1.2f);
                                        if (otherPlayer != null) {
                                            otherPlayer.sendMessage(net.minecraft.text.Text.literal("§b[CobbleTower] §e" + serverPlayer.getName().getString() + " §ais ready (1/2)! §fRight-click Trainer NPC to begin Co-op battle!"), false);
                                            otherPlayer.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.0f);
                                        }
                                        return ActionResult.SUCCESS;
                                    } else {
                                        // Both players are ready (2/2) -> launch multi-actor battle!
                                        party.clearReady();
                                        ServerPlayerEntity leader = serverPlayer.getServer().getPlayerManager().getPlayer(party.getLeaderId());
                                        ServerPlayerEntity member = serverPlayer.getServer().getPlayerManager().getPlayer(party.getMemberId());
                                        if (leader != null && member != null) {
                                            leader.sendMessage(net.minecraft.text.Text.literal("§b[CobbleTower] §aBoth players ready (2/2)! Launching Co-op Duo 3+3 Battle!"), false);
                                            member.sendMessage(net.minecraft.text.Text.literal("§b[CobbleTower] §aBoth players ready (2/2)! Launching Co-op Duo 3+3 Battle!"), false);
                                            leader.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
                                            member.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);

                                            boolean started = com.vitwo.battle.TowerBattleManager.getInstance().triggerDuoBattle(party, leader, member, entity, floor);
                                            if (started) return ActionResult.SUCCESS;
                                        }
                                    }
                                } else {
                                    // SOLO MODE: Apply downscale & launch battle
                                    com.vitwo.battle.LevelCapManager.applyLevelCapToPlayer(serverPlayer, floor, party);
                                }
                            }

                            // Fallback / Solo Trigger via RCTModAdapter
                            boolean success = com.vitwo.battle.RCTModAdapter.startBattle(entity, serverPlayer);
                            if (success) {
                                return ActionResult.SUCCESS;
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
            com.vitwo.config.TowerPersistenceService.getInstance().flushAllSync();
            com.vitwo.config.TowerPlayerDataManager.getInstance().clearCache();
            TowerPartyManager.getInstance().setCurrentServer(null);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> TowerPartyManager.getInstance().tick(server));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            TowerPartyManager.getInstance().handleDisconnect(handler.getPlayer());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            TowerPartyManager.getInstance().setCurrentServer(server);
            TowerPartyManager.getInstance().handleReconnect(handler.getPlayer());
            com.vitwo.config.TowerPlayerDataManager.getInstance().checkZitjCompensation(handler.getPlayer());
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
            String trainerId = com.vitwo.battle.TrainerPool.getRctTrainerIdForFloor(floor);
            var team = com.vitwo.battle.HellModeTeamLoader.createTeamFromTrainerId(trainerId, targetCap);
            if (team != null && !team.isEmpty()) {
                return team.get(fallbackIndex % team.size());
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

        com.vitwo.reward.TowerRewardManager.fullHeal(mon);
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
            com.vitwo.reward.TowerRewardManager.fullHeal(mon);
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
