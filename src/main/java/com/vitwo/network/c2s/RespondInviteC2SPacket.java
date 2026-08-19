package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RespondInviteC2SPacket(boolean accepted) implements CustomPayload {
    public static final Id<RespondInviteC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "respond_invite_c2s"));
    public static final PacketCodec<PacketByteBuf, RespondInviteC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, RespondInviteC2SPacket::accepted,
            RespondInviteC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
