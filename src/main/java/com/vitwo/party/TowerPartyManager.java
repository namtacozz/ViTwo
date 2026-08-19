package com.vitwo.party;

import com.vitwo.arena.TowerArenaManager;
import com.vitwo.battle.LevelCapManager;
import com.vitwo.battle.TowerBattleManager;
import com.vitwo.network.s2c.*;
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

    public int getDuoCheckpoint(UUID playerId) {
        return duoCheckpoints.getOrDefault(playerId, 1);
    }

    public void updateSoloCheckpoint(UUID playerId, int floor) {
        if (TowerParty.isCheckpointFloor(floor) && floor > getSoloCheckpoint(playerId)) {
            soloCheckpoints.put(playerId, floor);
        }
    }

    public void updateDuoCheckpoint(UUID playerId, int floor) {
        if (TowerParty.isCheckpointFloor(floor) && floor > getDuoCheckpoint(playerId)) {
            duoCheckpoints.put(playerId, floor);
        }
    }

    public Optional<TowerParty> getParty(UUID playerId) {
        UUID leaderId = playerToPartyLeader.get(playerId);
        if (leaderId == null) return Optional.empty();
        return Optional.ofNullable(activeParties.get(leaderId));
    }

    public boolean isInParty(UUID playerId) {
        return playerToPartyLeader.containsKey(playerId);
    }

    public void invitePlayer(ServerPlayerEntity inviter, ServerPlayerEntity target) {
        if (isInParty(target.getUuid())) {
            inviter.sendMessage(Text.translatable("vitwo.tower.party_full"), false);
            return;
        }

        pendingInvites.put(target.getUuid(), inviter.getUuid());
        pendingInviterNames.put(target.getUuid(), inviter.getName().getString());

        inviter.sendMessage(Text.translatable("vitwo.tower.invited", target.getName().getString()), false);
        target.sendMessage(Text.literal("§6[CobbleTower] §e" + inviter.getName().getString() + " §fmời bạn leo Tháp Đôi! Bấm phím §a[Y] §fđể mở Hub đồng ý."), false);

        ServerPlayNetworking.send(target, new ShowInviteToastS2CPacket(inviter.getUuid(), inviter.getName().getString()));
        syncPlayerState(target);
    }

    public void respondInvite(ServerPlayerEntity target, boolean accepted) {
        UUID inviterId = pendingInvites.remove(target.getUuid());
        pendingInviterNames.remove(target.getUuid());

        if (inviterId == null || target.getServer() == null) return;
        ServerPlayerEntity inviter = target.getServer().getPlayerManager().getPlayer(inviterId);

        if (!accepted) {
            if (inviter != null) inviter.sendMessage(Text.translatable("vitwo.tower.invite_declined", target.getName().getString()), false);
            target.sendMessage(Text.translatable("vitwo.tower.invite_declined", target.getName().getString()), false);
            syncPlayerState(target);
            return;
        }

        if (isInParty(inviterId) || isInParty(target.getUuid())) {
            target.sendMessage(Text.translatable("vitwo.tower.party_full"), false);
            syncPlayerState(target);
            return;
        }

        int minCp = Math.min(getDuoCheckpoint(inviterId), getDuoCheckpoint(target.getUuid()));
        TowerParty party = new TowerParty(inviterId, target.getUuid(), minCp);
        activeParties.put(inviterId, party);
        playerToPartyLeader.put(inviterId, inviterId);
        playerToPartyLeader.put(target.getUuid(), inviterId);

        if (inviter != null) inviter.sendMessage(Text.translatable("vitwo.tower.invite_accepted", target.getName().getString()), false);
        target.sendMessage(Text.translatable("vitwo.tower.invite_accepted", inviter != null ? inviter.getName().getString() : ""), false);

        syncParty(party, target.getServer());
    }

    public void leaveParty(ServerPlayerEntity player) {
        Optional<TowerParty> partyOpt = getParty(player.getUuid());
        if (partyOpt.isEmpty()) {
            syncPlayerState(player);
            return;
        }

        TowerParty party = partyOpt.get();
        MinecraftServer server = player.getServer();

        ServerPlayerEntity other = server != null ? server.getPlayerManager().getPlayer(party.getOtherPlayer(player.getUuid())) : null;
        if (other != null) {
            other.sendMessage(Text.translatable("vitwo.tower.party_disbanded"), false);
            syncPlayerState(other);
        }

        player.sendMessage(Text.translatable("vitwo.tower.party_disbanded"), false);
        disbandParty(party);
        syncPlayerState(player);
    }

    public void startTowerSession(TowerParty party, boolean isSolo, int checkpointFloor, MinecraftServer server) {
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        if (leader == null) return;

        // Level Cap Validation
        if (!LevelCapManager.isPlayerEligible(leader, checkpointFloor)) {
            int maxCap = LevelCapManager.getMaxLevelCapForFloor(checkpointFloor);
            leader.sendMessage(Text.literal("§c[CobbleTower] Đội hình có Pokemon vượt quá giới hạn cấp độ của chặng này (Max Lv." + maxCap + ")!"), false);
            return;
        }

        if (isSolo) {
            int maxCp = getSoloCheckpoint(leader.getUuid());
            if (checkpointFloor > maxCp) checkpointFloor = maxCp;

            party.setCurrentFloor(checkpointFloor);
            party.setHighestCheckpoint(maxCp);
            party.setState(TowerParty.State.PREPARING);
            party.setOriginalLeaderPos(leader.getBlockPos());

            TowerArenaManager.getInstance().teleportPartyToArena(party, leader, null);
            startFloor(party, server);
        } else {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(party.getMemberId());
            if (member == null) return;

            if (!LevelCapManager.isPlayerEligible(member, checkpointFloor)) {
                int maxCap = LevelCapManager.getMaxLevelCapForFloor(checkpointFloor);
                String msg = "§c[CobbleTower] Đồng đội " + member.getName().getString() + " có Pokemon vượt quá giới hạn Max Lv." + maxCap + "!";
                leader.sendMessage(Text.literal(msg), false);
                member.sendMessage(Text.literal("§c[CobbleTower] Đội hình của bạn có Pokemon vượt quá giới hạn Max Lv." + maxCap + "!"), false);
                return;
            }

            int effectiveCp = Math.min(getDuoCheckpoint(leader.getUuid()), getDuoCheckpoint(member.getUuid()));
            if (checkpointFloor > effectiveCp) checkpointFloor = effectiveCp;

            party.setCurrentFloor(checkpointFloor);
            party.setHighestCheckpoint(effectiveCp);
            party.setState(TowerParty.State.PREPARING);
            party.setOriginalLeaderPos(leader.getBlockPos());
            party.setOriginalMemberPos(member.getBlockPos());

            TowerArenaManager.getInstance().teleportPartyToArena(party, leader, member);
            startFloor(party, server);
        }
    }

    public void startFloor(TowerParty party, MinecraftServer server) {
        int floor = party.getCurrentFloor();
        party.setState(TowerParty.State.IN_BATTLE);
        party.clearSpectators();

        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        if (leader == null) return;

        String bossName = TowerBattleManager.getInstance().getBossNameForFloor(floor);
        TowerTitleS2CPacket titlePayload = new TowerTitleS2CPacket(
                "§6§lCOBBLE TOWER",
                "§eTầng §f" + floor + " §7| §cBoss: §f" + bossName,
                floor,
                bossName
        );

        ServerPlayNetworking.send(leader, titlePayload);
        if (member != null) ServerPlayNetworking.send(member, titlePayload);

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
            if (leader != null) leader.sendMessage(Text.literal("§6§l[COBBLE TOWER] CHÚC MỪNG! BẠN ĐÃ CHINH PHỤC THÀNH CÔNG ĐỈNH THÁP TẦNG 100!"), false);
            if (member != null) member.sendMessage(Text.literal("§6§l[COBBLE TOWER] CHÚC MỪNG! BẠN ĐÃ CHINH PHỤC THÀNH CÔNG ĐỈNH THÁP TẦNG 100!"), false);
            return;
        }

        if (clearedFloor % 5 == 0) {
            party.setState(TowerParty.State.REST_FLOOR);
            party.clearRestChoices();

            if (leader != null) ServerPlayNetworking.send(leader, new OpenRestScreenS2CPacket(clearedFloor));
            if (member != null) ServerPlayNetworking.send(member, new OpenRestScreenS2CPacket(clearedFloor));
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
            TowerRewardManager.getInstance().applyRestFloorHealing(player);
            player.sendMessage(Text.translatable("vitwo.tower.rest_floor.choice_heal_made"), false);
        } else {
            TowerRewardManager.getInstance().grantLootCache(player, party.getCurrentFloor());
            player.sendMessage(Text.translatable("vitwo.tower.rest_floor.choice_loot_made"), false);
        }

        if (party.haveBothChosenRest()) {
            party.setCurrentFloor(party.getCurrentFloor() + 1);
            startFloor(party, player.getServer());
        }
    }

    public void onPartyDefeated(TowerParty party, MinecraftServer server) {
        int failedFloor = party.getCurrentFloor();
        party.setState(TowerParty.State.LOBBY);

        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        if (leader != null) {
            leader.sendMessage(Text.translatable("vitwo.tower.defeat", failedFloor), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(leader, party.getOriginalLeaderPos());
        }
        if (member != null) {
            member.sendMessage(Text.translatable("vitwo.tower.defeat", failedFloor), false);
            TowerArenaManager.getInstance().returnPlayerToOriginalPos(member, party.getOriginalMemberPos());
        }

        syncParty(party, server);
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
                    pendingName
            ));
        }
    }

    public void syncParty(TowerParty party, MinecraftServer server) {
        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(party.getLeaderId());
        ServerPlayerEntity member = party.isSolo() ? null : server.getPlayerManager().getPlayer(party.getMemberId());

        String leaderName = leader != null ? leader.getName().getString() : "Đội trưởng";
        String memberName = member != null ? member.getName().getString() : "";

        int soloCpLeader = leader != null ? getSoloCheckpoint(leader.getUuid()) : 1;

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
                    ""
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
                    ""
            ));
        }
    }
}
