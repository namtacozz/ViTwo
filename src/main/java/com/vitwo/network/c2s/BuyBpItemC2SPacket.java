package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BuyBpItemC2SPacket(String itemId) implements CustomPayload {
    public static final Id<BuyBpItemC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "buy_bp_item_c2s"));
    public static final PacketCodec<PacketByteBuf, BuyBpItemC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, BuyBpItemC2SPacket::itemId,
            BuyBpItemC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
