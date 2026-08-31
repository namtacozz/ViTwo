package com.vitwo.reward;

import net.minecraft.network.PacketByteBuf;

public record GachaPokemonCandidate(
        int slotIndex,
        String speciesName,
        String displayName,
        String baseSpecies,
        String formAspect,
        String primaryType,
        String secondaryType,
        PokemonRarity rarity,
        boolean isLegendary,
        boolean isShiny
) {
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeString(speciesName != null ? speciesName : "");
        buf.writeString(displayName != null ? displayName : "");
        buf.writeString(baseSpecies != null ? baseSpecies : "");
        buf.writeString(formAspect != null ? formAspect : "");
        buf.writeString(primaryType != null ? primaryType : "Normal");
        buf.writeString(secondaryType != null ? secondaryType : "");
        buf.writeEnumConstant(rarity != null ? rarity : PokemonRarity.COMMON);
        buf.writeBoolean(isLegendary);
        buf.writeBoolean(isShiny);
    }

    public static GachaPokemonCandidate read(PacketByteBuf buf) {
        int slot = buf.readVarInt();
        String species = buf.readString();
        String display = buf.readString();
        String base = buf.readString();
        String form = buf.readString();
        String pType = buf.readString();
        String sType = buf.readString();
        PokemonRarity rarity = buf.readEnumConstant(PokemonRarity.class);
        boolean isLegend = buf.readBoolean();
        boolean isShiny = buf.readBoolean();
        return new GachaPokemonCandidate(slot, species, display, base, form, pType, sType, rarity, isLegend, isShiny);
    }

    public static GachaPokemonCandidate of(
            int slotIndex,
            String speciesName,
            String displayName,
            String baseSpecies,
            String formAspect,
            String primaryType,
            String secondaryType,
            boolean isLegendary,
            boolean isShiny
    ) {
        PokemonRarity rarity = PokemonRarity.fromSpecies(speciesName);
        return new GachaPokemonCandidate(
                slotIndex,
                speciesName,
                displayName,
                baseSpecies != null && !baseSpecies.isBlank() ? baseSpecies : speciesName,
                formAspect != null ? formAspect : "",
                primaryType != null ? primaryType : "Normal",
                secondaryType != null ? secondaryType : "",
                rarity,
                isLegendary,
                isShiny
        );
    }
}

