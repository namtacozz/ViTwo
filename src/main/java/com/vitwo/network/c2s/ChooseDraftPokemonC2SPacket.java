package com.vitwo.network.c2s;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ChooseDraftPokemonC2SPacket(int floor, int chosenSlotIndex) implements CustomPayload {
    public static final Id<ChooseDraftPokemonC2SPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "choose_draft_pokemon_c2s"));
    public static final PacketCodec<PacketByteBuf, ChooseDraftPokemonC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, ChooseDraftPokemonC2SPacket::floor,
            PacketCodecs.INTEGER, ChooseDraftPokemonC2SPacket::chosenSlotIndex,
            ChooseDraftPokemonC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
