package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenTowerEntryS2CPacket(
        int highestCheckpoint,
        boolean isLeader,
        String partyMemberName
) implements CustomPayload {
    public static final Id<OpenTowerEntryS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "open_tower_entry_s2c"));
    public static final PacketCodec<PacketByteBuf, OpenTowerEntryS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, OpenTowerEntryS2CPacket::highestCheckpoint,
            PacketCodecs.BOOL, OpenTowerEntryS2CPacket::isLeader,
            PacketCodecs.STRING, OpenTowerEntryS2CPacket::partyMemberName,
            OpenTowerEntryS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
