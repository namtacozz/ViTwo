package com.vitwo.party;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.vitwo.arena.TowerArenaManager;
import com.vitwo.battle.LevelCapManager;
import com.vitwo.battle.TowerBattleManager;
import com.vitwo.config.TowerPlayerDataManager;
import com.vitwo.network.s2c.OpenRunSummaryS2CPacket;
import com.vitwo.network.s2c.SyncPartyStateS2CPacket;
import com.vitwo.network.s2c.TowerTitleS2CPacket;
import com.vitwo.reward.TowerRewardManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TowerPartyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-PartyManager");
    private static final TowerPartyManager INSTANCE = new TowerPartyManager();
    public static TowerPartyManager getInstance() { return INSTANCE; }

    private final Map<UUID, TowerParty> activeParties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToPartyLeader = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingInviterNames = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pendingInviteTimestamps = new ConcurrentHashMap<>();
    private MinecraftServer currentServer;

    private TowerPartyManager() {}

    public void setCurrentServer(MinecraftServer server) {
        this.currentServer = server;
    }

    public MinecraftServer getCurrentServer() {
        return currentServer;
    }

    public int getSoloCheckpoint(UUID playerId) {
        return TowerPlayerDataManager.getInstance().getProfile(playerId).soloCheckpoint;
    }

    public void updateSoloCheckpoint(UUID playerId, int newFloor) {
        if (TowerParty.isCheckpointFloor(newFloor)) {
            TowerPlayerDataManager.getInstance().updateSoloCheckpoint(playerId, newFloor);
        }
    }

    public int getDuoCheckpoint(UUID playerId) {
        return TowerPlayerDataManager.getInstance().getProfile(playerId).duoCheckpoint;
    }

    public void updateDuoCheckpoint(UUID playerId, int newFloor) {
        if (TowerParty.isCheckpointFloor(newFloor)) {
            TowerPlayerDataManager.getInstance().updateDuoCheckpoint(playerId, newFloor);
        }
    }

    public Optional<TowerParty> getParty(UUID playerId) {
        UUID leaderId = playerToPartyLeader.get(playerId);
        if (leaderId != null) {
            return Optional.ofNullable(activeParties.get(leaderId));
        }
        return Optional.ofNullable(activeParties.get(playerId));
    }

    public TowerParty createSoloParty(ServerPlayerEntity player, int startingCheckpoint) {
        TowerParty party = new TowerParty(player.getUuid(), startingCheckpoint);
        activeParties.put(player.getUuid(), party);
        playerToPartyLeader.put(player.getUuid(), player.getUuid());
        syncPlayerState(player);
        return party;
    }

    public void invitePlayer(ServerPlayerEntity sender, UUID targetId) {
        if (sender.getUuid().equals(targetId)) return;

        Optional<TowerParty> existingParty = getParty(sender.getUuid());
        if (existingParty.isPresent() && !existingParty.get().isSolo()) {
            sender.sendMessage(Text.translatable("vitwo.tower.party_full"), false);
            return;
        }

        pendingInvites.put(targetId, sender.getUuid());
        pendingInviterNames.put(targetId, sender.getName().getString());
        pendingInviteTimestamps.put(targetId, System.currentTimeMillis());
        sender.sendMessage(Text.translatable("vitwo.tower.invited", targetId.toString()), false);

        ServerPlayerEntity targetPlayer = sender.getServer().getPlayerManager().getPlayer(targetId);
        if (targetPlayer != null) {
            targetPlayer.sendMessage(Text.translatable("vitwo.tower.invite_received", sender.getName().getString()), false);
            syncPlayerState(targetPlayer);
        }
    }

    public void acceptInvite(ServerPlayerEntity player) {
        UUID senderId = pendingInvites.remove(player.getUuid());
        pendingInviterNames.remove(player.getUuid());
        pendingInviteTimestamps.remove(player.getUuid());

        if (senderId == null) {
            player.sendMessage(Text.translatable("vitwo.tower.no_party"), false);
            return;
        }

        ServerPlayerEntity leader = player.getServer().getPlayerManager().getPlayer(senderId);
        if (leader == null) {
            player.sendMessage(Text.translatable("vitwo.tower.no_party"), false);
            return;
        }

        int startCp = Math.min(getDuoCheckpoint(senderId), getDuoCheckpoint(player.getUuid()));
        TowerParty party = new TowerParty(senderId, player.getUuid(), startCp);
        activeParties.put(senderId, party);
        playerToPartyLeader.put(senderId, senderId);
        playerToPartyLeader.put(player.getUuid(), senderId);

        leader.sendMessage(Text.translatable("vitwo.tower.invite_accepted", player.getName().getString()), false);
        player.sendMessage(Text.translatable("vitwo.tower.invite_accepted", leader.getName().getString()), false);

        syncParty(party, player.getServer());
    }

    public void declineInvite(ServerPlayerEntity player) {
        UUID senderId = pendingInvites.remove(player.getUuid());
        pendingInviterNames.remove(player.getUuid());
        pendingInviteTimestamps.remove(player.getUuid());

        if (senderId != null) {
            ServerPlayerEntity leader = player.getServer().getPlayerManager().getPlayer(senderId);
            if (leader != null) {
                leader.sendMessage(Text.translatable("vitwo.tower.invite_declined", player.getName().getString()), false);
            }
        }
        syncPlayerState(player);
    }

    public void invitePlayer(ServerPlayerEntity sender, ServerPlayerEntity target) {
        if (target != null) {
            invitePlayer(sender, target.getUuid());
        }
    }

    public void respondInvite(ServerPlayerEntity player, boolean accept) {
        if (accept) {
            acceptInvite(player);
        } else {
            declineInvite(player);
        }
    }

    public void leaveParty(ServerPlayerEntity player) {
        leaveParty(player, true);
    }

    public void leaveParty(ServerPlayerEntity player, boolean isLeader) {
        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isEmpty()) return;

        TowerParty party = partyOpt.get();
        MinecraftServer server = player.getServer();

        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        disbandParty(party);

        if (leader != null) leader.sendMessage(Text.translatable("vitwo.tower.party_disbanded"), false);
        if (member != null) member.sendMessage(Text.translatable("vitwo.tower.party_disbanded"), false);

        if (leader != null) syncPlayerState(leader);
        if (member != null) syncPlayerState(member);
    }

    public void startTowerSession(TowerParty party, boolean isSolo, int checkpointFloor, MinecraftServer server) {
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        if (leader == null) return;

        activeParties.put(party.getLeaderId(), party);
        playerToPartyLeader.put(party.getLeaderId(), party.getLeaderId());
        if (party.getMemberId() != null) {
            playerToPartyLeader.put(party.getMemberId(), party.getLeaderId());
        }

        // Level Cap Validation
        if (!LevelCapManager.isPlayerEligible(leader, checkpointFloor)) {
            int maxCap = LevelCapManager.getMaxLevelCapForFloor(checkpointFloor);
            leader.sendMessage(Text.translatable("vitwo.tower.level_cap_exceeded", maxCap), false);
            return;
        }

        // Regional Readiness Check
        if (checkpointFloor == 1 && !isPlayerKantoReady(leader)) {
            leader.sendMessage(Text.literal("§c[CobbleTower] The Tower Gateway is sealed! You must conquer the Kanto Region Gyms first to unlock CobbleTower."), false);
            return;
        }

        // Competitive Clause Validation (Species, Items, Legendary limit)
        com.vitwo.battle.TowerClauseManager.ValidationResult leaderClause = com.vitwo.battle.TowerClauseManager.getInstance().validateTeam(leader, checkpointFloor);
        if (!leaderClause.valid) {
            leader.sendMessage(Text.literal(leaderClause.errorMessage), false);
            return;
        }

        party.rollNewAttempt();
        if (isSolo) {
            int maxCp = getSoloCheckpoint(leader.getUuid());
            if (checkpointFloor > maxCp) checkpointFloor = maxCp;

            party.setCurrentFloor(checkpointFloor);
            party.setHighestCheckpoint(maxCp);
            party.setState(TowerParty.State.PREPARING);
            String leaderDim = leader.getServerWorld().getRegistryKey().getValue().toString();
            party.setOriginalLeaderExact(leaderDim, leader.getX(), leader.getY(), leader.getZ(), leader.getYaw(), leader.getPitch());
            party.clearForfeitVotes();

            startFloor(party, server);
        } else {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(party.getMemberId());
            if (member == null) return;

            if (!LevelCapManager.isPlayerEligible(member, checkpointFloor)) {
                int maxCap = LevelCapManager.getMaxLevelCapForFloor(checkpointFloor);
                leader.sendMessage(Text.translatable("vitwo.tower.level_cap_exceeded", maxCap), false);
                member.sendMessage(Text.translatable("vitwo.tower.level_cap_exceeded", maxCap), false);
                return;
            }

            com.vitwo.battle.TowerClauseManager.ValidationResult memberClause = com.vitwo.battle.TowerClauseManager.getInstance().validateTeam(member, checkpointFloor);
            if (!memberClause.valid) {
                leader.sendMessage(Text.literal("§cPartner's team invalid: " + memberClause.errorMessage), false);
                member.sendMessage(Text.literal(memberClause.errorMessage), false);
                return;
            }

            int effectiveCp = Math.min(getDuoCheckpoint(leader.getUuid()), getDuoCheckpoint(member.getUuid()));
            if (checkpointFloor > effectiveCp) checkpointFloor = effectiveCp;

            party.setCurrentFloor(checkpointFloor);
            party.setHighestCheckpoint(effectiveCp);
            party.setState(TowerParty.State.PREPARING);
            String leaderDim = leader.getServerWorld().getRegistryKey().getValue().toString();
            String memberDim = member.getServerWorld().getRegistryKey().getValue().toString();
            party.setOriginalLeaderExact(leaderDim, leader.getX(), leader.getY(), leader.getZ(), leader.getYaw(), leader.getPitch());
            party.setOriginalMemberExact(memberDim, member.getX(), member.getY(), member.getZ(), member.getYaw(), member.getPitch());
            party.clearForfeitVotes();

            startFloor(party, server);
        }
    }

    public void registerRestoredParty(TowerParty party, MinecraftServer server) {
        activeParties.put(party.getLeaderId(), party);
        playerToPartyLeader.put(party.getLeaderId(), party.getLeaderId());
        if (party.getMemberId() != null) {
            playerToPartyLeader.put(party.getMemberId(), party.getLeaderId());
        }
        party.setState(TowerParty.State.IN_BATTLE);
        syncParty(party, server);
    }

    public void startFloor(TowerParty party, MinecraftServer server) {
        int floor = party.getCurrentFloor();
        party.setState(TowerParty.State.IN_BATTLE);
        party.clearSpectators();
        party.clearForfeitVotes();
        party.clearReady();
        party.resetStallTicks();
        party.rollFloorTrainer(floor);

        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        if (leader == null) return;

        // Auto-downscale levels for party members
        LevelCapManager.applyLevelCapToPlayer(leader, floor, party);
        if (member != null) {
            LevelCapManager.applyLevelCapToPlayer(member, floor, party);
        }

        // Ensure 100% Full Team Rest (HP, PP, status) at the start of every floor
        TowerRewardManager.getInstance().applyFullTeamRest(leader);
        if (member != null) {
            TowerRewardManager.getInstance().applyFullTeamRest(member);
        }

        party.setState(TowerParty.State.PREPARING);

        // Teleport party to fresh floor arena and barrier boundary
        TowerArenaManager.getInstance().teleportPartyToArena(party, leader, member);

        String bossName = party.getCurrentBossName();
        int maxCap = LevelCapManager.getMaxLevelCapForFloor(floor);

        String titleSub;
        if (floor >= 100) {
            titleSub = "§6§l★ FLOOR 100: SUPREME CHAMPION CYNTHIA ★ §7(Cap: Lv.100)";
        } else if (floor >= 90) {
            titleSub = "§d§l✦ FLOOR " + floor + ": " + bossName.toUpperCase() + " ✦ §7(Cap: Lv." + maxCap + ")";
        } else if (floor % 5 == 0) {
            titleSub = "§e§l⬡ FLOOR " + floor + ": GYM LEADER " + bossName.toUpperCase() + " ⬡ §7(Cap: Lv." + maxCap + ")";
        } else {
            titleSub = "§b§lFLOOR " + floor + "/100 §7— Opponent: §f" + bossName + " §7(Cap: Lv." + maxCap + ")";
        }

        TowerTitleS2CPacket titlePayload = new TowerTitleS2CPacket(
                "§b§lCOBBLE TOWER",
                titleSub,
                floor,
                bossName
        );

        ServerPlayNetworking.send(leader, titlePayload);
        if (member != null) ServerPlayNetworking.send(member, titlePayload);

        // Send Floor Entrance Chat Messages
        leader.sendMessage(Text.literal("§6[CobbleTower] §fEntering §bFloor " + floor + "/100§f — Opponent: §e" + bossName + " §7(Level Cap: §aLv." + maxCap + "§7)"), false);
        if (floor >= 100) {
            leader.sendMessage(Text.literal("§6§l★ [COBBLE TOWER] SUPREME CHAMPION CYNTHIA! Defeat her to conquer the Tower! ★"), false);
        } else if (floor >= 90) {
            leader.sendMessage(Text.literal("§d[CobbleTower] §l✦ ELITE FOUR & CHAMPION! §fChallenging the legendary §e" + bossName + "§f!"), false);
        } else if (floor % 5 == 0) {
            leader.sendMessage(Text.literal("§6[CobbleTower] §l⚡ GYM LEADER BATTLE! §fChallenging Gym Leader §e" + bossName + "§f!"), false);
        }
        leader.sendMessage(Text.literal("§b[CobbleTower] §fRight-click Trainer NPC to begin battle!"), false);

        if (member != null) {
            member.sendMessage(Text.literal("§6[CobbleTower] §fEntering §bFloor " + floor + "/100§f — Opponent: §e" + bossName + " §7(Level Cap: §aLv." + maxCap + "§7)"), false);
            if (floor >= 100) {
                member.sendMessage(Text.literal("§6§l★ [COBBLE TOWER] SUPREME CHAMPION CYNTHIA! Defeat her to conquer the Tower! ★"), false);
            } else if (floor >= 90) {
                member.sendMessage(Text.literal("§d[CobbleTower] §l✦ ELITE FOUR & CHAMPION! §fChallenging the legendary §e" + bossName + "§f!"), false);
            } else if (floor % 5 == 0) {
                member.sendMessage(Text.literal("§6[CobbleTower] §l⚡ GYM LEADER BATTLE! §fChallenging Gym Leader §e" + bossName + "§f!"), false);
            }
            member.sendMessage(Text.literal("§b[CobbleTower] §fRight-click Trainer NPC to begin battle!"), false);
        }

        // Trigger Boss Cutscene for key milestone floors
        boolean isBossFloor = (floor == 10 || floor == 25 || floor == 50 || floor == 75 || floor == 90 || floor >= 91);
        if (isBossFloor) {
            String bossTitle = floor >= 100 ? "Genesis Deity" : (floor >= 91 ? "Primal Sovereign" : "Tower Sentinel");
            String quote = floor >= 100 ? "The cosmos kneels before the divine sovereign." : (floor >= 91 ? "Witness the primal majesty of ancient titans." : "Only the strongest trainers may ascend past this summit.");
            com.vitwo.network.s2c.TowerBossIntroS2CPacket intro = new com.vitwo.network.s2c.TowerBossIntroS2CPacket(floor, bossName, bossTitle, quote, floor >= 91);
            ServerPlayNetworking.send(leader, intro);
            if (member != null) ServerPlayNetworking.send(member, intro);
        }

        // Announce active floor curse
        String curseMsg = com.vitwo.battle.TowerCurseManager.getInstance().getCurseNotification(floor);
        if (!curseMsg.isEmpty()) {
            leader.sendMessage(Text.literal(curseMsg), false);
            if (member != null) member.sendMessage(Text.literal(curseMsg), false);
        }

        if (party.isSolo() || member == null) {
            TowerBattleManager.getInstance().startSoloDoubleBattle(party, leader, floor);
        } else {
            TowerBattleManager.getInstance().startDuoDoubleBattle(party, leader, member, floor);
        }

        // Run Persistence
        TowerRunPersistenceManager.getInstance().saveRun(party);

        syncParty(party, server);
    }

    public void onFloorWon(TowerParty party, MinecraftServer server) {
        int clearedFloor = party.getCurrentFloor();
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        if (party.isSolo() && leader != null) {
            updateSoloCheckpoint(leader.getUuid(), clearedFloor + 1);
            TowerPlayerDataManager.getInstance().recordFloorCleared(leader.getUuid(), clearedFloor, party.isTrueRun());
        } else if (leader != null && member != null) {
            updateDuoCheckpoint(leader.getUuid(), clearedFloor + 1);
            updateDuoCheckpoint(member.getUuid(), clearedFloor + 1);
            TowerPlayerDataManager.getInstance().recordFloorCleared(leader.getUuid(), clearedFloor, party.isTrueRun());
            TowerPlayerDataManager.getInstance().recordFloorCleared(member.getUuid(), clearedFloor, party.isTrueRun());
        }

        int turnsThisFloor = 3;
        int faintsThisFloor = 0;
        party.incrementTurns(turnsThisFloor);
        party.resetStallTicks();
        party.resetHardStallTicks();
        party.resetNoBattleTicks();

        String playerNames = leader != null ? leader.getName().getString() : "Leader";
        if (member != null) {
            playerNames += " & " + member.getName().getString();
        }
        int turns = party.getTurnsElapsed();
        int duration = party.getDurationSeconds();
        int faints = party.getFaintsCount();

        // Update real-time Leaderboard with this cleared floor
        com.vitwo.config.TowerLeaderboardManager.getInstance().recordProgress(playerNames, party.isSolo(), clearedFloor, duration, turns, faints);

        // 1. Grant Generous Floor Rewards (BP + Rich Minecraft Resources + Candies)
        TowerRewardManager.getInstance().grantFloorReward(leader, member, clearedFloor, party.isTrueRun(), turnsThisFloor, faintsThisFloor);

        // 2. Full Team Heal (100% HP, 100% PP & Clear Status) after EVERY floor!
        if (leader != null) TowerRewardManager.getInstance().applyFullTeamRest(leader);
        if (member != null) TowerRewardManager.getInstance().applyFullTeamRest(member);

        // 3. Trigger CS:GO Pokemon Gacha on special floors (%5, 90-99, 100) or Item/BP Gacha on regular floors
        if (clearedFloor % 5 == 0 || clearedFloor >= 90) {
            TowerRewardManager.getInstance().triggerPokemonGacha(party, server, clearedFloor);
        } else {
            TowerRewardManager.getInstance().triggerItemGacha(party, server, clearedFloor);
        }

        // 4. Send Floor Victory Title
        TowerTitleS2CPacket winTitle = new TowerTitleS2CPacket(
                "§a§lFLOOR " + clearedFloor + " CLEARED!",
                "§eHealed 100% HP/PP • Received Rewards • Ascending to Floor " + (clearedFloor + 1) + "!",
                clearedFloor,
                ""
        );
        if (leader != null) {
            ServerPlayNetworking.send(leader, winTitle);
            leader.sendMessage(Text.literal("§a[CobbleTower] §l✔ FLOOR " + clearedFloor + " CLEARED! §7(100% HP/PP Restored & Rewards Granted)"), false);
            if (clearedFloor < 100) {
                leader.sendMessage(Text.literal("§b[CobbleTower] §fPreparing ascent to §eFloor " + (clearedFloor + 1) + "§f..."), false);
            }
        }
        if (member != null) {
            ServerPlayNetworking.send(member, winTitle);
            member.sendMessage(Text.literal("§a[CobbleTower] §l✔ FLOOR " + clearedFloor + " CLEARED! §7(100% HP/PP Restored & Rewards Granted)"), false);
            if (clearedFloor < 100) {
                member.sendMessage(Text.literal("§b[CobbleTower] §fPreparing ascent to §eFloor " + (clearedFloor + 1) + "§f..."), false);
            }
        }

        if (clearedFloor >= 100) {
            party.setState(TowerParty.State.COMPLETED);

            int bpEarned = party.isTrueRun() ? 10000 : 5000;

            if (party.isTrueRun()) {
                com.vitwo.config.TowerLeaderboardManager.getInstance().recordCompletion(playerNames, party.isSolo(), duration, turns, faints);
            }

            // Global server broadcast
            server.getPlayerManager().broadcast(Text.literal("§6§l★ [COBBLE TOWER] §eChallenger §b" + playerNames + " §ehas conquered §6ALL 100 FLOORS §eand defeated Champion Cynthia! ★"), false);

            if (leader != null) {
                TowerPlayerDataManager.getInstance().recordRunResult(leader.getUuid(), 100, party.isTrueRun(), turns, duration, true);
                ServerPlayNetworking.send(leader, new OpenRunSummaryS2CPacket(100, true, party.isTrueRun(), duration, turns, faints, bpEarned, 100));
                TowerArenaManager.getInstance().returnPlayerToOriginalPos(leader, party.getOriginalLeaderPos());
                server.execute(() -> LevelCapManager.restorePlayerLevels(leader, party));
            }
            if (member != null) {
                TowerPlayerDataManager.getInstance().recordRunResult(member.getUuid(), 100, party.isTrueRun(), turns, duration, true);
                ServerPlayNetworking.send(member, new OpenRunSummaryS2CPacket(100, true, party.isTrueRun(), duration, turns, faints, bpEarned, 100));
                TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
                server.execute(() -> LevelCapManager.restorePlayerLevels(member, party));
            }

            TowerRunPersistenceManager.getInstance().deleteRun(party.getLeaderId());
            TowerArenaManager.getInstance().cleanupFloorArena(party, server);
            disbandParty(party);
            if (leader != null) syncPlayerState(leader);
            if (member != null) syncPlayerState(member);
            return;
        }

        // Progress directly to the next floor! (Every floor 1-99 is a Battle Tower floor, no rest floor pauses)
        party.setCurrentFloor(clearedFloor + 1);
        startFloor(party, server);
    }

    public static void terminateActiveBattleForPlayer(ServerPlayerEntity player) {
        if (player == null) return;
        // STRICT DIMENSION ISOLATION: Never terminate battles in the Overworld!
        if (player.getServerWorld() == null || !player.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
            return;
        }
        try {
            var battle = com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(player);
            if (battle != null) {
                battle.stop();
                com.cobblemon.mod.common.battles.BattleRegistry.closeBattle(battle);
            }
        } catch (Throwable ignored) {}
    }

    public void handleForfeitVote(ServerPlayerEntity player) {
        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isEmpty()) {
            if (player.getServerWorld() != null && player.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
                terminateActiveBattleForPlayer(player);
                TowerArenaManager.getInstance().returnPlayerToOriginalPos(player, null);
                player.sendMessage(Text.literal("§e[CobbleTower] Returned to Overworld."), false);
                syncPlayerState(player);
            }
            return;
        }

        TowerParty party = partyOpt.get();
        boolean shouldForfeit = party.isSolo() || party.voteForfeit(player.getUuid());

        if (shouldForfeit) {
            forfeitTower(party.getLeaderId(), player.getServer());
        } else {
            UUID otherId = party.getOtherPlayer(player.getUuid());
            if (otherId != null && player.getServer() != null) {
                ServerPlayerEntity other = player.getServer().getPlayerManager().getPlayer(otherId);
                if (other != null) {
                    other.sendMessage(Text.literal("§e[CobbleTower] §fPartner voted to forfeit (1/2). Open [Y] or use /tower forfeit to confirm."), false);
                }
            }
            if (player.getServer() != null) {
                syncParty(party, player.getServer());
            }
        }
    }

    public void forfeitTower(UUID playerId, MinecraftServer server) {
        Optional<TowerParty> partyOpt = getParty(playerId);
        if (partyOpt.isEmpty()) return;

        TowerParty party = partyOpt.get();
        if (server == null) return;

        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        // Terminate any active battle immediately
        terminateActiveBattleForPlayer(leader);
        terminateActiveBattleForPlayer(member);

        party.setState(TowerParty.State.LOBBY);

        int floor = party.getCurrentFloor();
        int turns = party.getTurnsElapsed();
        int duration = party.getDurationSeconds();
        int faints = party.getFaintsCount();

        if (leader != null) {
            leader.playSound(SoundEvents.ENTITY_WITHER_DEATH, 0.7f, 0.8f);
            TowerPlayerDataManager.getInstance().recordRunResult(leader.getUuid(), floor, party.isTrueRun(), turns, duration, false);
            ServerPlayNetworking.send(leader, new OpenRunSummaryS2CPacket(floor, false, party.isTrueRun(), duration, turns, faints, 0, floor));
            leader.sendMessage(Text.translatable("vitwo.tower.forfeited"), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(leader, party.getOriginalLeaderPos());
            server.execute(() -> LevelCapManager.restorePlayerLevels(leader, party));
        }
        if (member != null) {
            member.playSound(SoundEvents.ENTITY_WITHER_DEATH, 0.7f, 0.8f);
            TowerPlayerDataManager.getInstance().recordRunResult(member.getUuid(), floor, party.isTrueRun(), turns, duration, false);
            ServerPlayNetworking.send(member, new OpenRunSummaryS2CPacket(floor, false, party.isTrueRun(), duration, turns, faints, 0, floor));
            member.sendMessage(Text.translatable("vitwo.tower.forfeited"), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
            server.execute(() -> LevelCapManager.restorePlayerLevels(member, party));
        }

        TowerRunPersistenceManager.getInstance().deleteRun(party.getLeaderId());
        TowerArenaManager.getInstance().cleanupFloorArena(party, server);
        disbandParty(party);
        if (leader != null) syncPlayerState(leader);
        if (member != null) syncPlayerState(member);
    }

    /**
     * Handles party defeat when all player Pokémon faint in battle.
     */
    public void onPartyDefeated(TowerParty party, MinecraftServer server) {
        int failedFloor = party.getCurrentFloor();
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        // Terminate any active battle immediately
        terminateActiveBattleForPlayer(leader);
        terminateActiveBattleForPlayer(member);

        party.setState(TowerParty.State.LOBBY);
        party.incrementFaints(6);

        int turns = party.getTurnsElapsed();
        int duration = party.getDurationSeconds();
        int faints = party.getFaintsCount();

        if (leader != null) {
            TowerPlayerDataManager.getInstance().recordRunResult(leader.getUuid(), failedFloor, party.isTrueRun(), turns, duration, false);
            ServerPlayNetworking.send(leader, new OpenRunSummaryS2CPacket(failedFloor, false, party.isTrueRun(), duration, turns, faints, 0, failedFloor));
            leader.sendMessage(Text.translatable("vitwo.tower.defeat", failedFloor), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(leader, party.getOriginalLeaderPos());
            server.execute(() -> LevelCapManager.restorePlayerLevels(leader, party));
        }
        if (member != null) {
            TowerPlayerDataManager.getInstance().recordRunResult(member.getUuid(), failedFloor, party.isTrueRun(), turns, duration, false);
            ServerPlayNetworking.send(member, new OpenRunSummaryS2CPacket(failedFloor, false, party.isTrueRun(), duration, turns, faints, 0, failedFloor));
            member.sendMessage(Text.translatable("vitwo.tower.defeat", failedFloor), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
            server.execute(() -> LevelCapManager.restorePlayerLevels(member, party));
        }

        TowerRunPersistenceManager.getInstance().deleteRun(party.getLeaderId());
        TowerArenaManager.getInstance().cleanupFloorArena(party, server);

        disbandParty(party);
        if (leader != null) syncPlayerState(leader);
        if (member != null) syncPlayerState(member);
    }

    public void handleDisconnect(ServerPlayerEntity player) {
        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isEmpty()) return;

        TowerParty party = partyOpt.get();
        TowerRunPersistenceManager.getInstance().saveRun(party);

        if (party.isSolo()) {
            return;
        }

        party.handlePlayerDisconnect(player.getUuid());
        UUID otherId = party.getOtherPlayer(player.getUuid());
        if (otherId != null && player.getServer() != null) {
            ServerPlayerEntity otherPlayer = player.getServer().getPlayerManager().getPlayer(otherId);
            if (otherPlayer != null) {
                otherPlayer.sendMessage(Text.translatable("vitwo.tower.reconnect_wait", 180), false);
            }
        }
    }

    public void handleReconnect(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) return;
        this.currentServer = player.getServer();

        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isEmpty()) {
            boolean inTowerDim = player.getServerWorld() != null && player.getServerWorld().getRegistryKey().getValue().getPath().contains("tower");
            if (inTowerDim) {
                boolean restored = TowerRunPersistenceManager.getInstance().restoreRun(player, player.getServer());
                if (restored) return;
                int soloCp = getSoloCheckpoint(player.getUuid());
                TowerParty recoveredParty = new TowerParty(player.getUuid(), soloCp);
                recoveredParty.setSolo(true);
                recoveredParty.setCurrentFloor(soloCp);
                recoveredParty.setHighestCheckpoint(soloCp);
                recoveredParty.rollFloorTrainer(soloCp);
                recoveredParty.setState(TowerParty.State.IN_BATTLE);
                registerRestoredParty(recoveredParty, player.getServer());
                TowerRunPersistenceManager.getInstance().saveRun(recoveredParty);
                return;
            } else {
                // If player is in Overworld but has dangling run data, failsafe restore their levels and delete dangling run
                var runOpt = TowerRunPersistenceManager.getInstance().getActiveRun(player.getUuid());
                if (runOpt.isPresent()) {
                    LevelCapManager.restorePlayerLevelsFromRunData(player, runOpt.get());
                    TowerRunPersistenceManager.getInstance().deleteRun(player.getUuid());
                }
                syncPlayerState(player);
                return;
            }
        }

        TowerParty party = partyOpt.get();
        if (party.getDisconnectedPlayerId() != null && player.getUuid().equals(party.getDisconnectedPlayerId())) {
            party.handlePlayerReconnect(player.getUuid());
            player.sendMessage(Text.translatable("vitwo.tower.reconnect_success"), false);

            UUID otherId = party.getOtherPlayer(player.getUuid());
            if (otherId != null && player.getServer() != null) {
                ServerPlayerEntity otherPlayer = player.getServer().getPlayerManager().getPlayer(otherId);
                if (otherPlayer != null) {
                    otherPlayer.sendMessage(Text.translatable("vitwo.tower.reconnect_success"), false);
                }
            }
        }
        syncParty(party, player.getServer());
    }

    public void tick(MinecraftServer server) {
        this.currentServer = server;

        // Periodic invite expiration cleanup (60s TTL)
        if (server.getTicks() % 20 == 0 && !pendingInviteTimestamps.isEmpty()) {
            long now = System.currentTimeMillis();
            pendingInviteTimestamps.entrySet().removeIf(entry -> {
                if (now - entry.getValue() > 60_000L) {
                    UUID targetId = entry.getKey();
                    pendingInvites.remove(targetId);
                    pendingInviterNames.remove(targetId);
                    ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetId);
                    if (targetPlayer != null) {
                        syncPlayerState(targetPlayer);
                    }
                    return true;
                }
                return false;
            });
        }

        for (TowerParty party : activeParties.values()) {
            if (party.getDisconnectedPlayerId() != null) {
                if (party.isDisconnectGraceExpired()) {
                    ServerPlayerEntity remaining = server.getPlayerManager().getPlayer(party.getOtherPlayer(party.getDisconnectedPlayerId()));
                    if (remaining != null) {
                        remaining.sendMessage(Text.translatable("vitwo.tower.reconnect_timeout"), false);
                        LevelCapManager.restorePlayerLevels(remaining, party);
                        TowerArenaManager.getInstance().returnPlayerToOriginalPos(remaining, party.getOriginalLeaderPos());
                        syncPlayerState(remaining);
                    }
                    TowerArenaManager.getInstance().cleanupFloorArena(party, server);
                    disbandParty(party);
                }
            }

            // Battle Anti-Stall Watcher — Definitive NPC Freeze Prevention
            if (party.getState() == TowerParty.State.IN_BATTLE) {
                ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
                if (leader != null) {
                    // Prevent Overworld Party Leaks: If player is not in the tower dimension, silently disband the stale party without touching Overworld battles!
                    if (leader.getServerWorld() == null || !leader.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
                        LOGGER.warn("[CobbleTower] Player {} is in a TowerParty but not in the tower dimension! Disbanding stale party.", leader.getName().getString());
                        disbandParty(party);
                        continue;
                    }

                    var battle = com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(leader);
                    if (battle != null) {
                        // Battle is active and alive in Cobblemon Showdown!
                        party.setCurrentBattleTurns(battle.getTurn());
                        party.resetNoBattleTicks();
                        party.resetStallTicks();
                        party.resetHardStallTicks();
                    } else {
                        party.resetStallTicks();
                        party.resetHardStallTicks();
                        int noBattle = party.incrementNoBattleTicks();
                        // If no battle has started for 60s (1200 ticks) while in IN_BATTLE,
                        // handle as abandoned/defeated session
                        if (noBattle >= 1200) {
                            LOGGER.warn("[CobbleTower] Active battle ended or timed out on Floor {}. Auto-handling defeat.", party.getCurrentFloor());
                            party.resetNoBattleTicks();
                            onPartyDefeated(party, server);
                            continue;
                        }
                    }
                }
            } else {
                party.resetStallTicks();
                party.resetHardStallTicks();
                party.resetNoBattleTicks();
            }

            // Periodic 1s Real-Time HUD and Party Sync
            if (server.getTicks() % 20 == 0) {
                syncParty(party, server);
            }
        }

        // Spawn Visual Cosmetic Auras for online players
        if (server.getTicks() % 4 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                String aura = TowerPlayerDataManager.getInstance().getActiveCosmeticAura(p.getUuid());
                if (aura != null && !aura.equals("NONE")) {
                    ServerWorld world = p.getServerWorld();
                    double px = p.getX();
                    double py = p.getY();
                    double pz = p.getZ();

                    if ("cosmetic_shiny_aura".equals(aura)) {
                        for (int i = 0; i < 2; i++) {
                            double angle = (server.getTicks() * 0.15 + (i * Math.PI)) % (2 * Math.PI);
                            double radius = 0.75;
                            double ox = Math.cos(angle) * radius;
                            double oz = Math.sin(angle) * radius;
                            double oy = 0.3 + ((server.getTicks() % 24) / 24.0) * 1.5;
                            world.spawnParticles(ParticleTypes.WAX_ON, px + ox, py + oy, pz + oz, 1, 0.0, 0.02, 0.0, 0.01);
                        }
                    } else if ("cosmetic_particle_trail".equals(aura)) {
                        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py + 0.1, pz, 2, 0.15, 0.05, 0.15, 0.01);
                        world.spawnParticles(ParticleTypes.GLOW_SQUID_INK, px, py + 0.5, pz, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        }
    }

    public void disbandParty(TowerParty party) {
        activeParties.remove(party.getLeaderId());
        playerToPartyLeader.remove(party.getLeaderId());
        if (party.getMemberId() != null) {
            playerToPartyLeader.remove(party.getMemberId());
        }
    }

    public void syncPlayerState(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) return;
        this.currentServer = player.getServer();

        // === ONE-TIME AUTO-COMPENSATION for player Zitj (level loss bug fix) ===
        TowerPlayerDataManager.getInstance().checkZitjCompensation(player);

        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isPresent()) {
            syncParty(partyOpt.get(), player.getServer());
        } else {
            boolean inTowerDim = player.getServerWorld() != null && player.getServerWorld().getRegistryKey().getValue().getPath().contains("tower");
            if (inTowerDim) {
                boolean restored = TowerRunPersistenceManager.getInstance().restoreRun(player, player.getServer());
                if (restored) return;
                int soloCp = getSoloCheckpoint(player.getUuid());
                TowerParty recoveredParty = new TowerParty(player.getUuid(), soloCp);
                recoveredParty.setSolo(true);
                recoveredParty.setCurrentFloor(soloCp);
                recoveredParty.setHighestCheckpoint(soloCp);
                recoveredParty.rollFloorTrainer(soloCp);
                recoveredParty.setState(TowerParty.State.IN_BATTLE);
                registerRestoredParty(recoveredParty, player.getServer());
                TowerRunPersistenceManager.getInstance().saveRun(recoveredParty);
                return;
            }

            String pendingName = pendingInviterNames.getOrDefault(player.getUuid(), "");
            int bp = TowerPlayerDataManager.getInstance().getBp(player.getUuid());
            int highestFloor = TowerPlayerDataManager.getInstance().getProfile(player.getUuid()).highestFloorTrueRun;
            ServerPlayNetworking.send(player, new SyncPartyStateS2CPacket(
                    false,
                    false,
                    "",
                    "",
                    1,
                    getSoloCheckpoint(player.getUuid()),
                    getDuoCheckpoint(player.getUuid()),
                    false,
                    false,
                    pendingName,
                    false,
                    0,
                    "",
                    bp,
                    true,
                    highestFloor,
                    0,
                    0,
                    0,
                    0,
                    1.0f
            ));
            ServerPlayNetworking.send(player, new com.vitwo.network.s2c.SyncLeaderboardS2CPacket(com.vitwo.config.TowerLeaderboardManager.getInstance().getTopEntries()));
        }
    }

    public void syncParty(TowerParty party, MinecraftServer server) {
        if (server == null) return;
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        String leaderName = leader != null ? leader.getName().getString() : "Leader";
        String memberName = member != null ? member.getName().getString() : "";

        int soloCpLeader = leader != null ? getSoloCheckpoint(leader.getUuid()) : 1;
        boolean inSession = party.getState() == TowerParty.State.IN_BATTLE
                || party.getState() == TowerParty.State.PREPARING
                || party.getState() == TowerParty.State.REST_FLOOR;
        String bossName = party.getCurrentBossName();
        int votes = party.getForfeitVoteCount();

        int duration = party.getDurationSeconds();
        int battleTurns = party.getCurrentBattleTurns();
        int bpEarned = party.getBpEarnedInRun();

        int leaderAlive = 0;
        float leaderHpSum = 0f;
        float leaderMaxHpSum = 0f;
        if (leader != null) {
            try {
                var lp = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(leader);
                if (lp != null) {
                    for (Pokemon mon : lp) {
                        if (mon != null) {
                            if (mon.getCurrentHealth() > 0) leaderAlive++;
                            leaderHpSum += mon.getCurrentHealth();
                            leaderMaxHpSum += mon.getMaxHealth();
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        float leaderHpPct = leaderMaxHpSum > 0 ? (leaderHpSum / leaderMaxHpSum) : 1.0f;

        int memberAlive = 0;
        float memberHpSum = 0f;
        float memberMaxHpSum = 0f;
        if (member != null) {
            try {
                var mp = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(member);
                if (mp != null) {
                    for (Pokemon mon : mp) {
                        if (mon != null) {
                            if (mon.getCurrentHealth() > 0) memberAlive++;
                            memberHpSum += mon.getCurrentHealth();
                            memberMaxHpSum += mon.getMaxHealth();
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        float memberHpPct = memberMaxHpSum > 0 ? (memberHpSum / memberMaxHpSum) : 1.0f;

        var leaderboardPacket = new com.vitwo.network.s2c.SyncLeaderboardS2CPacket(com.vitwo.config.TowerLeaderboardManager.getInstance().getTopEntries());

        if (leader != null) {
            int bpLeader = TowerPlayerDataManager.getInstance().getBp(leader.getUuid());
            int highestLeader = TowerPlayerDataManager.getInstance().getProfile(leader.getUuid()).highestFloorTrueRun;
            ServerPlayNetworking.send(leader, new SyncPartyStateS2CPacket(
                    !party.isSolo(),
                    true,
                    leaderName,
                    memberName,
                    party.getCurrentFloor(),
                    soloCpLeader,
                    party.getHighestCheckpoint(),
                    party.getState() == TowerParty.State.IN_BATTLE,
                    party.isPlayerSpectating(leader.getUuid()),
                    "",
                    inSession,
                    votes,
                    bossName,
                    bpLeader,
                    party.isTrueRun(),
                    highestLeader,
                    duration,
                    battleTurns,
                    bpEarned,
                    memberAlive,
                    memberHpPct
            ));
            ServerPlayNetworking.send(leader, leaderboardPacket);
        }
        if (member != null) {
            int soloCpMember = getSoloCheckpoint(member.getUuid());
            int bpMember = TowerPlayerDataManager.getInstance().getBp(member.getUuid());
            int highestMember = TowerPlayerDataManager.getInstance().getProfile(member.getUuid()).highestFloorTrueRun;
            ServerPlayNetworking.send(member, new SyncPartyStateS2CPacket(
                    true,
                    false,
                    leaderName,
                    memberName,
                    party.getCurrentFloor(),
                    soloCpMember,
                    party.getHighestCheckpoint(),
                    party.getState() == TowerParty.State.IN_BATTLE,
                    party.isPlayerSpectating(member.getUuid()),
                    "",
                    inSession,
                    votes,
                    bossName,
                    bpMember,
                    party.isTrueRun(),
                    highestMember,
                    duration,
                    battleTurns,
                    bpEarned,
                    leaderAlive,
                    leaderHpPct
            ));
            ServerPlayNetworking.send(member, leaderboardPacket);
        }
    }

    private boolean isPlayerKantoReady(ServerPlayerEntity player) {
        // Temporarily unlocked for testing / evaluation
        return true;
    }
}
