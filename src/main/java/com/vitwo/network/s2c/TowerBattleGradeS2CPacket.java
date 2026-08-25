package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TowerBattleGradeS2CPacket(
        int floor,
        String grade,
        int bonusBp,
        int turns,
        int faints
) implements CustomPayload {
    public static final Id<TowerBattleGradeS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "tower_battle_grade_s2c"));

    public static final PacketCodec<PacketByteBuf, TowerBattleGradeS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, TowerBattleGradeS2CPacket::floor,
            PacketCodecs.STRING, TowerBattleGradeS2CPacket::grade,
            PacketCodecs.INTEGER, TowerBattleGradeS2CPacket::bonusBp,
            PacketCodecs.INTEGER, TowerBattleGradeS2CPacket::turns,
            PacketCodecs.INTEGER, TowerBattleGradeS2CPacket::faints,
            TowerBattleGradeS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
