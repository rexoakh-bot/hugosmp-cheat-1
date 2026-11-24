package de.hugosmp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Runder Button + kleiner Builder (gleiche Aufrufe wie ButtonWidget.builder). */
public class RB extends ButtonWidget {
    public boolean selected;

    protected RB(int x, int y, int w, int h, Text msg, PressAction a, boolean sel) {
        super(x, y, w, h, msg, a, DEFAULT_NARRATION_SUPPLIER);
        this.selected = sel;
    }

    @Override
    public void renderWidget(DrawContext g, int mx, int my, float delta) {
        String label = getMessage().getString();
        boolean on = label.equals("AN") || label.endsWith(": AN");
        boolean waiting = label.equals("Taste drücken...");
        int bg = waiting ? 0xFF2C3150 : on ? (isHovered() ? 0xFF3C9B6E : 0xFF348F65) : selected ? 0xFF5C6599 : isHovered() ? 0xFF5F68A0 : 0xFF50588A;
        int edge = on ? 0xFF7BE8B0 : waiting ? Config.I.accent() : selected ? Config.I.accent() : 0xFF7580B8;
        Gui.round(g, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 6, edge);
        Gui.round(g, getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 5, bg);
        var tr = MinecraftClient.getInstance().textRenderer;
        if (label.equals("Eingabe")) // Platzhalter: nicht fett, etwas heller/ruhiger
            g.drawCenteredTextWithShadow(tr, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, 0xFFBCC1DD);
        else
            g.drawCenteredTextWithShadow(tr, getMessage().copy().formatted(Formatting.BOLD), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);
        if (label.equals("AN")) { // Haekchen links im Button
            int cx = getX() + 10, cy = getY() + getHeight() / 2 + 1;
            int[][] pts = {{0, 0}, {1, 1}, {2, 2}, {3, 1}, {4, 0}, {5, -1}, {6, -2}};
            for (int[] q : pts) g.fill(cx + q[0] * 2, cy + q[1] * 2 - 1, cx + q[0] * 2 + 2, cy + q[1] * 2 + 1, 0xFFFFFFFF);
        }
    }

    public static Builder builder(Text t, PressAction a) { return new Builder(t, a); }

    public static class Builder {
        Text t; PressAction a; int x, y, w, h; boolean sel;
        Builder(Text t, PressAction a) { this.t = t; this.a = a; }
        public Builder dimensions(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; return this; }
        public Builder sel(boolean s) { sel = s; return this; }
        public RB build() { return new RB(x, y, w, h, t, a, sel); }
    }
}
