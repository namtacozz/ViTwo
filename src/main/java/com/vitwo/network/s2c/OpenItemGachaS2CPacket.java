package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import com.vitwo.reward.GachaItemCandidate;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record OpenItemGachaS2CPacket(
        int floor,
        List<GachaItemCandidate> candidates,
        int winningIndex
) implements CustomPayload {
    public static final Id<OpenItemGachaS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "open_item_gacha_s2c"));

    public static final PacketCodec<PacketByteBuf, OpenItemGachaS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeVarInt(value.floor());
                buf.writeVarInt(value.candidates().size());
                for (GachaItemCandidate c : value.candidates()) {
                    c.write(buf);
                }
                buf.writeVarInt(value.winningIndex());
            },
            buf -> {
                int floor = buf.readVarInt();
                int size = buf.readVarInt();
                List<GachaItemCandidate> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(GachaItemCandidate.read(buf));
                }
                int winningIndex = buf.readVarInt();
                return new OpenItemGachaS2CPacket(floor, list, winningIndex);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

