package io.github.ary82.royaltitles;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Players:  /royal equip <title>   /royal unequip   /royal nametag on|off
 * Ops (level 2), all saved to config/royal_titles.json:
 *   /royal title create|describe|rename|rarity|delete|list ...
 *   /royal rarity set|remove|list ...
 *   /royal reload
 * Ops, saved with the world:
 *   /royal grant|revoke <players> <id>  (works for offline players too)
 */
public final class RoyalCommands {
    private static final SuggestionProvider<CommandSourceStack> ALL_TITLES = (c, b) ->
            SharedSuggestionProvider.suggest(cfg().titles.keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> RARITIES = (c, b) ->
            SharedSuggestionProvider.suggest(cfg().rarities.keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> OWNED = (c, b) ->
            SharedSuggestionProvider.suggest(TitleManager.data().owned
                    .getOrDefault(c.getSource().getPlayerOrException().getUUID(), Set.of()), b);

    private RoyalCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("royal")
            // ---- players ----
            .then(literal("equip")
                .then(argument("id", word()).suggests(OWNED).executes(c -> equip(c, arg(c, "id")))))
            .then(literal("unequip").executes(c -> equip(c, "")))
            .then(literal("nametag")
                .then(literal("on").executes(c -> nametag(c, true)))
                .then(literal("off").executes(c -> nametag(c, false))))

            // ---- ops: titles ----
            .then(op("title")
                .then(literal("create")
                    .then(argument("id", word())
                        .then(argument("rarity", word()).suggests(RARITIES)
                            .then(argument("name", greedyString()).executes(RoyalCommands::create)))))
                .then(literal("describe")
                    .then(argument("id", word()).suggests(ALL_TITLES)
                        .then(argument("text", greedyString()).executes(RoyalCommands::describe))))
                .then(literal("rename")
                    .then(argument("id", word()).suggests(ALL_TITLES)
                        .then(argument("name", greedyString()).executes(RoyalCommands::rename))))
                .then(literal("rarity")
                    .then(argument("id", word()).suggests(ALL_TITLES)
                        .then(argument("rarity", word()).suggests(RARITIES).executes(RoyalCommands::setRarity))))
                .then(literal("delete")
                    .then(argument("id", word()).suggests(ALL_TITLES).executes(RoyalCommands::delete)))
                .then(literal("list").executes(RoyalCommands::listTitles)))

            // ---- ops: rarities ----
            .then(op("rarity")
                .then(literal("set")
                    .then(argument("name", word()).suggests(RARITIES)
                        .then(argument("color", word()).executes(RoyalCommands::raritySet))))
                .then(literal("remove")
                    .then(argument("name", word()).suggests(RARITIES).executes(RoyalCommands::rarityRemove)))
                .then(literal("list").executes(RoyalCommands::rarityList)))

            // ---- ops: config / grant / revoke ----
            .then(op("reload").executes(RoyalCommands::reload))
            .then(op("grant")
                .then(argument("targets", GameProfileArgument.gameProfile())
                    .then(argument("id", word()).suggests(ALL_TITLES).executes(c -> grant(c, true)))))
            .then(op("revoke")
                .then(argument("targets", GameProfileArgument.gameProfile())
                    .then(argument("id", word()).suggests(ALL_TITLES).executes(c -> grant(c, false))))));
    }

    // ---------- helpers ----------
    private static TitleConfig cfg() { return TitleConfig.SERVER; }

    private static LiteralArgumentBuilder<CommandSourceStack> op(String name) {
        return literal(name).requires(s -> s.hasPermission(2));
    }

    private static String arg(CommandContext<CommandSourceStack> c, String name) {
        return getString(c, name).toLowerCase(Locale.ROOT);
    }

    private static int fail(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendFailure(Component.literal(msg));
        return 0;
    }

    private static int ok(CommandContext<CommandSourceStack> c, Component msg) {
        c.getSource().sendSuccess(() -> msg, true);
        return Command.SINGLE_SUCCESS;
    }

    private static Component colored(String id) {
        TitleConfig.Title t = cfg().titles.get(id);
        return Component.literal(t.name).setStyle(Style.EMPTY.withColor(cfg().color(t.rarity)));
    }

    /** Writes the config file and pushes the change to everyone. */
    private static int saved(CommandContext<CommandSourceStack> c, Component msg) {
        TitleConfig.save();
        TitleManager.changed();
        return ok(c, msg);
    }

    /** Config edits are refused while the file has errors, so a hand edit is never overwritten. */
    private static boolean locked(CommandContext<CommandSourceStack> c) {
        if (TitleConfig.isBroken()) fail(c, "config/royal_titles.json has errors. Fix it, then run /royal reload.");
        return TitleConfig.isBroken();
    }

    /** Applies edit to an existing title, then saves and reports msg. */
    private static int editTitle(CommandContext<CommandSourceStack> c, String msg, Consumer<TitleConfig.Title> edit) {
        if (locked(c)) return 0;
        String id = arg(c, "id");
        TitleConfig.Title t = cfg().titles.get(id);
        if (t == null) return fail(c, "No such title: " + id);
        edit.accept(t);
        return saved(c, Component.literal(msg).append(colored(id)));
    }

