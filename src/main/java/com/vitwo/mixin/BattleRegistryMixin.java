package com.vitwo.mixin;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.api.battles.model.ai.BattleAI;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.vitwo.battle.HellModeTeamLoader;
import com.vitwo.battle.LevelCapManager;
import com.vitwo.battle.TrainerPool;
import com.vitwo.party.TowerParty;
import com.vitwo.party.TowerPartyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = BattleRegistry.class, remap = false)
public class BattleRegistryMixin {

    @ModifyVariable(method = "startBattle", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static BattleFormat vitwo$enforceTowerDoubleBattle(BattleFormat format, BattleFormat originalFormat, BattleSide side1, BattleSide side2, boolean start) {
        if (side1 == null || side2 == null) return format;

        // Check strictly for NPC Trainer actors (ActorType.NPC), EXCLUDING Wild Pokémon (ActorType.WILD)
        boolean hasNpcTrainer = false;
        for (BattleActor actor : side1.getActors()) {
            if (actor != null && actor.getType() == ActorType.NPC) {
                hasNpcTrainer = true;
                break;
            }
        }
        if (!hasNpcTrainer) {
            for (BattleActor actor : side2.getActors()) {
                if (actor != null && actor.getType() == ActorType.NPC) {
                    hasNpcTrainer = true;
                    break;
                }
            }
        }

        // 1. Find if any player on side1 or side2 is in an active Tower party
        TowerParty towerParty = null;
        ServerPlayerEntity towerPlayer = null;
        MinecraftServer server = TowerPartyManager.getInstance().getCurrentServer();
        if (server == null) {
            try {
                server = com.cobblemon.mod.common.util.DistributionUtilsKt.server();
                if (server != null) {
                    TowerPartyManager.getInstance().setCurrentServer(server);
                }
            } catch (Throwable ignored) {}
        }

        for (BattleSide side : new BattleSide[]{side1, side2}) {
            if (side == null || side.getActors() == null) continue;
            for (BattleActor actor : side.getActors()) {
                if (actor != null && (actor instanceof PlayerBattleActor || actor.getType() == ActorType.PLAYER)) {
                    for (UUID uuid : actor.getPlayerUUIDs()) {
                        if (server != null && towerPlayer == null) {
                            towerPlayer = server.getPlayerManager().getPlayer(uuid);
                        }
                        if (towerParty == null) {
                            Optional<TowerParty> pOpt = TowerPartyManager.getInstance().getParty(uuid);
                            if (pOpt.isPresent()) {
                                towerParty = pOpt.get();
                            }
                        }
                    }
                }
            }
            if (towerParty != null && towerPlayer != null) break;
        }

        // Check if player is currently inside the Tower Dimension
        boolean isInTowerDimension = false;
        if (towerPlayer != null && towerPlayer.getServerWorld() != null) {
            isInTowerDimension = towerPlayer.getServerWorld().getRegistryKey().getValue().getPath().contains("tower");
        } else if (towerParty != null && server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(towerParty.getLeaderId());
            if (leader != null && leader.getServerWorld() != null) {
                isInTowerDimension = leader.getServerWorld().getRegistryKey().getValue().getPath().contains("tower");
            }
        }

        BattleSide playerSide = null;
        BattleSide npcSide = null;

        for (BattleSide side : new BattleSide[]{side1, side2}) {
            if (side == null || side.getActors() == null) continue;
            for (BattleActor actor : side.getActors()) {
                if (actor != null && (actor instanceof PlayerBattleActor || actor.getType() == ActorType.PLAYER)) {
                    playerSide = side;
                } else if (actor != null && actor.getType() == ActorType.NPC) {
                    npcSide = side;
                }
            }
        }

        // Ensure ALL Player BattlePokemon enter EVERY battle (Overworld & Tower) with 100% Full HP & 100% Full PP (5/5)
        if (playerSide != null) {
            for (BattleActor actor : playerSide.getActors()) {
                if (actor == null) continue;
                for (BattlePokemon bp : actor.getPokemonList()) {
                    if (bp == null) continue;
                    if (bp.getEffectedPokemon() != null) {
                        com.vitwo.reward.TowerRewardManager.fullHeal(bp.getEffectedPokemon());
                    }
                    if (bp.getOriginalPokemon() != null) {
                        com.vitwo.reward.TowerRewardManager.fullHeal(bp.getOriginalPokemon());
                    }
                    if (bp.getMoveSet() != null) {
                        bp.getMoveSet().heal();
                        for (Move m : bp.getMoveSet()) {
                            if (m != null) {
                                m.setCurrentPp(m.getMaxPp());
                                m.update();
                            }
                        }
                        bp.getMoveSet().update();
                    }
                }
                for (UUID pId : actor.getPlayerUUIDs()) {
                    if (server != null) {
                        ServerPlayerEntity spe = server.getPlayerManager().getPlayer(pId);
                        if (spe != null) {
                            com.vitwo.reward.TowerRewardManager.getInstance().applyFullTeamRest(spe);
                        }
                    }
                }
            }
        }

        // If NOT in TowerDimension (Overworld):
        // Automatically inject fresh Hell Mode team into the NPC BattleActor in place!
        if (towerParty == null || !isInTowerDimension) {
            if (hasNpcTrainer && npcSide != null) {
                for (BattleActor actor : npcSide.getActors()) {
                    if (actor == null || actor.getType() != ActorType.NPC) continue;

                    String trainerId = extractTrainerId(actor);
                    if (trainerId != null && !trainerId.isBlank()) {
                        boolean applied = HellModeTeamLoader.applyHellModeTeamToActor(actor, trainerId, null);
                        if (!applied) {
                            for (String prefix : new String[]{"kanto_", "johto_", "hoenn_", "gym_leader_", "leader_", "champion_"}) {
                                if (!trainerId.startsWith(prefix)) {
                                    applied = HellModeTeamLoader.applyHellModeTeamToActor(actor, prefix + trainerId, null);
                                    if (applied) break;
                                }
                            }
                        }
                    }
                }
                return BattleFormat.Companion.getGEN_9_DOUBLES();
            }
            return format;
        }

        // =========================================================================
        // IN TOWER DIMENSION:
        // Load official 6-mon Hell Mode team from RCTMod scaled to floor Level Cap
        // =========================================================================
        int floor = towerParty.getCurrentFloor();
        int targetCap = LevelCapManager.getMaxLevelCapForFloor(floor);
        String chosenTrainerId = towerParty.getCurrentTrainerId();
        if (chosenTrainerId == null || chosenTrainerId.isBlank()) {
            chosenTrainerId = TrainerPool.getRctTrainerIdForFloor(floor);
        }

        if (towerParty.isSolo()) {
            // SOLO MODE (6v6 Double Battle)
            if (npcSide != null) {
                for (BattleActor actor : npcSide.getActors()) {
                    if (actor == null || actor.getType() != ActorType.NPC) continue;
                    boolean applied = HellModeTeamLoader.applyHellModeTeamToActor(actor, chosenTrainerId, targetCap);
                    if (!applied && !chosenTrainerId.contains("_")) {
                        HellModeTeamLoader.applyHellModeTeamToActor(actor, "kanto_" + chosenTrainerId, targetCap);
                    }
                    for (BattlePokemon bp : actor.getPokemonList()) {
                        if (bp != null) {
                            Pokemon mon = bp.getEffectedPokemon() != null ? bp.getEffectedPokemon() : bp.getOriginalPokemon();
                            if (mon != null) {
                                towerParty.recordEncounteredPokemon(mon);
                            }
                        }
                    }
                }
            }
            return BattleFormat.Companion.getGEN_9_DOUBLES();
        } else {
            // DUO CO-OP MODE (3+3 vs 3+3 Multi Battle)
            // 1. Setup Side 1 (Player Side): 2 PlayerBattleActors (Leader 3 mons, Member 3 mons)
            if (playerSide != null && server != null) {
                UUID leaderId = towerParty.getLeaderId();
                UUID memberId = towerParty.getMemberId();

                if (leaderId != null && memberId != null) {
                    ServerPlayerEntity leaderEntity = server.getPlayerManager().getPlayer(leaderId);
                    ServerPlayerEntity memberEntity = server.getPlayerManager().getPlayer(memberId);

                    if (leaderEntity != null && memberEntity != null) {
                        try {
                            var leaderStorage = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(leaderEntity);
                            var memberStorage = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(memberEntity);

                            List<BattlePokemon> leaderTeam = new ArrayList<>();
                            int count = 0;
                            if (leaderStorage != null) {
                                for (Pokemon mon : leaderStorage) {
                                    if (mon != null && count < 3) {
                                        leaderTeam.add(BattlePokemon.Companion.safeCopyOf(mon));
                                        count++;
                                    }
                                }
                            }

                            List<BattlePokemon> memberTeam = new ArrayList<>();
                            count = 0;
                            if (memberStorage != null) {
                                for (Pokemon mon : memberStorage) {
                                    if (mon != null && count < 3) {
                                        memberTeam.add(BattlePokemon.Companion.safeCopyOf(mon));
                                        count++;
                                    }
                                }
                            }

                            PlayerBattleActor leaderActor = new PlayerBattleActor(leaderId, leaderTeam);
                            for (BattlePokemon bp : leaderTeam) {
                                bp.setActor(leaderActor);
                            }

                            PlayerBattleActor memberActor = new PlayerBattleActor(memberId, memberTeam);
                            for (BattlePokemon bp : memberTeam) {
                                bp.setActor(memberActor);
                            }

                            ((BattleSideAccessor) (Object) playerSide).vitwo$setActors(new BattleActor[]{ leaderActor, memberActor });
                            org.slf4j.LoggerFactory.getLogger("CobbleTower-BattleRegistry").info("[CobbleTower] Successfully set up Duo Player Side with Leader {} and Member {}", leaderEntity.getName().getString(), memberEntity.getName().getString());
                        } catch (Throwable t) {
                            org.slf4j.LoggerFactory.getLogger("CobbleTower-BattleRegistry").error("[CobbleTower] Failed to set up Duo Player Side: {}", t.getMessage(), t);
                        }
                    }
                }
            }

            // 2. Setup Side 2 (NPC Side): Split Boss 6-mon team into 2 TrainerBattleActors (3 mons + 3 mons)
            if (npcSide != null && npcSide.getActors() != null && npcSide.getActors().length >= 1) {
                BattleActor primaryNpcActor = npcSide.getActors()[0];
                List<Pokemon> fullTeam = HellModeTeamLoader.createTeamFromTrainerId(chosenTrainerId, targetCap);
                if (fullTeam == null || fullTeam.isEmpty()) {
                    if (!chosenTrainerId.contains("_")) {
                        fullTeam = HellModeTeamLoader.createTeamFromTrainerId("kanto_" + chosenTrainerId, targetCap);
                    }
                }

                if (fullTeam != null && !fullTeam.isEmpty()) {
                    List<BattlePokemon> npcTeam1 = new ArrayList<>();
                    List<BattlePokemon> npcTeam2 = new ArrayList<>();

                    for (int i = 0; i < fullTeam.size(); i++) {
                        Pokemon pkm = fullTeam.get(i);
                        towerParty.recordEncounteredPokemon(pkm);
                        if (i < 3) {
                            npcTeam1.add(BattlePokemon.Companion.safeCopyOf(pkm));
                        } else if (i < 6) {
                            npcTeam2.add(BattlePokemon.Companion.safeCopyOf(pkm));
                        }
                    }

                    String bossName = primaryNpcActor.getName().getString();
                    BattleAI ai = (primaryNpcActor instanceof AIBattleActor aiba && aiba.getBattleAI() != null)
                            ? aiba.getBattleAI()
                            : new StrongBattleAI(100);

                    TrainerBattleActor npcActor1 = new TrainerBattleActor(bossName, UUID.randomUUID(), npcTeam1, ai);
                    for (BattlePokemon bp : npcTeam1) bp.setActor(npcActor1);

                    TrainerBattleActor npcActor2 = new TrainerBattleActor(bossName, UUID.randomUUID(), npcTeam2, ai);
                    for (BattlePokemon bp : npcTeam2) bp.setActor(npcActor2);

                    ((BattleSideAccessor) (Object) npcSide).vitwo$setActors(new BattleActor[]{ npcActor1, npcActor2 });
                    org.slf4j.LoggerFactory.getLogger("CobbleTower-BattleRegistry").info("[CobbleTower] Successfully split Boss {} into 2 Multi-Battle Actors (3+3)", bossName);
                }
            }

            // Return GEN_9_MULTI for Duo Co-op Battles so each player controls their own slot!
            return BattleFormat.Companion.getGEN_9_MULTI();
        }
    }

    private static String extractTrainerId(BattleActor actor) {
        if (actor == null) return null;
        try {
            if (actor instanceof EntityBackedBattleActor<?> entityActor) {
                Object entity = entityActor.getEntity();
                if (entity != null) {
                    try {
                        Method m = entity.getClass().getMethod("getTrainerId");
                        Object tid = m.invoke(entity);
                        if (tid instanceof String s && !s.isBlank()) {
                            return s;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        // Fallback: Check actor name
        try {
            String name = actor.getName().getString();
            if (name != null && !name.isBlank()) {
                String cleanName = name.toLowerCase(java.util.Locale.ROOT).trim();
                cleanName = cleanName.replace("leader ", "").replace("champion ", "").replace("elite four ", "").trim();
                return cleanName;
            }
        } catch (Throwable ignored) {}

        return null;
    }
}
