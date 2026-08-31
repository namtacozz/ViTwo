package com.vitwo.reward;

import net.minecraft.network.PacketByteBuf;

public record GachaItemCandidate(
        int slotIndex,
        String id,
        String displayName,
        String category,
        int bpAmount,
        int quantity,
        int rarityTier, // 3=Gold/Jackpot, 2=Purple/Rare, 1=Blue/Uncommon, 0=Gray/Common
        int color
) {
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeString(id != null ? id : "");
        buf.writeString(displayName != null ? displayName : "");
        buf.writeString(category != null ? category : "");
        buf.writeVarInt(bpAmount);
        buf.writeVarInt(quantity);
        buf.writeVarInt(rarityTier);
        buf.writeInt(color);
    }

    public static GachaItemCandidate read(PacketByteBuf buf) {
        int slot = buf.readVarInt();
        String id = buf.readString();
        String display = buf.readString();
        String cat = buf.readString();
        int bp = buf.readVarInt();
        int qty = buf.readVarInt();
        int tier = buf.readVarInt();
        int color = buf.readInt();
        return new GachaItemCandidate(slot, id, display, cat, bp, qty, tier, color);
    }
}

