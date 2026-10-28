package io.github.ary82.royaltitles.net;

import io.github.ary82.royaltitles.RoyalTitles;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String VERSION = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RoyalTitles.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPacket::encode).decoder(SyncPacket::decode).consumerMainThread(SyncPacket::handle).add();
        CHANNEL.messageBuilder(EquipPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EquipPacket::encode).decoder(EquipPacket::decode).consumerMainThread(EquipPacket::handle).add();
        CHANNEL.messageBuilder(NametagPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NametagPacket::encode).decoder(NametagPacket::decode).consumerMainThread(NametagPacket::handle).add();
    }
}
