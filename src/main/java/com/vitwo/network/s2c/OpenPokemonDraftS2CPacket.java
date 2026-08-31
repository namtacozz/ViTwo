package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record OpenPokemonDraftS2CPacket(
        int floor,
        String bossName,
        List<DraftOption> options
) implements CustomPayload {
    public static final Id<OpenPokemonDraftS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "open_pokemon_draft_s2c"));

    public record DraftOption(
            int slotIndex,
            String originalSpecies,
            String baseSpecies,
            String formAspect,
            String displayName,
            String primaryType,
            String secondaryType,
            boolean isShiny,
            boolean isLegendary
    ) {}

    public static final PacketCodec<PacketByteBuf, OpenPokemonDraftS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.floor());
                buf.writeString(value.bossName());
                buf.writeInt(value.options().size());
                for (DraftOption opt : value.options()) {
                    buf.writeInt(opt.slotIndex());
                    buf.writeString(opt.originalSpecies());
                    buf.writeString(opt.baseSpecies());
                    buf.writeString(opt.formAspect() != null ? opt.formAspect() : "");
                    buf.writeString(opt.displayName());
                    buf.writeString(opt.primaryType());
                    buf.writeString(opt.secondaryType());
                    buf.writeBoolean(opt.isShiny());
                    buf.writeBoolean(opt.isLegendary());
                }
            },
            buf -> {
                int floor = buf.readInt();
                String bossName = buf.readString();
                int size = buf.readInt();
                List<DraftOption> options = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int slotIndex = buf.readInt();
                    String orig = buf.readString();
                    String base = buf.readString();
                    String formAspect = buf.readString();
                    String display = buf.readString();
                    String t1 = buf.readString();
                    String t2 = buf.readString();
                    boolean shiny = buf.readBoolean();
                    boolean legend = buf.readBoolean();
                    options.add(new DraftOption(slotIndex, orig, base, formAspect, display, t1, t2, shiny, legend));
                }
                return new OpenPokemonDraftS2CPacket(floor, bossName, options);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
