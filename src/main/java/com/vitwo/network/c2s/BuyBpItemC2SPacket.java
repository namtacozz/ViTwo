package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BuyBpItemC2SPacket(String itemId, int quantity) implements CustomPayload {
    public static final Id<BuyBpItemC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "buy_bp_item_c2s"));
    public static final PacketCodec<PacketByteBuf, BuyBpItemC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, BuyBpItemC2SPacket::itemId,
            PacketCodecs.INTEGER, BuyBpItemC2SPacket::quantity,
            BuyBpItemC2SPacket::new
    );

    public BuyBpItemC2SPacket(String itemId) {
        this(itemId, 1);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
