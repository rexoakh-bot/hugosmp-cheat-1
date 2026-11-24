package de.hugosmp;

import net.minecraft.block.Block;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Zweites Fenster fuer Block ESP: Suchfeld + scrollbare Liste aller Bloecke. Speichert nur die Auswahl. */
public class BlockPickerScreen extends Screen {
    private static final int ROW_H = 22;
    private static final int PANEL = 0xFF2A2E45, ROW = 0xFF3F4670, DIM = 0xFFBCC1DD;

    private static class Entry {
        final String id, name, lower; final ItemStack stack;
        Entry(String id, String name, ItemStack stack) { this.id = id; this.name = name; this.lower = (name + " " + id).toLowerCase(); this.stack = stack; }
    }

    private static List<Entry> ALL;
    private final Screen parent;
    private final List<Entry> filtered = new ArrayList<>();
    private TextFieldWidget search;
    private int px, py, pw, ph, listTop, listBottom, scroll = 0;
    private int tpx, tpy, tpw = 118, tph = 100;   // kleines Tracer-Fenster

    public BlockPickerScreen(Screen parent) {
        super(Text.literal("Block ESP"));
        this.parent = parent;
        if (ALL == null) {
            ALL = new ArrayList<>();
            for (Block b : Registries.BLOCK) {
                Item it = b.asItem();
                if (it == Items.AIR) continue;
                ALL.add(new Entry(Registries.BLOCK.getId(b).toString(), b.getName().getString(), new ItemStack(it)));
            }
            ALL.sort(Comparator.comparing(e -> e.name));
        }
    }

    @Override
    protected void init() {
        pw = Math.min(width - 20, 340);
        ph = Math.min(height - 20, 320);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        listTop = py + 66;
        listBottom = py + ph - 68;
        search = new TextFieldWidget(textRenderer, px + 12, py + 38, pw - 24, 20, Text.literal("Suchen"));
        search.setMaxLength(40);
        search.setPlaceholder(Text.literal("Block suchen..."));
        search.setChangedListener(v -> { scroll = 0; refilter(); });
        addDrawableChild(search);
        setInitialFocus(search);
        tpx = px + pw + 8;
        if (tpx + tpw > width - 4) tpx = px - tpw - 8;
        if (tpx < 4) tpx = Math.max(4, px + pw - tpw);
        tpy = py;
        addDrawableChild(RB.builder(Text.literal(Config.I.tracer ? "AN" : "AUS"), b -> {
            Config.I.tracer = !Config.I.tracer;
            Config.save();
            b.setMessage(Text.literal(Config.I.tracer ? "AN" : "AUS"));
        }).dimensions(tpx + 12, tpy + 30, tpw - 24, 22).build());
        addDrawableChild(new AlphaSlider(tpx + 12, tpy + 78, tpw - 24, 14));
        int[] ranges = {8, 16, 32, 64};
        RB[] rbs = new RB[ranges.length];
        for (int i = 0; i < ranges.length; i++) {
            final int r = ranges[i], ii = i;
            rbs[i] = RB.builder(Text.literal(String.valueOf(r)), b -> {
                Config.I.blockEspRange = r; Config.save();
                for (int k = 0; k < rbs.length; k++) rbs[k].selected = ranges[k] == r;
            }).sel(Config.I.blockEspRange == r).dimensions(px + 100 + i * 52, py + ph - 62, 46, 18).build();
            addDrawableChild(rbs[i]);
        }
        addDrawableChild(RB.builder(Text.literal("Fertig"), b -> close())
                .dimensions(px + pw / 2 - 45, py + ph - 30, 90, 22).build());
        refilter();
    }

