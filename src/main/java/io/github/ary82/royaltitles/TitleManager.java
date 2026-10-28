package io.github.ary82.royaltitles;

import io.github.ary82.royaltitles.net.Net;
import io.github.ary82.royaltitles.net.SyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Server-side operations on the saved data. */
public final class TitleManager {
    private TitleManager() {}

    public static TitleData data() {
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return null;
        return s.overworld().getDataStorage().computeIfAbsent(TitleData::load, TitleData::new, RoyalTitles.MODID);
    }

    /** Call after any change: saves, syncs every client, refreshes names. */
    public static void changed() {
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        TitleData d = data();
        if (s == null || d == null) return;
        d.setDirty();
        Net.CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncPacket(TitleConfig.SERVER.toJson(), d.save(new CompoundTag())));
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.refreshDisplayName();
            p.refreshTabListName();
        }
    }

    public static boolean grant(UUID id, String name, String titleId) {
        TitleData d = data();
        d.names.put(id, name);
        return d.owned.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(titleId);
    }

    public static boolean revoke(UUID id, String titleId) {
        TitleData d = data();
        var set = d.owned.get(id);
        if (set == null || !set.remove(titleId)) return false;
        if (titleId.equals(d.equipped.get(id))) d.equipped.remove(id);
        return true;
    }

    /** Returns an error message, or null on success. Empty titleId unequips. */
    public static String equip(ServerPlayer p, String titleId) {
        TitleData d = data();
        UUID id = p.getUUID();
        if (titleId.isEmpty()) {
            d.equipped.remove(id);
        } else {
            if (!TitleConfig.SERVER.titles.containsKey(titleId)) return "No such title: " + titleId;
            if (!d.owned.getOrDefault(id, Set.of()).contains(titleId)) return "You don't own that title.";
            d.equipped.put(id, titleId);
        }
        changed();
        return null;
    }

    public static void setNametag(ServerPlayer p, boolean show) {
        TitleData d = data();
        if (show) d.nametag.add(p.getUUID());
        else d.nametag.remove(p.getUUID());
        changed();
    }

    /** "name, [Title]" with the title in its rarity color; just the name if none is equipped. */
    public static Component withTitle(TitleConfig cfg, TitleData d, UUID player, Component name) {
        String eq = d.equipped.get(player);
        TitleConfig.Title t = eq == null ? null : cfg.titles.get(eq);
        if (t == null) return name;
        MutableComponent title = Component.literal("[" + t.name + "]").setStyle(Style.EMPTY.withColor(cfg.color(t.rarity)));
        return Component.empty().append(name).append(", ").append(title);
    }
}
