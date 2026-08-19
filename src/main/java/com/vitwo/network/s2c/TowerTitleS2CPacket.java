package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TowerTitleS2CPacket(
        String mainTitle,
        String subTitle,
        int floor,
        String bossName
) implements CustomPayload {
    public static final Id<TowerTitleS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "tower_title_s2c"));
    public static final PacketCodec<PacketByteBuf, TowerTitleS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, TowerTitleS2CPacket::mainTitle,
            PacketCodecs.STRING, TowerTitleS2CPacket::subTitle,
            PacketCodecs.INTEGER, TowerTitleS2CPacket::floor,
            PacketCodecs.STRING, TowerTitleS2CPacket::bossName,
            TowerTitleS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
