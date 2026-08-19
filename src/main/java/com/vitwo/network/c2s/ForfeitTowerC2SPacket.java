package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ForfeitTowerC2SPacket() implements CustomPayload {
    public static final Id<ForfeitTowerC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "forfeit_tower_c2s"));

    public static final PacketCodec<PacketByteBuf, ForfeitTowerC2SPacket> CODEC = PacketCodec.unit(new ForfeitTowerC2SPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
