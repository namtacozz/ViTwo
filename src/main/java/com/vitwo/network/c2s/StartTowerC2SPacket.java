package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StartTowerC2SPacket(boolean isSolo, int checkpointFloor) implements CustomPayload {
    public static final Id<StartTowerC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "start_tower_c2s"));
    public static final PacketCodec<PacketByteBuf, StartTowerC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, StartTowerC2SPacket::isSolo,
            PacketCodecs.INTEGER, StartTowerC2SPacket::checkpointFloor,
            StartTowerC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
