package io.github.ary82.royaltitles.client;

import io.github.ary82.royaltitles.TitleConfig;
import io.github.ary82.royaltitles.TitleData;
import io.github.ary82.royaltitles.net.EquipPacket;
import io.github.ary82.royaltitles.net.NametagPacket;
import io.github.ary82.royaltitles.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.*;

/**
 * Lists every player's titles as colored chips. Hover for the meaning,
 * click one of YOUR chips to equip it as your main title (click again to unequip).
 */
public class TitlesScreen extends Screen {
    private static final int PAD = 8, CHIP_H = 13, GAP = 3, ROW_GAP = 9, TOP = 28;

    private record Chip(int x, int y, int w, int h, String id, UUID owner) {
        boolean hit(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Chip> chips = new ArrayList<>();
    private double scroll = 0;
    private int maxScroll = 0;
    private Button nametagButton;

    public TitlesScreen() {
        super(Component.translatable("screen.royal_titles.title"));
    }

    @Override
    protected void init() {
        nametagButton = addRenderableWidget(Button.builder(Component.empty(),
                        b -> Net.CHANNEL.sendToServer(new NametagPacket(!showsNametag())))
                .bounds(x1() + panelW() - 100, y2() + 4, 100, 20).build());
    }

    private boolean showsNametag() { return TitleData.CLIENT.nametag.contains(self()); }

    @Override
    public boolean isPauseScreen() { return false; }

    private int panelW() { return Math.min(width - 20, 380); }
    private int x1() { return width / 2 - panelW() / 2; }
    private int y2() { return height - 28; }

    private UUID self() { return minecraft.player.getUUID(); }

    private List<UUID> players(TitleData d) {
        List<UUID> list = new ArrayList<>();
        d.owned.forEach((u, set) -> { if (!set.isEmpty()) list.add(u); });
        UUID me = self();
        list.sort(Comparator
                .comparing((UUID u) -> !u.equals(me))
                .thenComparing(u -> nameOf(d, u).toLowerCase(Locale.ROOT)));
        return list;
    }

    private String nameOf(TitleData d, UUID u) {
        String n = d.names.get(u);
        return n != null ? n : u.toString().substring(0, 8);
    }

    /** Rarest first, then alphabetical. Skips ids whose definition was deleted. */
    private List<String> titlesOf(TitleConfig cfg, TitleData d, UUID u) {
        List<String> ids = new ArrayList<>();
        for (String id : d.owned.getOrDefault(u, Set.of())) if (cfg.titles.containsKey(id)) ids.add(id);
        ids.sort(Comparator
                .comparingInt((String id) -> -cfg.rarityIndex(cfg.titles.get(id).rarity))
                .thenComparing(id -> cfg.titles.get(id).name.toLowerCase(Locale.ROOT)));
        return ids;
    }

    private static int scale(int rgb, float f) {
        int r = (int) (((rgb >> 16) & 0xFF) * f), g = (int) (((rgb >> 8) & 0xFF) * f), b = (int) ((rgb & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void border(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        TitleData d = TitleData.CLIENT;
        TitleConfig cfg = TitleConfig.CLIENT;
        int x1 = x1(), x2 = x1 + panelW(), y1 = TOP, y2 = y2();
        int innerRight = x2 - PAD;
        UUID me = self();

        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        g.fill(x1, y1, x2, y2, 0xC0101010);
        border(g, x1, y1, x2 - x1, y2 - y1, 0xFF555555);

        chips.clear();
        List<UUID> list = players(d);
        int startY = y1 + PAD;
        int y = startY - (int) scroll;

        g.enableScissor(x1 + 1, y1 + 1, x2 - 1, y2 - 1);
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.literal("No titles have been granted yet."),
                    width / 2, y1 + 20, 0xAAAAAA);
        }
        for (UUID pid : list) {
            boolean mine = pid.equals(me);
            g.drawString(font, nameOf(d, pid) + (mine ? " (you)" : ""), x1 + PAD, y, mine ? 0xFFFF55 : 0xFFFFFF);
            y += font.lineHeight + 3;
            int cx = x1 + PAD;
            String eq = d.equipped.get(pid);
            for (String id : titlesOf(cfg, d, pid)) {
                TitleConfig.Title def = cfg.titles.get(id);
                int col = cfg.color(def.rarity);
                int w = font.width(def.name) + 8;
                if (cx + w > innerRight && cx > x1 + PAD) {
                    cx = x1 + PAD;
                    y += CHIP_H + GAP;
                }
                boolean isEq = id.equals(eq);
                Chip chip = new Chip(cx, y, w, CHIP_H, id, pid);
                boolean hover = chip.hit(mx, my) && my > y1 && my < y2 && mx > x1 && mx < x2;
                g.fill(cx, y, cx + w, y + CHIP_H, scale(col, hover ? 0.45f : 0.25f));
                border(g, cx, y, w, CHIP_H, isEq ? 0xFFFFFFFF : (0xFF000000 | col));
                g.drawString(font, def.name, cx + 4, y + 3, 0xFF000000 | col);
                chips.add(chip);
                cx += w + GAP;
            }
            y += CHIP_H + ROW_GAP;
        }
        int contentH = (y + (int) scroll) - startY;
        maxScroll = Math.max(0, contentH - (y2 - y1 - PAD * 2));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        g.disableScissor();

        if (maxScroll > 0) {
            int trackH = y2 - y1 - 4;
            int barH = Math.max(16, trackH * trackH / (trackH + maxScroll));
            int barY = y1 + 2 + (int) ((trackH - barH) * (scroll / maxScroll));
            g.fill(x2 - 4, barY, x2 - 2, barY + barH, 0xFF888888);
        }

        String eq = d.equipped.get(me);
        String footer = eq != null && cfg.titles.containsKey(eq)
                ? "Main title: " + cfg.titles.get(eq).name
                : "Click your title to equip it";
        g.drawString(font, footer, x1, y2 + 10, 0xAAAAAA);
        nametagButton.setMessage(Component.literal("Nametag: " + (showsNametag() ? "On" : "Off")));

        // tooltip
        if (mx > x1 && mx < x2 && my > y1 && my < y2) {
            for (Chip chip : chips) {
                if (!chip.hit(mx, my)) continue;
                TitleConfig.Title def = cfg.titles.get(chip.id);
                int col = cfg.color(def.rarity);
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(def.name).setStyle(Style.EMPTY.withColor(col).withBold(true)));
                String r = def.rarity.isEmpty() ? "" : Character.toUpperCase(def.rarity.charAt(0)) + def.rarity.substring(1);
                tip.add(Component.literal(r).setStyle(Style.EMPTY.withColor(col)));
                String desc = def.description.isEmpty() ? "No description." : def.description;
                for (FormattedText line : font.getSplitter().splitLines(desc, 200, Style.EMPTY)) {
                    tip.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
                }
                if (chip.id.equals(d.equipped.get(chip.owner))) {
                    tip.add(Component.literal("Main title").withStyle(ChatFormatting.GREEN));
                }
                if (chip.owner.equals(me)) {
                    tip.add(Component.literal(chip.id.equals(d.equipped.get(me)) ? "Click to unequip" : "Click to equip")
                            .withStyle(ChatFormatting.YELLOW));
                }
                g.renderComponentTooltip(font, tip, mx, my);
                break;
            }
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && my > TOP && my < y2() && mx > x1() && mx < x1() + panelW()) {
            for (Chip chip : chips) {
                if (chip.hit(mx, my) && chip.owner.equals(self())) {
                    String eq = TitleData.CLIENT.equipped.get(self());
                    Net.CHANNEL.sendToServer(new EquipPacket(chip.id.equals(eq) ? "" : chip.id));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - delta * 14));
        return true;
    }
}
