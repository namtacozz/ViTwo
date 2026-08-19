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
        String currentBossName
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
            },
            buf -> new SyncPartyStateS2CPacket(
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readString(),
                    buf.readString(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readString(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readString()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
