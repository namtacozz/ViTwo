package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record InvitePlayerC2SPacket(UUID targetPlayerId) implements CustomPayload {
    public static final Id<InvitePlayerC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "invite_player_c2s"));

    public static final PacketCodec<PacketByteBuf, InvitePlayerC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeUuid(value.targetPlayerId()),
            buf -> new InvitePlayerC2SPacket(buf.readUuid())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
