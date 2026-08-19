package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LeavePartyC2SPacket(boolean confirm) implements CustomPayload {
    public static final Id<LeavePartyC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "leave_party_c2s"));
    public static final PacketCodec<PacketByteBuf, LeavePartyC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, LeavePartyC2SPacket::confirm,
            LeavePartyC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
