package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RestChoiceC2SPacket(int choiceType) implements CustomPayload {
    public static final Id<RestChoiceC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "rest_choice_c2s"));

    public static final PacketCodec<PacketByteBuf, RestChoiceC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeInt(value.choiceType()),
            buf -> new RestChoiceC2SPacket(buf.readInt())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
