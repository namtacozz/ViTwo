package com.vitwo.network.c2s;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record ReadyTeamPreviewC2SPacket(List<Integer> slotOrder) implements CustomPayload {
    public static final Id<ReadyTeamPreviewC2SPacket> ID = new Id<>(Identifier.of("vitwo", "ready_team_preview"));
    public static final PacketCodec<PacketByteBuf, ReadyTeamPreviewC2SPacket> CODEC = CustomPayload.codecOf(
            ReadyTeamPreviewC2SPacket::write,
            ReadyTeamPreviewC2SPacket::new
    );

    public ReadyTeamPreviewC2SPacket(PacketByteBuf buf) {
        this(readIntList(buf));
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(slotOrder != null ? slotOrder.size() : 0);
        if (slotOrder != null) {
            for (int val : slotOrder) {
                buf.writeInt(val);
            }
        }
    }

    private static List<Integer> readIntList(PacketByteBuf buf) {
        int size = buf.readInt();
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readInt());
        }
        return list;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
