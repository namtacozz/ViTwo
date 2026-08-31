package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncPartyStateS2CPacket(
        boolean hasParty,
        boolean isLeader,
        String leaderName,
        String memberName,
        int currentFloor,
        int soloCheckpoint,
        int duoCheckpoint,
        boolean inBattle,
        boolean isSpectating,
        String pendingInviterName,
        boolean inTowerSession,
        int forfeitVotes,
        String currentBossName,
        int battlePoints,
        boolean isTrueRun,
        int highestFloor,
        int runDurationSeconds,
        int battleTurns,
        int bpEarnedInRun,
        int partnerAliveCount,
        float partnerHpPercent
) implements CustomPayload {
    public static final Id<SyncPartyStateS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "sync_party_state_s2c"));

    public static final PacketCodec<PacketByteBuf, SyncPartyStateS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBoolean(value.hasParty());
                buf.writeBoolean(value.isLeader());
                buf.writeString(value.leaderName());
                buf.writeString(value.memberName());
                buf.writeInt(value.currentFloor());
                buf.writeInt(value.soloCheckpoint());
                buf.writeInt(value.duoCheckpoint());
                buf.writeBoolean(value.inBattle());
                buf.writeBoolean(value.isSpectating());
                buf.writeString(value.pendingInviterName());
                buf.writeBoolean(value.inTowerSession());
                buf.writeInt(value.forfeitVotes());
                buf.writeString(value.currentBossName());
                buf.writeInt(value.battlePoints());
                buf.writeBoolean(value.isTrueRun());
                buf.writeInt(value.highestFloor());
                buf.writeInt(value.runDurationSeconds());
                buf.writeInt(value.battleTurns());
                buf.writeInt(value.bpEarnedInRun());
                buf.writeInt(value.partnerAliveCount());
                buf.writeFloat(value.partnerHpPercent());
            },
            buf -> {
                boolean hasParty = buf.readBoolean();
                boolean isLeader = buf.readBoolean();
                String leaderName = buf.readString();
                String memberName = buf.readString();
                int currentFloor = buf.readInt();
                int soloCheckpoint = buf.readInt();
                int duoCheckpoint = buf.readInt();
                boolean inBattle = buf.readBoolean();
                boolean isSpectating = buf.readBoolean();
                String pendingInviterName = buf.readString();
                boolean inTowerSession = buf.readBoolean();
                int forfeitVotes = buf.readInt();
                String currentBossName = buf.readString();
                int battlePoints = buf.readInt();
                boolean isTrueRun = buf.readBoolean();
                int highestFloor = buf.readInt();

                // Gracefully handle backwards-compatibility with servers sending 16-field packet
                int runDurationSeconds = buf.isReadable(4) ? buf.readInt() : 0;
                int battleTurns = buf.isReadable(4) ? buf.readInt() : 0;
                int bpEarnedInRun = buf.isReadable(4) ? buf.readInt() : 0;
                int partnerAliveCount = buf.isReadable(4) ? buf.readInt() : 0;
                float partnerHpPercent = buf.isReadable(4) ? buf.readFloat() : 1.0f;

                return new SyncPartyStateS2CPacket(
                        hasParty,
                        isLeader,
                        leaderName,
                        memberName,
                        currentFloor,
                        soloCheckpoint,
                        duoCheckpoint,
                        inBattle,
                        isSpectating,
                        pendingInviterName,
                        inTowerSession,
                        forfeitVotes,
                        currentBossName,
                        battlePoints,
                        isTrueRun,
                        highestFloor,
                        runDurationSeconds,
                        battleTurns,
                        bpEarnedInRun,
                        partnerAliveCount,
                        partnerHpPercent
                );
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
