package de.hugosmp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Kleines Fenster fuer Player ESP: Tracer (AN/AUS) und Alpha-Regler. */
public class PlayerEspScreen extends Screen {
    private static final int PANEL = 0xFF2A2E45, DIM = 0xFFBCC1DD;
    private final Screen parent;
    private int px, py, pw = 130, ph = 186;

    public PlayerEspScreen(Screen parent) {
        super(Text.literal("Player ESP"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        addDrawableChild(RB.builder(Text.literal(Config.I.playerTracer ? "AN" : "AUS"), b -> {
            Config.I.playerTracer = !Config.I.playerTracer;
            Config.save();
            b.setMessage(Text.literal(Config.I.playerTracer ? "AN" : "AUS"));
        }).dimensions(px + 12, py + 46, pw - 24, 22).build());
        addDrawableChild(new AlphaSlider(px + 12, py + 94, pw - 24, 14, true));
        int[] ranges = {16, 32, 64, 128};
        RB[] rbs = new RB[ranges.length];
        for (int i = 0; i < ranges.length; i++) {
            final int r = ranges[i];
            rbs[i] = RB.builder(Text.literal(String.valueOf(r)), b -> {
                Config.I.playerRange = r; Config.save();
                for (int k = 0; k < rbs.length; k++) rbs[k].selected = ranges[k] == r;
            }).sel(Config.I.playerRange == r).dimensions(px + 12 + i * 27, py + 132, 24, 18).build();
            addDrawableChild(rbs[i]);
        }
        addDrawableChild(RB.builder(Text.literal("Fertig"), b -> close())
                .dimensions(px + pw / 2 - 40, py + ph - 30, 80, 22).build());
    }

    @Override public void close() { client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }

    private void txt(DrawContext g, String s, int x, int y, int color) {
        g.drawText(textRenderer, Text.literal(s).formatted(Formatting.BOLD), x, y, color, true);
    }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        parent.render(g, -1, -1, delta);
        g.fill(0, 0, width, height, 0x88000000);
        Gui.round(g, px, py, px + pw, py + ph, 12, PANEL);
        g.fill(px + 14, py, px + pw - 14, py + 2, Config.I.accent());
        txt(g, "Player ESP", px + 12, py + 12, 0xFFFFFFFF);
        txt(g, "Tracer", px + 12, py + 32, 0xFFF0F2FF);
        txt(g, "Alpha " + Config.I.playerAlpha + "%", px + 12, py + 78, 0xFFF0F2FF);
        txt(g, "Reichweite:", px + 12, py + 118, 0xFFF0F2FF);
        super.render(g, mx, my, delta);
    }
}
