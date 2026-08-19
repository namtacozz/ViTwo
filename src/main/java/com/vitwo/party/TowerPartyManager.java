package com.vitwo.party;

import com.vitwo.arena.TowerArenaManager;
import com.vitwo.battle.LevelCapManager;
import com.vitwo.battle.TowerBattleManager;
import com.vitwo.network.s2c.OpenRestScreenS2CPacket;
import com.vitwo.network.s2c.SyncPartyStateS2CPacket;
import com.vitwo.network.s2c.TowerTitleS2CPacket;
import com.vitwo.reward.TowerRewardManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
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

    private final Map<UUID, Integer> soloCheckpoints = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> duoCheckpoints = new ConcurrentHashMap<>();

    private TowerPartyManager() {}

    public int getSoloCheckpoint(UUID playerId) {
        return soloCheckpoints.getOrDefault(playerId, 1);
    }

    public void updateSoloCheckpoint(UUID playerId, int newFloor) {
        if (TowerParty.isCheckpointFloor(newFloor)) {
            int current = getSoloCheckpoint(playerId);
            if (newFloor > current) {
                soloCheckpoints.put(playerId, newFloor);
            }
        }
    }

    public int getDuoCheckpoint(UUID playerId) {
        return duoCheckpoints.getOrDefault(playerId, 1);
    }

    public void updateDuoCheckpoint(UUID playerId, int newFloor) {
        if (TowerParty.isCheckpointFloor(newFloor)) {
            int current = getDuoCheckpoint(playerId);
            if (newFloor > current) {
                duoCheckpoints.put(playerId, newFloor);
            }
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

        // Kanto Completion / Regional Readiness Check (Floor 1 Requirement)
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

        if (isSolo) {
            int maxCp = getSoloCheckpoint(leader.getUuid());
            if (checkpointFloor > maxCp) checkpointFloor = maxCp;

            party.setCurrentFloor(checkpointFloor);
            party.setHighestCheckpoint(maxCp);
            party.setState(TowerParty.State.PREPARING);
            party.setOriginalLeaderPos(leader.getBlockPos());
            party.clearForfeitVotes();

            TowerArenaManager.getInstance().teleportPartyToArena(party, leader, null);
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
            party.setOriginalLeaderPos(leader.getBlockPos());
            party.setOriginalMemberPos(member.getBlockPos());
            party.clearForfeitVotes();

            TowerArenaManager.getInstance().teleportPartyToArena(party, leader, member);
            startFloor(party, server);
        }
    }

    public void startFloor(TowerParty party, MinecraftServer server) {
        int floor = party.getCurrentFloor();
        party.setState(TowerParty.State.IN_BATTLE);
        party.clearSpectators();
        party.clearForfeitVotes();

        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        if (leader == null) return;

        String bossName = TowerBattleManager.getInstance().getBossNameForFloor(floor);
        TowerTitleS2CPacket titlePayload = new TowerTitleS2CPacket(
                "§b§lCOBBLE TOWER",
                "§eFloor " + floor + " §7| §cBoss: §f" + bossName,
                floor,
                bossName
        );

        ServerPlayNetworking.send(leader, titlePayload);
        if (member != null) ServerPlayNetworking.send(member, titlePayload);

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

        TowerRewardManager.getInstance().grantFloorReward(leader, member, clearedFloor);
        TowerRewardManager.getInstance().checkMilestones(leader, member, clearedFloor);

        if (clearedFloor >= 100) {
            party.setState(TowerParty.State.COMPLETED);
            if (leader != null) {
                leader.sendMessage(Text.translatable("advancements.vitwo.master_tower.description"), false);
                TowerArenaManager.getInstance().returnPlayerToOriginalPos(leader, party.getOriginalLeaderPos());
            }
            if (member != null) {
                member.sendMessage(Text.translatable("advancements.vitwo.master_tower.description"), false);
                TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
            }
            disbandParty(party);
            if (leader != null) syncPlayerState(leader);
            if (member != null) syncPlayerState(member);
            return;
        }

        if (clearedFloor % 5 == 0) {
            party.setState(TowerParty.State.REST_FLOOR);
            party.clearRestChoices();

            if (leader != null) ServerPlayNetworking.send(leader, new OpenRestScreenS2CPacket(clearedFloor));
            if (member != null) ServerPlayNetworking.send(member, new OpenRestScreenS2CPacket(clearedFloor));
            syncParty(party, server);
        } else {
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
            TowerRewardManager.getInstance().applyTeamHeal(player);
            player.sendMessage(Text.translatable("vitwo.tower.rest_floor.choice_heal_made"), false);
        } else {
            TowerRewardManager.getInstance().grantLootCache(player, party.getCurrentFloor());
            player.sendMessage(Text.translatable("vitwo.tower.rest_floor.choice_loot_made"), false);
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

        if (leader != null) {
            leader.sendMessage(Text.translatable("vitwo.tower.forfeited"), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(leader, party.getOriginalLeaderPos());
        }
        if (member != null) {
            member.sendMessage(Text.translatable("vitwo.tower.forfeited"), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
        }

        disbandParty(party);
        if (leader != null) syncPlayerState(leader);
        if (member != null) syncPlayerState(member);
    }

    public void onPartyDefeated(TowerParty party, MinecraftServer server) {
        int failedFloor = party.getCurrentFloor();
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        party.setState(TowerParty.State.LOBBY);

        if (leader != null) {
            leader.sendMessage(Text.translatable("vitwo.tower.defeat", failedFloor), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(leader, party.getOriginalLeaderPos());
        }
        if (member != null) {
            member.sendMessage(Text.translatable("vitwo.tower.defeat", failedFloor), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
        }

        disbandParty(party);
        if (leader != null) syncPlayerState(leader);
        if (member != null) syncPlayerState(member);
    }

    public void handleDisconnect(ServerPlayerEntity player) {
        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isEmpty()) return;

        TowerParty party = partyOpt.get();
        if (party.isSolo()) {
            disbandParty(party);
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
        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isEmpty()) {
            syncPlayerState(player);
            return;
        }

        TowerParty party = partyOpt.get();
        if (party.isPlayerDisconnected() && player.getUuid().equals(party.getDisconnectedPlayerId())) {
            party.handlePlayerReconnect(player.getUuid());
            player.sendMessage(Text.translatable("vitwo.tower.reconnect_success"), false);

            UUID otherId = party.getOtherPlayer(player.getUuid());
            if (otherId != null && player.getServer() != null) {
                ServerPlayerEntity otherPlayer = player.getServer().getPlayerManager().getPlayer(otherId);
                if (otherPlayer != null) {
                    otherPlayer.sendMessage(Text.translatable("vitwo.tower.reconnect_success"), false);
                }
            }
            syncParty(party, player.getServer());
        }
    }

    public void tick(MinecraftServer server) {
        for (TowerParty party : activeParties.values()) {
            if (party.isPlayerDisconnected()) {
                if (party.isDisconnectTimedOut()) {
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
    }

    public void disbandParty(TowerParty party) {
        activeParties.remove(party.getLeaderId());
        playerToPartyLeader.remove(party.getLeaderId());
        if (party.getMemberId() != null) {
            playerToPartyLeader.remove(party.getMemberId());
        }
    }

    public void syncPlayerState(ServerPlayerEntity player) {
        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isPresent()) {
            syncParty(partyOpt.get(), player.getServer());
        } else {
            String pendingName = pendingInviterNames.getOrDefault(player.getUuid(), "");
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
                    ""
            ));
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
                || party.getState() == TowerParty.State.REST_FLOOR
                || party.getState() == TowerParty.State.PREPARING;
        String bossName = TowerBattleManager.getInstance().getBossNameForFloor(party.getCurrentFloor());
        int votes = party.getForfeitVoteCount();

        if (leader != null) {
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
                    bossName
            ));
        }
        if (member != null) {
            int soloCpMember = getSoloCheckpoint(member.getUuid());
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
                    bossName
            ));
        }
    }

    private boolean isPlayerKantoReady(ServerPlayerEntity player) {
        if (player == null) return true;
        try {
            // Check if player has progressed enough in Kanto (party has battle-ready Pokemon Lv.20+)
            Class<?> cobblemonClass = Class.forName("com.cobblemon.mod.common.Cobblemon");
            Object cobblemonInst = cobblemonClass.getField("INSTANCE").get(null);
            java.lang.reflect.Method getStorageMethod = cobblemonInst.getClass().getMethod("getStorage");
            Object storage = getStorageMethod.invoke(cobblemonInst);
            java.lang.reflect.Method getPartyMethod = storage.getClass().getMethod("getParty", ServerPlayerEntity.class);
            Iterable<?> party = (Iterable<?>) getPartyMethod.invoke(storage, player);

            for (Object pokemon : party) {
                if (pokemon == null) continue;
                java.lang.reflect.Method getLevelMethod = pokemon.getClass().getMethod("getLevel");
                int level = (int) getLevelMethod.invoke(pokemon);
                if (level >= 20) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true; // fallback allow if reflection fails
        }
    }
}
