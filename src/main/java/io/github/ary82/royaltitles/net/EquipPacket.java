package io.github.ary82.royaltitles.net;

import io.github.ary82.royaltitles.TitleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: equip a title id (empty string = unequip). */
public record EquipPacket(String id) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(id, 64);
    }

    public static EquipPacket decode(FriendlyByteBuf buf) {
        return new EquipPacket(buf.readUtf(64));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender == null) return;
        String err = TitleManager.equip(sender, id);
        if (err != null) sender.sendSystemMessage(Component.literal(err));
    }
}
