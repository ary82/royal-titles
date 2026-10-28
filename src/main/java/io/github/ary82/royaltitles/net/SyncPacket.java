package io.github.ary82.royaltitles.net;

import io.github.ary82.royaltitles.client.ClientHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: the title config (as JSON) plus everyone's player data. */
public record SyncPacket(String config, CompoundTag players) {
    private static final int MAX_CONFIG = 1 << 20;

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(config, MAX_CONFIG);
        buf.writeNbt(players);
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        return new SyncPacket(buf.readUtf(MAX_CONFIG), buf.readNbt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.apply(config, players));
    }
}
