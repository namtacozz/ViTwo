package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TowerBossIntroS2CPacket(
        int floor,
        String bossName,
        String bossTitle,
        String quote,
        boolean isApex
) implements CustomPayload {
    public static final Id<TowerBossIntroS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "tower_boss_intro_s2c"));

    public static final PacketCodec<PacketByteBuf, TowerBossIntroS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.floor());
                buf.writeString(value.bossName());
                buf.writeString(value.bossTitle());
                buf.writeString(value.quote());
                buf.writeBoolean(value.isApex());
            },
            buf -> new TowerBossIntroS2CPacket(
                    buf.readInt(),
                    buf.readString(),
                    buf.readString(),
                    buf.readString(),
                    buf.readBoolean()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
