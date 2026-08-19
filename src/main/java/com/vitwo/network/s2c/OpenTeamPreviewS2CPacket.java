package com.vitwo.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record OpenTeamPreviewS2CPacket(
        int floor,
        int durationSeconds,
        String opponentName,
        String opponentTitle,
        List<String> opponentTeam,
        List<String> playerTeam
) implements CustomPayload {
    public static final Id<OpenTeamPreviewS2CPacket> ID = new Id<>(Identifier.of("vitwo", "open_team_preview"));
    public static final PacketCodec<PacketByteBuf, OpenTeamPreviewS2CPacket> CODEC = CustomPayload.codecOf(
            OpenTeamPreviewS2CPacket::write,
            OpenTeamPreviewS2CPacket::new
    );

    public OpenTeamPreviewS2CPacket(PacketByteBuf buf) {
        this(
                buf.readInt(),
                buf.readInt(),
                buf.readString(),
                buf.readString(),
                readStringList(buf),
                readStringList(buf)
        );
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(floor);
        buf.writeInt(durationSeconds);
        buf.writeString(opponentName);
        buf.writeString(opponentTitle);
        writeStringList(buf, opponentTeam);
        writeStringList(buf, playerTeam);
    }

    private static List<String> readStringList(PacketByteBuf buf) {
        int size = buf.readInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readString());
        }
        return list;
    }

    private static void writeStringList(PacketByteBuf buf, List<String> list) {
        buf.writeInt(list != null ? list.size() : 0);
        if (list != null) {
            for (String s : list) {
                buf.writeString(s != null ? s : "");
            }
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
