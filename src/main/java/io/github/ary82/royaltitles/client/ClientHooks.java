package io.github.ary82.royaltitles.client;

import io.github.ary82.royaltitles.TitleConfig;
import io.github.ary82.royaltitles.TitleData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;

public final class ClientHooks {
    private ClientHooks() {}

    public static void apply(String config, CompoundTag players) {
        TitleConfig.CLIENT = TitleConfig.fromJson(config);
        TitleData.CLIENT = TitleData.load(players);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (AbstractClientPlayer p : mc.level.players()) p.refreshDisplayName();
        }
    }
}
