package de.hugosmp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Farbfeld zum Auswaehlen der GUI-Farbe. */
public class SW extends ButtonWidget {
    private final int idx;

    public SW(int x, int y, int w, int h, int idx, PressAction a) {
        super(x, y, w, h, Text.literal(Config.COLORS[idx]), a, DEFAULT_NARRATION_SUPPLIER);
        this.idx = idx;
    }

    @Override
    public void renderWidget(DrawContext g, int mx, int my, float delta) {
        boolean sel = Config.I.guiColor == idx;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int edge = sel ? 0xFFFFFFFF : isHovered() ? 0xFFC8CCE8 : 0xFF7580B8;
        Gui.round(g, x, y, x + w, y + h, 6, edge);
        int in = sel ? 3 : 2;
        if (idx == Config.COLORS.length - 1) { // Mix: mehrere Farben nebeneinander
            int n = Config.COLORS.length - 1, bw = (w - 2 * in) / 3;
            Gui.round(g, x + in, y + in, x + w - in, y + h - in, 4, 0xFF000000 | Config.COLOR_RGB[0]);
            g.fill(x + in + bw, y + in, x + in + 2 * bw, y + h - in, 0xFF000000 | Config.COLOR_RGB[2]);
            g.fill(x + in + 2 * bw, y + in + 2, x + w - in - 2, y + h - in - 2, 0xFF000000 | Config.COLOR_RGB[1]);
        } else {
            Gui.round(g, x + in, y + in, x + w - in, y + h - in, 4, 0xFF000000 | Config.COLOR_RGB[idx]);
        }
    }
}
