package com.vitwo.network.s2c;

import com.vitwo.network.ViTwoPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record ShowInviteToastS2CPacket(UUID inviterUuid, String inviterName) implements CustomPayload {
    public static final Id<ShowInviteToastS2CPacket> ID = new Id<>(Identifier.of(ViTwoPackets.MOD_ID, "show_invite_toast_s2c"));

    public static final PacketCodec<PacketByteBuf, ShowInviteToastS2CPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.inviterUuid());
                buf.writeString(value.inviterName());
            },
            buf -> new ShowInviteToastS2CPacket(
                    buf.readUuid(),
                    buf.readString()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
