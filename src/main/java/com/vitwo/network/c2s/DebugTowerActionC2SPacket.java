package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DebugTowerActionC2SPacket(String action, int value) implements CustomPayload {
    public static final Id<DebugTowerActionC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "debug_tower_action_c2s"));
    public static final PacketCodec<PacketByteBuf, DebugTowerActionC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, DebugTowerActionC2SPacket::action,
            PacketCodecs.INTEGER, DebugTowerActionC2SPacket::value,
            DebugTowerActionC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
