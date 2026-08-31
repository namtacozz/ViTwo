package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClaimItemGachaC2SPacket(int floor, int winningIndex) implements CustomPayload {
    public static final Id<ClaimItemGachaC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "claim_item_gacha_c2s"));

    public static final PacketCodec<PacketByteBuf, ClaimItemGachaC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeVarInt(value.floor());
                buf.writeVarInt(value.winningIndex());
            },
            buf -> new ClaimItemGachaC2SPacket(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

