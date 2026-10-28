package io.github.ary82.royaltitles.client;

import io.github.ary82.royaltitles.RoyalTitles;
import io.github.ary82.royaltitles.TitleConfig;
import io.github.ary82.royaltitles.TitleData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RoyalTitles.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (ClientModEvents.OPEN.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new TitlesScreen());
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        TitleData.CLIENT = new TitleData();
        TitleConfig.CLIENT = new TitleConfig();
    }
}
