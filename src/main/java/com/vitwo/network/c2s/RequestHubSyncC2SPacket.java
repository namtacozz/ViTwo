package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestHubSyncC2SPacket() implements CustomPayload {
    public static final Id<RequestHubSyncC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "request_hub_sync_c2s"));
    public static final PacketCodec<PacketByteBuf, RequestHubSyncC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> {},
            buf -> new RequestHubSyncC2SPacket()
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
