package com.vitwo.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GhostSupportActionC2SPacket(int actionType) implements CustomPayload {
    public static final Id<GhostSupportActionC2SPacket> ID = new Id<>(Identifier.of("vitwo", "ghost_support_action"));
    public static final PacketCodec<PacketByteBuf, GhostSupportActionC2SPacket> CODEC = CustomPayload.codecOf(
            GhostSupportActionC2SPacket::write,
            GhostSupportActionC2SPacket::new
    );

    public GhostSupportActionC2SPacket(PacketByteBuf buf) {
        this(buf.readInt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(actionType);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
