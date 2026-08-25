package com.vitwo.party;

import com.vitwo.arena.TowerArenaManager;
import com.vitwo.battle.LevelCapManager;
import com.vitwo.battle.TowerBattleManager;
import com.vitwo.config.TowerPlayerDataManager;
import com.vitwo.network.s2c.OpenRestScreenS2CPacket;
import com.vitwo.network.s2c.OpenRunSummaryS2CPacket;
import com.vitwo.network.s2c.SyncPartyStateS2CPacket;
import com.vitwo.network.s2c.TowerTitleS2CPacket;
import com.vitwo.reward.TowerRewardManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TowerPartyManager {
    private static final TowerPartyManager INSTANCE = new TowerPartyManager();
    public static TowerPartyManager getInstance() { return INSTANCE; }

    private final Map<UUID, TowerParty> activeParties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToPartyLeader = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingInviterNames = new ConcurrentHashMap<>();
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
        party.rollFloorTrainer(floor);

        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        if (leader == null) return;

        party.setState(TowerParty.State.PREPARING);

        // Teleport party to fresh floor arena and barrier boundary
        TowerArenaManager.getInstance().teleportPartyToArena(party, leader, member);

        String bossName = party.getCurrentBossName();
        TowerTitleS2CPacket titlePayload = new TowerTitleS2CPacket(
                "§b§lCOBBLE TOWER",
                "§eFloor " + floor + " §7| §cBoss: §f" + bossName,
                floor,
                bossName
        );

        ServerPlayNetworking.send(leader, titlePayload);
        if (member != null) ServerPlayNetworking.send(member, titlePayload);

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
        } else if (leader != null && member != null) {
            updateDuoCheckpoint(leader.getUuid(), clearedFloor + 1);
            updateDuoCheckpoint(member.getUuid(), clearedFloor + 1);
        }

        int turnsThisFloor = 3;
        int faintsThisFloor = 0;
        party.incrementTurns(turnsThisFloor);

        TowerRewardManager.getInstance().grantFloorReward(leader, member, clearedFloor, party.isTrueRun(), turnsThisFloor, faintsThisFloor);
        TowerRewardManager.getInstance().checkMilestones(leader, member, clearedFloor, party.isTrueRun());

        if (clearedFloor >= 100) {
            party.setState(TowerParty.State.COMPLETED);

            // Record victory
            int turns = party.getTurnsElapsed();
            int duration = party.getDurationSeconds();
            int faints = party.getFaintsCount();
            int bpEarned = party.isTrueRun() ? 3000 : 1000;

            String playerNames = leader != null ? leader.getName().getString() : "Leader";
            if (member != null) {
                playerNames += " & " + member.getName().getString();
            }
            if (party.isTrueRun()) {
                com.vitwo.config.TowerLeaderboardManager.getInstance().recordCompletion(playerNames, party.isSolo(), duration, turns, faints);
            }

            if (leader != null) {
                TowerPlayerDataManager.getInstance().recordRunResult(leader.getUuid(), 100, party.isTrueRun(), turns, duration, true);
                ServerPlayNetworking.send(leader, new OpenRunSummaryS2CPacket(100, true, party.isTrueRun(), duration, turns, faints, bpEarned, 100));
                TowerArenaManager.getInstance().returnPlayerToOriginalPos(leader, party.getOriginalLeaderPos());
            }
            if (member != null) {
                TowerPlayerDataManager.getInstance().recordRunResult(member.getUuid(), 100, party.isTrueRun(), turns, duration, true);
                ServerPlayNetworking.send(member, new OpenRunSummaryS2CPacket(100, true, party.isTrueRun(), duration, turns, faints, bpEarned, 100));
                TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
            }

            TowerRunPersistenceManager.getInstance().deleteRun(party.getLeaderId());
            TowerArenaManager.getInstance().cleanupFloorArena(party, server);
            disbandParty(party);
            if (leader != null) syncPlayerState(leader);
            if (member != null) syncPlayerState(member);
            return;
        }

        if (clearedFloor % 5 == 0) {
            party.setState(TowerParty.State.REST_FLOOR);
            party.clearRestChoices();

            // Automatic Base Heal (+25% HP, +50% PP) for alive mons
            if (leader != null) TowerRewardManager.getInstance().applyBaseHeal(leader);
            if (member != null) TowerRewardManager.getInstance().applyBaseHeal(member);

            // Directly open Rest Station GUI in place without teleporting away
            if (leader != null) ServerPlayNetworking.send(leader, new OpenRestScreenS2CPacket(clearedFloor));
            if (member != null) ServerPlayNetworking.send(member, new OpenRestScreenS2CPacket(clearedFloor));
            syncParty(party, server);
        } else {
            // Do NOT cleanup arena on floor advance, Arena Pooling reuses the structure!
            party.setCurrentFloor(clearedFloor + 1);
            startFloor(party, server);
        }
    }

    public void handleRestChoice(ServerPlayerEntity player, int choice) {
        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isEmpty()) return;

        TowerParty party = partyOpt.get();
        if (party.getState() != TowerParty.State.REST_FLOOR) return;

        party.setRestChoice(player.getUuid(), choice);

        if (choice == 1) {
            TowerRewardManager.getInstance().applyFullTeamRest(player);
            player.sendMessage(Text.literal("§a[Rest Station] Selected Full Team Rest!"), false);
        } else if (choice == 2) {
            TowerRewardManager.getInstance().applyWarPrep(player, party, 1);
            player.sendMessage(Text.literal("§c[Rest Station] Selected War Preparation Buff!"), false);
        } else {
            TowerRewardManager.getInstance().grantLootCache(player, party.getCurrentFloor());
            player.sendMessage(Text.literal("§6[Rest Station] Selected Treasure Cache!"), false);
        }

        if (party.haveBothChosenRest()) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                party.setCurrentFloor(party.getCurrentFloor() + 1);
                startFloor(party, server);
            }
        }
    }

    public void handleForfeitVote(ServerPlayerEntity player) {
        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isEmpty()) return;

        TowerParty party = partyOpt.get();
        boolean shouldForfeit = party.voteForfeit(player.getUuid());

        if (shouldForfeit) {
            forfeitTower(party.getLeaderId(), player.getServer());
        } else {
            UUID otherId = party.getOtherPlayer(player.getUuid());
            if (otherId != null && player.getServer() != null) {
                ServerPlayerEntity other = player.getServer().getPlayerManager().getPlayer(otherId);
                if (other != null) {
                    other.sendMessage(Text.literal("§e[CobbleTower] §fPartner voted to forfeit (1/2). Open [Y] to confirm."), false);
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

        party.setState(TowerParty.State.LOBBY);

        int floor = party.getCurrentFloor();
        int turns = party.getTurnsElapsed();
        int duration = party.getDurationSeconds();
        int faints = party.getFaintsCount();

        if (leader != null) {
            TowerPlayerDataManager.getInstance().recordRunResult(leader.getUuid(), floor, party.isTrueRun(), turns, duration, false);
            ServerPlayNetworking.send(leader, new OpenRunSummaryS2CPacket(floor, false, party.isTrueRun(), duration, turns, faints, 0, floor));
            leader.sendMessage(Text.translatable("vitwo.tower.forfeited"), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(leader, party.getOriginalLeaderPos());
        }
        if (member != null) {
            TowerPlayerDataManager.getInstance().recordRunResult(member.getUuid(), floor, party.isTrueRun(), turns, duration, false);
            ServerPlayNetworking.send(member, new OpenRunSummaryS2CPacket(floor, false, party.isTrueRun(), duration, turns, faints, 0, floor));
            member.sendMessage(Text.translatable("vitwo.tower.forfeited"), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
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
        }
        if (member != null) {
            TowerPlayerDataManager.getInstance().recordRunResult(member.getUuid(), failedFloor, party.isTrueRun(), turns, duration, false);
            ServerPlayNetworking.send(member, new OpenRunSummaryS2CPacket(failedFloor, false, party.isTrueRun(), duration, turns, faints, 0, failedFloor));
            member.sendMessage(Text.translatable("vitwo.tower.defeat", failedFloor), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
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
            boolean restored = TowerRunPersistenceManager.getInstance().restoreRun(player, player.getServer());
            if (!restored && player.getServerWorld() != null && player.getServerWorld().getRegistryKey().getValue().getPath().contains("tower")) {
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
            if (restored) {
                return;
            }
            syncPlayerState(player);
            return;
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
        for (TowerParty party : activeParties.values()) {
            if (party.getDisconnectedPlayerId() != null) {
                if (party.isDisconnectGraceExpired()) {
                    ServerPlayerEntity remaining = server.getPlayerManager().getPlayer(party.getOtherPlayer(party.getDisconnectedPlayerId()));
                    if (remaining != null) {
                        remaining.sendMessage(Text.translatable("vitwo.tower.reconnect_timeout"), false);
                        TowerArenaManager.getInstance().returnPlayerToOriginalPos(remaining, party.getOriginalLeaderPos());
                        syncPlayerState(remaining);
                    }
                    disbandParty(party);
                }
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
                    true
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

        var leaderboardPacket = new com.vitwo.network.s2c.SyncLeaderboardS2CPacket(com.vitwo.config.TowerLeaderboardManager.getInstance().getTopEntries());

        if (leader != null) {
            int bpLeader = TowerPlayerDataManager.getInstance().getBp(leader.getUuid());
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
                    party.isTrueRun()
            ));
            ServerPlayNetworking.send(leader, leaderboardPacket);
        }
        if (member != null) {
            int soloCpMember = getSoloCheckpoint(member.getUuid());
            int bpMember = TowerPlayerDataManager.getInstance().getBp(member.getUuid());
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
                    party.isTrueRun()
            ));
            ServerPlayNetworking.send(member, leaderboardPacket);
        }
    }

    private boolean isPlayerKantoReady(ServerPlayerEntity player) {
        // Temporarily unlocked for testing / evaluation
        return true;
    }
}
