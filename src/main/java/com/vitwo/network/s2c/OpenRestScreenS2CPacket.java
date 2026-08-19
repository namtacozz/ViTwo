package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenRestScreenS2CPacket(int floor) implements CustomPayload {
    public static final Id<OpenRestScreenS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "open_rest_screen_s2c"));
    public static final PacketCodec<PacketByteBuf, OpenRestScreenS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, OpenRestScreenS2CPacket::floor,
            OpenRestScreenS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
