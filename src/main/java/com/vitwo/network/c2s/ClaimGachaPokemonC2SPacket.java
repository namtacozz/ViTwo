package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClaimGachaPokemonC2SPacket(
        int floor,
        int winningCandidateIndex,
        boolean isShiny,
        int[] rolledIvs
) implements CustomPayload {
    public static final Id<ClaimGachaPokemonC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "claim_gacha_pokemon_c2s"));

    public static final PacketCodec<PacketByteBuf, ClaimGachaPokemonC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeVarInt(value.floor());
                buf.writeVarInt(value.winningCandidateIndex());
                buf.writeBoolean(value.isShiny());
                for (int i = 0; i < 6; i++) {
                    buf.writeVarInt(value.rolledIvs() != null && value.rolledIvs().length > i ? value.rolledIvs()[i] : 31);
                }
            },
            buf -> {
                int floor = buf.readVarInt();
                int winIdx = buf.readVarInt();
                boolean shiny = buf.readBoolean();
                int[] ivs = new int[6];
                for (int i = 0; i < 6; i++) {
                    ivs[i] = buf.readVarInt();
                }
                return new ClaimGachaPokemonC2SPacket(floor, winIdx, shiny, ivs);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

