package com.vitwo.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncGhostSupportS2CPacket(int charges, int maxCharges) implements CustomPayload {
    public static final Id<SyncGhostSupportS2CPacket> ID = new Id<>(Identifier.of("vitwo", "sync_ghost_support"));
    public static final PacketCodec<PacketByteBuf, SyncGhostSupportS2CPacket> CODEC = CustomPayload.codecOf(
            SyncGhostSupportS2CPacket::write,
            SyncGhostSupportS2CPacket::new
    );

    public SyncGhostSupportS2CPacket(PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(charges);
        buf.writeInt(maxCharges);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
