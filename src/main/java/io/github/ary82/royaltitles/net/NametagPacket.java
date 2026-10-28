package io.github.ary82.royaltitles.net;

import io.github.ary82.royaltitles.TitleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: show or hide the sender's main title above their head. */
public record NametagPacket(boolean show) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(show);
    }

    public static NametagPacket decode(FriendlyByteBuf buf) {
        return new NametagPacket(buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getSender() != null) TitleManager.setNametag(ctx.get().getSender(), show);
    }
}
