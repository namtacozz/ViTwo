package com.vitwo.network.s2c;

import com.vitwo.config.TowerLeaderboardManager.LeaderboardEntry;
import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SyncLeaderboardS2CPacket(List<LeaderboardEntry> entries) implements CustomPayload {
    public static final Id<SyncLeaderboardS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "sync_leaderboard_s2c"));

    public static final PacketCodec<PacketByteBuf, SyncLeaderboardS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.entries().size());
                for (LeaderboardEntry entry : value.entries()) {
                    buf.writeInt(entry.rank());
                    buf.writeString(entry.playerNames());
                    buf.writeBoolean(entry.isSolo());
                    buf.writeInt(entry.durationSeconds());
                    buf.writeInt(entry.totalTurns());
                    buf.writeInt(entry.faints());
                    buf.writeLong(entry.completionTimestamp());
                }
            },
            buf -> {
                int size = buf.readInt();
                List<LeaderboardEntry> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int rank = buf.readInt();
                    String names = buf.readString();
                    boolean solo = buf.readBoolean();
                    int dur = buf.readInt();
                    int turns = buf.readInt();
                    int faints = buf.readInt();
                    long time = buf.readLong();
                    list.add(new LeaderboardEntry(rank, names, solo, dur, turns, faints, time));
                }
                return new SyncLeaderboardS2CPacket(list);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
