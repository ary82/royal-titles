package io.github.ary82.royaltitles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Title and rarity definitions. The server keeps them in config/royal_titles.json
 * (edited by hand or through /royal commands) and syncs a copy to clients.
 */
public class TitleConfig {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve(RoyalTitles.MODID + ".json");

    public static TitleConfig SERVER = defaults();
    public static TitleConfig CLIENT = new TitleConfig();
    /** Set when the file failed to parse, so we never overwrite the admin's broken edit. */
    private static boolean broken;

    public static class Title {
        public String name, description = "", rarity;
        Title() {}
        public Title(String name, String rarity) { this.name = name; this.rarity = rarity; }
    }

    /** rarity -> color ("#FFAA00" or a name like "gold"), from most common to rarest. */
    public Map<String, String> rarities = new LinkedHashMap<>();
    public Map<String, Title> titles = new LinkedHashMap<>();

    private static TitleConfig defaults() {
        TitleConfig c = new TitleConfig();
        c.rarities.put("common", "#AAAAAA");
        c.rarities.put("uncommon", "#55FF55");
        c.rarities.put("rare", "#5588FF");
        c.rarities.put("epic", "#AA55FF");
        c.rarities.put("legendary", "#FFAA00");
        return c;
    }

    public int color(String rarity) {
        Integer c = parseColor(rarities.getOrDefault(rarity, ""));
        return c != null ? c : 0xAAAAAA;
    }

    public int rarityIndex(String rarity) {
        return List.copyOf(rarities.keySet()).indexOf(rarity);
    }

    /** Hex like FFD700 / #FFD700, or a chat color name like gold. */
    public static Integer parseColor(String s) {
        if (s.startsWith("#")) s = s.substring(1);
        if (s.matches("[0-9a-fA-F]{6}")) return Integer.parseInt(s, 16);
        ChatFormatting f = ChatFormatting.getByName(s);
        return f != null && f.isColor() ? f.getColor() : null;
    }

    public String toJson() { return GSON.toJson(this); }

    public static TitleConfig fromJson(String json) {
        TitleConfig c = GSON.fromJson(json, TitleConfig.class);
        if (c == null) throw new JsonParseException("file is empty");
        if (c.rarities == null) c.rarities = new LinkedHashMap<>();
        if (c.titles == null) c.titles = new LinkedHashMap<>();
        c.titles.forEach((id, t) -> {
            if (t.name == null) t.name = id;
            if (t.description == null) t.description = "";
            if (t.rarity == null) t.rarity = "";
        });
        return c;
    }

    public static boolean isBroken() { return broken; }

    /** Loads the server config, creating it with defaults if missing. Returns an error message, or null. */
    public static String load() {
        try {
            if (Files.notExists(FILE)) {
                SERVER = defaults();
                broken = false;
                save();
            } else {
                SERVER = fromJson(Files.readString(FILE));
                broken = false;
            }
            return null;
        } catch (IOException | JsonParseException e) {
            broken = true;
            LOG.error("Couldn't load {}, keeping previous titles until it's fixed: {}", FILE, e.getMessage());
            return e.getMessage();
        }
    }

    public static void save() {
        if (broken) return;
        try {
            Files.writeString(FILE, SERVER.toJson());
        } catch (IOException e) {
            LOG.error("Couldn't save {}", FILE, e);
        }
    }
}
