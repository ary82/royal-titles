package io.github.ary82.royaltitles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Per-player state, saved with the world. Definitions live in {@link TitleConfig}. */
public class TitleData extends SavedData {
    /** Client-side copy, replaced whenever the server syncs. */
    public static TitleData CLIENT = new TitleData();

    public final Map<UUID, Set<String>> owned = new HashMap<>();
    public final Map<UUID, String> equipped = new HashMap<>();
    public final Map<UUID, String> names = new HashMap<>();
    /** Players who chose to show their main title above their head. */
    public final Set<UUID> nametag = new HashSet<>();

    public static TitleData load(CompoundTag tag) {
        TitleData d = new TitleData();
        each(tag, "owned", c -> {
            Set<String> set = new LinkedHashSet<>();
            for (Tag s : c.getList("t", Tag.TAG_STRING)) set.add(s.getAsString());
            d.owned.put(c.getUUID("u"), set);
        });
        each(tag, "equipped", c -> d.equipped.put(c.getUUID("u"), c.getString("t")));
        each(tag, "names", c -> d.names.put(c.getUUID("u"), c.getString("n")));
        for (Tag t : tag.getList("nametag", Tag.TAG_INT_ARRAY)) d.nametag.add(NbtUtils.loadUUID(t));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("owned", list(owned, (c, set) -> {
            ListTag l = new ListTag();
            set.forEach(s -> l.add(StringTag.valueOf(s)));
            c.put("t", l);
        }));
        tag.put("equipped", list(equipped, (c, t) -> c.putString("t", t)));
        tag.put("names", list(names, (c, n) -> c.putString("n", n)));
        ListTag nt = new ListTag();
        nametag.forEach(u -> nt.add(NbtUtils.createUUID(u)));
        tag.put("nametag", nt);
        return tag;
    }

    private static void each(CompoundTag tag, String key, Consumer<CompoundTag> f) {
        for (Tag t : tag.getList(key, Tag.TAG_COMPOUND)) f.accept((CompoundTag) t);
    }

    /** One {u: uuid, ...} compound per player, filled in by f. */
    private static <V> ListTag list(Map<UUID, V> map, BiConsumer<CompoundTag, V> f) {
        ListTag l = new ListTag();
        map.forEach((u, v) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("u", u);
            f.accept(c, v);
            l.add(c);
        });
        return l;
    }
}
