package io.github.ary82.royaltitles;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RoyalTitles.MODID)
public final class CommonEvents {
    @SubscribeEvent
    public static void serverStart(ServerAboutToStartEvent e) {
        TitleConfig.load();
    }

    /**
     * Server side this is the name used in chat, so the title is always added.
     * Client side it's the nametag, which only shows the title if the player opted in.
     */
    @SubscribeEvent
    public static void nameFormat(PlayerEvent.NameFormat e) {
        boolean client = e.getEntity().level().isClientSide;
        TitleData d = client ? TitleData.CLIENT : TitleManager.data();
        if (d == null || (client && !d.nametag.contains(e.getEntity().getUUID()))) return;
        TitleConfig cfg = client ? TitleConfig.CLIENT : TitleConfig.SERVER;
        e.setDisplayname(TitleManager.withTitle(cfg, d, e.getEntity().getUUID(), e.getDisplayname()));
    }

    @SubscribeEvent
    public static void tabName(PlayerEvent.TabListNameFormat e) {
        TitleData d = TitleManager.data();
        if (d == null || !d.equipped.containsKey(e.getEntity().getUUID())) return;
        var base = e.getDisplayName() != null ? e.getDisplayName() : e.getEntity().getName();
        e.setDisplayName(TitleManager.withTitle(TitleConfig.SERVER, d, e.getEntity().getUUID(), base));
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        TitleData d = TitleManager.data();
        if (d == null || !(e.getEntity() instanceof ServerPlayer sp)) return;
        d.names.put(sp.getUUID(), sp.getGameProfile().getName());
        TitleManager.changed(); // everyone gets the fresh snapshot, including the joiner
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        RoyalCommands.register(e.getDispatcher());
    }
}
