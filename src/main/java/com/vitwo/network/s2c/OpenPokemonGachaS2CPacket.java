package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import com.vitwo.reward.GachaPokemonCandidate;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record OpenPokemonGachaS2CPacket(
        int floor,
        String bossName,
        List<GachaPokemonCandidate> candidates,
        int winningIndex,
        boolean isShinyWinner,
        int[] rolledIvs
) implements CustomPayload {
    public static final Id<OpenPokemonGachaS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "open_pokemon_gacha_s2c"));

    public static final PacketCodec<PacketByteBuf, OpenPokemonGachaS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeVarInt(value.floor());
                buf.writeString(value.bossName() != null ? value.bossName() : "");
                buf.writeVarInt(value.candidates().size());
                for (GachaPokemonCandidate c : value.candidates()) {
                    c.write(buf);
                }
                buf.writeVarInt(value.winningIndex());
                buf.writeBoolean(value.isShinyWinner());
                for (int i = 0; i < 6; i++) {
                    buf.writeVarInt(value.rolledIvs() != null && value.rolledIvs().length > i ? value.rolledIvs()[i] : 31);
                }
            },
            buf -> {
                int floor = buf.readVarInt();
                String bossName = buf.readString();
                int size = buf.readVarInt();
                List<GachaPokemonCandidate> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(GachaPokemonCandidate.read(buf));
                }
                int winningIndex = buf.readVarInt();
                boolean isShinyWinner = buf.readBoolean();
                int[] ivs = new int[6];
                for (int i = 0; i < 6; i++) {
                    ivs[i] = buf.readVarInt();
                }
                return new OpenPokemonGachaS2CPacket(floor, bossName, list, winningIndex, isShinyWinner, ivs);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

