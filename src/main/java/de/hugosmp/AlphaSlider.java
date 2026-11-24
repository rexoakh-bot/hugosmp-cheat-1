package de.hugosmp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

/** Regler fuer die Deckkraft (Alpha) der Block-Farbe: 5 bis 100 Prozent. */
public class AlphaSlider extends SliderWidget {
    private final boolean players;

    public AlphaSlider(int x, int y, int w, int h) { this(x, y, w, h, false); }

    public AlphaSlider(int x, int y, int w, int h, boolean players) {
        super(x, y, w, h, Text.empty(), ((players ? Config.I.playerAlpha : Config.I.blockAlpha) - 5) / 95.0);
        this.players = players;
    }

    @Override protected void updateMessage() { setMessage(Text.empty()); }

    @Override
    protected void applyValue() {
        int a = 5 + (int) Math.round(this.value * 95);
        if (players) Config.I.playerAlpha = a; else Config.I.blockAlpha = a;
        Config.save();
    }

    @Override
    public void renderWidget(DrawContext g, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int ty = y + h / 2 - 2;
        Gui.round(g, x, ty, x + w, ty + 4, 2, 0xFF3F4670);                       // Schiene
        int knobX = x + (int) Math.round(this.value * (w - 8));
        Gui.round(g, x, ty, knobX + 4, ty + 4, 2, 0xFF4C9CFF);                   // gefuellter Teil (blau)
        Gui.round(g, knobX, y + 1, knobX + 8, y + h - 1, 3, isHovered() ? 0xFFFFFFFF : 0xFFE8EAF6); // Knopf
    }
}