    private void refilter() {
        filtered.clear();
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        for (Entry e : ALL) if (q.isEmpty() || e.lower.contains(q)) filtered.add(e);
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int visibleRows() { return Math.max(1, (listBottom - listTop) / ROW_H); }
    private int maxScroll() { return Math.max(0, filtered.size() - visibleRows()); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(verticalAmount) * 3));
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() == 0 && click.x() >= px + 12 && click.x() <= px + pw - 18 && click.y() >= listTop && click.y() < listBottom) {
            int idx = scroll + (int) ((click.y() - listTop) / ROW_H);
            if (idx >= 0 && idx < filtered.size()) {
                String id = filtered.get(idx).id;
                List<String> sel = Config.I.blockEspBlocks;
                if (!sel.remove(id)) sel.add(id);
                Config.save();
                return true;
            }
        }
        return false;
    }

    @Override public void close() { client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }

    private void txt(DrawContext g, String s, int x, int y, int color) {
        g.drawText(textRenderer, Text.literal(s).formatted(Formatting.BOLD), x, y, color, true);
    }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        parent.render(g, -1, -1, delta);                  // Hauptmenue bleibt dahinter sichtbar
        g.fill(0, 0, width, height, 0x88000000);          // abdunkeln
        int acc = Config.I.accent();
        Gui.round(g, px, py, px + pw, py + ph, 12, PANEL);
        g.fill(px + 14, py, px + pw - 14, py + 2, acc);
        txt(g, "Block ESP: Bloecke waehlen", px + 12, py + 12, 0xFFFFFFFF);
        String cnt = Config.I.blockEspBlocks.size() + " gewaehlt";
        txt(g, cnt, px + pw - 12 - textRenderer.getWidth(Text.literal(cnt).formatted(Formatting.BOLD)), py + 12, DIM);

        // Kleines zweites Fenster: Tracer
        Gui.round(g, tpx, tpy, tpx + tpw, tpy + tph, 12, PANEL);
        g.fill(tpx + 14, tpy, tpx + tpw - 14, tpy + 2, acc);
        txt(g, "Tracer", tpx + 12, tpy + 12, 0xFFFFFFFF);
        txt(g, "Alpha " + Config.I.blockAlpha + "%", tpx + 12, tpy + 62, 0xFFF0F2FF);

        int rows = visibleRows();
        for (int i = 0; i < rows; i++) {
            int idx = scroll + i;
            if (idx >= filtered.size()) break;
            Entry e = filtered.get(idx);
            int ry = listTop + i * ROW_H;
            boolean sel = Config.I.blockEspBlocks.contains(e.id);
            boolean hov = mx >= px + 12 && mx <= px + pw - 18 && my >= ry && my < ry + ROW_H;
            Gui.round(g, px + 12, ry, px + pw - 18, ry + ROW_H - 2, 6, sel ? 0xFF2F6F55 : hov ? 0xFF4A5280 : ROW);
            g.drawItem(e.stack, px + 16, ry + 1);
            txt(g, textRenderer.trimToWidth(e.name, pw - 100), px + 38, ry + 7, 0xFFF0F2FF);
            if (sel) {
                int cx = px + pw - 36, cy = ry + 10;
                for (int[] q : new int[][]{{0, 0}, {1, 1}, {2, 2}, {3, 1}, {4, 0}, {5, -1}, {6, -2}})
                    g.fill(cx + q[0], cy + q[1], cx + q[0] + 1, cy + q[1] + 2, 0xFFFFFFFF);
            }
        }
        // Scrollbar
        if (maxScroll() > 0) {
            int trackH = listBottom - listTop;
            int barH = Math.max(16, trackH * rows / filtered.size());
            int barY = listTop + (trackH - barH) * scroll / maxScroll();
            Gui.round(g, px + pw - 14, listTop, px + pw - 10, listBottom, 2, 0xFF3F4670);
            Gui.round(g, px + pw - 14, barY, px + pw - 10, barY + barH, 2, acc);
        }
        txt(g, "Reichweite:", px + 12, py + ph - 58, DIM);
        if (filtered.isEmpty()) txt(g, "Kein Block gefunden", px + 16, listTop + 8, DIM);
        super.render(g, mx, my, delta);
    }
}