    // ---------- player ----------
    private static int equip(CommandContext<CommandSourceStack> c, String id) throws CommandSyntaxException {
        String err = TitleManager.equip(c.getSource().getPlayerOrException(), id);
        if (err != null) return fail(c, err);
        c.getSource().sendSuccess(() -> id.isEmpty()
                ? Component.literal("Title unequipped.")
                : Component.literal("Equipped: ").append(colored(id)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int nametag(CommandContext<CommandSourceStack> c, boolean show) throws CommandSyntaxException {
        TitleManager.setNametag(c.getSource().getPlayerOrException(), show);
        c.getSource().sendSuccess(() -> Component.literal("Title " + (show ? "shown" : "hidden") + " above your head."), false);
        return Command.SINGLE_SUCCESS;
    }

    // ---------- titles ----------
    private static int create(CommandContext<CommandSourceStack> c) {
        if (locked(c)) return 0;
        String id = arg(c, "id"), rarity = arg(c, "rarity");
        if (cfg().titles.containsKey(id)) return fail(c, "Title already exists: " + id);
        if (!cfg().rarities.containsKey(rarity)) return fail(c, "Unknown rarity: " + rarity);
        cfg().titles.put(id, new TitleConfig.Title(getString(c, "name").trim(), rarity));
        return saved(c, Component.literal("Created title ").append(colored(id)));
    }

    private static int describe(CommandContext<CommandSourceStack> c) {
        return editTitle(c, "Updated description of ", t -> t.description = getString(c, "text"));
    }

    private static int rename(CommandContext<CommandSourceStack> c) {
        return editTitle(c, "Renamed to ", t -> t.name = getString(c, "name").trim());
    }

    private static int setRarity(CommandContext<CommandSourceStack> c) {
        String rarity = arg(c, "rarity");
        if (!cfg().rarities.containsKey(rarity)) return fail(c, "Unknown rarity: " + rarity);
        return editTitle(c, "Rarity set: ", t -> t.rarity = rarity);
    }

    private static int delete(CommandContext<CommandSourceStack> c) {
        if (locked(c)) return 0;
        String id = arg(c, "id");
        if (cfg().titles.remove(id) == null) return fail(c, "No such title: " + id);
        TitleData d = TitleManager.data();
        d.owned.values().forEach(s -> s.remove(id));
        d.equipped.values().removeIf(id::equals);
        return saved(c, Component.literal("Deleted title " + id));
    }

    private static int listTitles(CommandContext<CommandSourceStack> c) {
        if (cfg().titles.isEmpty()) return fail(c, "No titles defined yet.");
        cfg().titles.forEach((id, t) -> c.getSource().sendSuccess(() ->
                Component.literal(id + " - ").append(colored(id))
                        .append(Component.literal(" (" + t.rarity + ")").withStyle(ChatFormatting.GRAY)), false));
        return cfg().titles.size();
    }

    // ---------- rarities ----------
    private static int raritySet(CommandContext<CommandSourceStack> c) {
        if (locked(c)) return 0;
        String name = arg(c, "name");
        Integer col = TitleConfig.parseColor(getString(c, "color"));
        if (col == null) return fail(c, "Color must be hex like FFD700 or a name like gold.");
        String hex = String.format("#%06X", col);
        cfg().rarities.put(name, hex); // new rarities are appended as the rarest
        return saved(c, Component.literal("Rarity " + name + " = " + hex).setStyle(Style.EMPTY.withColor(col)));
    }

    private static int rarityRemove(CommandContext<CommandSourceStack> c) {
        if (locked(c)) return 0;
        String name = arg(c, "name");
        if (!cfg().rarities.containsKey(name)) return fail(c, "Unknown rarity: " + name);
        if (cfg().titles.values().stream().anyMatch(t -> t.rarity.equals(name))) return fail(c, "Titles still use that rarity.");
        cfg().rarities.remove(name);
        return saved(c, Component.literal("Removed rarity " + name));
    }

    private static int rarityList(CommandContext<CommandSourceStack> c) {
        cfg().rarities.forEach((n, col) -> c.getSource().sendSuccess(() ->
                Component.literal(n + " " + col).setStyle(Style.EMPTY.withColor(cfg().color(n))), false));
        return cfg().rarities.size();
    }

    private static int reload(CommandContext<CommandSourceStack> c) {
        String err = TitleConfig.load();
        if (err != null) return fail(c, "Couldn't load config/royal_titles.json: " + err);
        TitleManager.changed();
        return ok(c, Component.literal("Reloaded " + cfg().titles.size() + " titles and " + cfg().rarities.size() + " rarities."));
    }

    // ---------- grant / revoke ----------
    private static int grant(CommandContext<CommandSourceStack> c, boolean give) throws CommandSyntaxException {
        String id = arg(c, "id");
        if (!cfg().titles.containsKey(id)) return fail(c, "No such title: " + id);
        int n = 0;
        for (GameProfile gp : GameProfileArgument.getGameProfiles(c, "targets")) {
            if (give ? TitleManager.grant(gp.getId(), gp.getName(), id) : TitleManager.revoke(gp.getId(), id)) n++;
        }
        if (n == 0) return fail(c, give ? "Nobody gained the title (already owned?)." : "Nobody had that title.");
        TitleManager.changed();
        return ok(c, Component.literal(give ? "Granted " : "Revoked ").append(colored(id))
                .append(Component.literal((give ? " to " : " from ") + n + " player(s)")));
    }
}
