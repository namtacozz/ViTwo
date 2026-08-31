package com.vitwo.network;

import com.vitwo.network.c2s.*;
import com.vitwo.network.s2c.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ViTwoPackets {
    public static final String MOD_ID = "vitwo";

    public static void registerPayloads() {
        // Register C2S (Client -> Server)
        PayloadTypeRegistry.playC2S().register(InvitePlayerC2SPacket.ID, InvitePlayerC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RespondInviteC2SPacket.ID, RespondInviteC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(StartTowerC2SPacket.ID, StartTowerC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(LeavePartyC2SPacket.ID, LeavePartyC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RestChoiceC2SPacket.ID, RestChoiceC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ForfeitTowerC2SPacket.ID, ForfeitTowerC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BuyBpItemC2SPacket.ID, BuyBpItemC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(GhostSupportActionC2SPacket.ID, GhostSupportActionC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DebugTowerActionC2SPacket.ID, DebugTowerActionC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ChooseDraftPokemonC2SPacket.ID, ChooseDraftPokemonC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestHubSyncC2SPacket.ID, RequestHubSyncC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimGachaPokemonC2SPacket.ID, ClaimGachaPokemonC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimItemGachaC2SPacket.ID, ClaimItemGachaC2SPacket.CODEC);

        // Register S2C (Server -> Client)
        PayloadTypeRegistry.playS2C().register(ShowInviteToastS2CPacket.ID, ShowInviteToastS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPartyStateS2CPacket.ID, SyncPartyStateS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenTowerEntryS2CPacket.ID, OpenTowerEntryS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(TowerTitleS2CPacket.ID, TowerTitleS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenRestScreenS2CPacket.ID, OpenRestScreenS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenRunSummaryS2CPacket.ID, OpenRunSummaryS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGhostSupportS2CPacket.ID, SyncGhostSupportS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncLeaderboardS2CPacket.ID, SyncLeaderboardS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(TowerBossIntroS2CPacket.ID, TowerBossIntroS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(TowerBattleGradeS2CPacket.ID, TowerBattleGradeS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenPokemonDraftS2CPacket.ID, OpenPokemonDraftS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenPokemonGachaS2CPacket.ID, OpenPokemonGachaS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenItemGachaS2CPacket.ID, OpenItemGachaS2CPacket.CODEC);
    }
}
