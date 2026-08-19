package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenRunSummaryS2CPacket(
        int floor,
        boolean isVictory,
        boolean isTrueRun,
        int durationSeconds,
        int totalTurns,
        int totalFaints,
        int bpEarned,
        int newHighestFloor
) implements CustomPayload {
    public static final Id<OpenRunSummaryS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "open_run_summary_s2c"));

    public static final PacketCodec<PacketByteBuf, OpenRunSummaryS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.floor());
                buf.writeBoolean(value.isVictory());
                buf.writeBoolean(value.isTrueRun());
                buf.writeInt(value.durationSeconds());
                buf.writeInt(value.totalTurns());
                buf.writeInt(value.totalFaints());
                buf.writeInt(value.bpEarned());
                buf.writeInt(value.newHighestFloor());
            },
            buf -> new OpenRunSummaryS2CPacket(
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
