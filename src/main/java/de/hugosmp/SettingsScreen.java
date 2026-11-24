package de.hugosmp;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class SettingsScreen extends Screen {
    private static final String[] TABS = {"InvSee", "Visuals", "Schutz", "Item Protect", "GUI", "Erlaubte Server"};
    private static int tab = 0;
    private int x, y, w, h, side;
    private boolean pickerOpen = false, guardPickerOpen = false, ahEditing = false;
    private int rebinding = -1; // 0 = InvSee, 1 = AH Sell Macro
    private ButtonWidget keyBtn0, keyBtn1;

    public SettingsScreen() { super(Text.literal("cavevfx Mod")); }

    private static final int BG = 0xEE2A2E45, PANEL = 0xFF343A5C, ROW = 0xFF3F4670, DIM = 0xFFBCC1DD;

    @Override
    protected void init() {
        Config c = Config.I;
        float s = c.guiScale / 100f;
        w = (int) Math.min(width - 10, 1020 * s);
        h = (int) Math.min(height - 10, 477 * s);
        w = Math.max(w, 420); h = Math.max(h, 300);
        x = (width - w) / 2 + c.guiX; y = (height - h) / 2 + c.guiY;
        side = Math.max(110, w / 4);
        clearChildren();
        keyBtn0 = keyBtn1 = null;

        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            addDrawableChild(RB.builder(Text.literal(TABS[i]), b -> { tab = t; rebinding = -1; pickerOpen = false; guardPickerOpen = false; ahEditing = false; init(); })
                    .sel(tab == i).dimensions(x + 8, y + 40 + i * 26, side - 16, 22).build());
        }

        int cx = x + side + 16, cw = w - side - 32, ry = y + 70, bw = 110;
        switch (tab) {
            case 0 -> {
                keyBtn0 = row("InvSee-Taste", keyName(c.invSeeKey), cx, ry + 28, cw, bw, b -> { rebinding = 0; refreshKeys(); });
                row("InvSee", onOff(c.autoInvSeeRtp), cx, ry, cw, bw, b -> { c.autoInvSeeRtp = !c.autoInvSeeRtp; save(); });
            }
            case 1 -> {
                row("Player ESP", onOff(c.playerEsp), cx, ry, cw, bw, b -> { c.playerEsp = !c.playerEsp; save(); });
                row("Block ESP", onOff(c.blockEsp), cx, ry + 28, cw, bw, b -> { c.blockEsp = !c.blockEsp; save(); });
                row("AH Sell Macro", onOff(c.ahSellMacro), cx, ry + 56, cw, bw, b -> { c.ahSellMacro = !c.ahSellMacro; save(); });
                keyBtn1 = RB.builder(Text.literal(keyName(c.ahKey)), b -> { rebinding = 1; refreshKeys(); })
                        .dimensions(cx + cw - bw - 8, ry + 56 + 27, bw, 20).build();
                addDrawableChild(keyBtn1);
                row("GUI-Farben", Config.COLORS[c.guiColor], cx, ry + 112 + (ahEditing ? 50 : 0), cw, bw, b -> { pickerOpen = !pickerOpen; ahEditing = false; init(); });
                if (ahEditing) {
                    TextFieldWidget tf = new TextFieldWidget(textRenderer, cx, ry + 124, cw, 20, Text.literal("AH Text"));
                    tf.setMaxLength(100);
                    tf.setText(c.ahText);
                    tf.setChangedListener(v -> { c.ahText = v; Config.save(); });
                    addDrawableChild(tf);
                    setInitialFocus(tf);
                }
                if (pickerOpen) {
                    int sx = cx, sy = ry + 112 + 38;
                    for (int i = 0; i < Config.COLORS.length; i++) {
                        final int ci = i;
                        addDrawableChild(new SW(sx + (i % 5) * 46, sy + (i / 5) * 28, 40, 22, i, b -> { c.guiColor = ci; Config.save(); pickerOpen = false; init(); }));
                    }
                }
            }
            case 2 -> {
                row("Durability Guard", onOff(c.durabilityGuard), cx, ry, cw, bw, b -> { c.durabilityGuard = !c.durabilityGuard; save(); });
                row("Guard-Schwelle", c.guardThreshold + "%", cx, ry + 28, cw, bw, b -> { guardPickerOpen = !guardPickerOpen; init(); });
                if (guardPickerOpen) {
                    int[] opts = {5, 15, 20, 40};
                    for (int i = 0; i < opts.length; i++) {
                        final int v = opts[i];
                        addDrawableChild(RB.builder(Text.literal(v + "%"), b -> { c.guardThreshold = v; Config.save(); guardPickerOpen = false; init(); })
                                .sel(c.guardThreshold == v).dimensions(cx + i * 66, ry + 60, 60, 22).build());
                    }
                }
            }
            case 3 -> {
                int colW = (cw - 8) / 2, i = 0;
                for (String g : Config.GROUPS) {
                    int gx = cx + (i % 2) * (colW + 8), gy = ry - 8 + (i / 2) * 26;
                    addDrawableChild(RB.builder(Text.literal(g + ": " + onOff(c.itemProtect.get(g))), b -> {
                        c.itemProtect.put(g, !c.itemProtect.get(g)); save();
                    }).dimensions(gx, gy, colW, 22).build());
                    i++;
                }
            }
            case 4 -> {
                int by = ry;
                addDrawableChild(RB.builder(Text.literal("-"), b -> { c.guiScale = Math.max(50, c.guiScale - 5); save(); }).dimensions(x + w - 150, by, 30, 20).build());
                addDrawableChild(RB.builder(Text.literal("+"), b -> { c.guiScale = Math.min(120, c.guiScale + 5); save(); }).dimensions(x + w - 50, by, 30, 20).build());
                int ay = ry + 70;
                String[] arr = {"<", ">", "^", "v"};
                for (int i = 0; i < 4; i++) {
                    final int d = i;
                    addDrawableChild(RB.builder(Text.literal(arr[i]), b -> {
                        if (d == 0) c.guiX -= 10; else if (d == 1) c.guiX += 10; else if (d == 2) c.guiY -= 10; else c.guiY += 10; save();
                    }).dimensions(cx + i * 62, ay, 56, 22).build());
                }
                addDrawableChild(RB.builder(Text.literal("Position zuruecksetzen"), b -> { c.guiX = 0; c.guiY = 0; save(); })
                        .dimensions(cx, ay + 34, 180, 22).build());
            }
            case 5 -> {
                TextFieldWidget sf = new TextFieldWidget(textRenderer, cx, ry, cw, 20, Text.literal("Server"));
                sf.setMaxLength(120);
                sf.setPlaceholder(Text.literal("z. B. localhost, mein.server.de"));
                sf.setText(c.espServers);
                sf.setChangedListener(v -> { c.espServers = v; Config.save(); });
                addDrawableChild(sf);
            }
        }
    }

    private ButtonWidget row(String label, String val, int rx, int ry, int rw, int bw, ButtonWidget.PressAction a) {
        ButtonWidget b = RB.builder(Text.literal(val), a).dimensions(rx + rw - bw - 8, ry + 3, bw, 20).build();
        addDrawableChild(b);
        return b;
    }
    private void save() { Config.save(); init(); }
    private void refreshKeys() {
        if (keyBtn0 != null) keyBtn0.setMessage(Text.literal(rebinding == 0 ? "Taste drücken..." : keyName(Config.I.invSeeKey)));
        if (keyBtn1 != null) keyBtn1.setMessage(Text.literal(rebinding == 1 ? "Taste drücken..." : keyName(Config.I.ahKey)));
    }
    private static String onOff(boolean b) { return b ? "AN" : "AUS"; }
    private static String keyName(int k) { if (k < 0) return "Eingabe"; return InputUtil.Type.KEYSYM.createFromCode(k).getLocalizedText().getString(); }

    /** Rechtsklick auf "AH Sell Macro" oeffnet/schliesst das Textfeld. */
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (tab == 1 && click.button() == 1) {
            int cx = x + side + 16, ry = y + 70;
            if (click.x() >= cx && click.x() <= cx + 140 && click.y() >= ry && click.y() <= ry + 26) {
                client.setScreen(new PlayerEspScreen(this));   // Rechtsklick auf Player ESP
                return true;
            }
            if (click.x() >= cx && click.x() <= cx + 140 && click.y() >= ry + 28 && click.y() <= ry + 54) {
                client.setScreen(new BlockPickerScreen(this));   // Rechtsklick auf Block ESP
                return true;
            }
            if (click.x() >= cx && click.x() <= cx + 140 && click.y() >= ry + 56 && click.y() <= ry + 108) {
                ahEditing = !ahEditing;
                pickerOpen = false;
                init();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (rebinding >= 0) {
            int k = input.key();
            if (k != GLFW.GLFW_KEY_ESCAPE) { if (rebinding == 0) Config.I.invSeeKey = k; else Config.I.ahKey = k; Config.save(); }
            rebinding = -1; refreshKeys();
            return true;
        }
        if (input.key() == HugoSmpClient.openKeyCode() && !(getFocused() instanceof TextFieldWidget)) { close(); return true; }
        return super.keyPressed(input);
    }

    private void txt(DrawContext g, String str, int tx, int ty, int color) {
        g.drawText(textRenderer, Text.literal(str).formatted(Formatting.BOLD), tx, ty, color, true);
    }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        Config c = Config.I;
        int acc = c.accent();
        Gui.round(g, x, y, x + w, y + h, 12, BG);
        g.fill(x + 14, y, x + w - 14, y + 2, acc);
        txt(g, "cavevfx Mod", x + 12, y + 12, 0xFFFFFFFF);
        txt(g, "v1.4.0", x + w - 14 - textRenderer.getWidth(Text.literal("v1.4.0").formatted(Formatting.BOLD)), y + 12, DIM);
        g.fill(x + side, y + 34, x + side + 1, y + h - 8, 0xFF5A628F);
        Gui.round(g, x + 5, y + 44 + tab * 26, x + 8, y + 58 + tab * 26, 1, acc);
        Gui.round(g, x + side + 8, y + 34, x + w - 10, y + h - 10, 10, PANEL);

        int cx = x + side + 16, cw = w - side - 32, ry = y + 70;
        String[][] head = {
            {"InvSee", "Inventar anschauen bei Gegnern."},
            {"Anzeige", "HUD und Farbschema der Mod anpassen"},
            {"Schutz", "Schutzfunktionen fuer dein Inventar und deine Ausruestung"},
            {"Item Protect", "Aktive Gruppen koennen nicht verschoben oder gedroppt werden"},
            {"GUI", "Groesse und Position des Einstellungsfensters"},
            {"Erlaubte Server", "Hier laufen Player ESP und Block ESP."}};
        txt(g, head[tab][0], cx, y + 40, 0xFFFFFFFF);
        txt(g, head[tab][1], cx, y + 54, DIM);

        int rows = switch (tab) { case 0 -> 2; case 1 -> 4; case 2 -> 2; default -> 0; };
        if (tab == 1) {
            Gui.round(g, cx, ry, cx + cw, ry + 26, 8, ROW);
            Gui.round(g, cx, ry + 28, cx + cw, ry + 54, 8, ROW);
            Gui.round(g, cx, ry + 56, cx + cw, ry + 108, 8, ROW);   // AH Sell Macro (hoeher: AUS + Eingabe untereinander)
            int go = ahEditing ? 50 : 0;
            Gui.round(g, cx, ry + 112 + go, cx + cw, ry + 138 + go, 8, ROW);
        } else if (tab != 3) for (int i = 0; i < rows; i++) Gui.round(g, cx, ry + i * 28, cx + cw, ry + i * 28 + 26, 8, ROW);
        String[][] labels = {{"InvSee", "InvSee-Taste"}, {"Player ESP", "Block ESP", "AH Sell Macro", "GUI-Farben"},
                {"Durability Guard", "Guard-Schwelle"}, {}, {}, {}};
        int[] ys1 = {9, 37, 78, 121 + (ahEditing ? 50 : 0)};
        for (int i = 0; i < labels[tab].length; i++) txt(g, labels[tab][i], cx + 10, ry + (tab == 1 ? ys1[i] : 9 + i * 28), 0xFFF0F2FF);

        int ny = tab == 1 ? ry + 146 : ry + rows * 28 + 8;
        switch (tab) {
            case 0 -> txt(g, "Oeffnet das Inventar des gefundenen Queue-Gegners.", cx, ny, DIM);
            case 1 -> { if (ahEditing) txt(g, "Text bei Tastendruck (z. B. /ah sell 100):", cx, ry + 112, DIM);
                        else if (pickerOpen) txt(g, "Farbe waehlen:", cx, ry + 112 + 28, DIM);
                        else {
                            String t = c.ahText.isBlank() ? "Rechtsklick auf AH Sell Macro: Text festlegen." : "Text: " + textRenderer.trimToWidth(c.ahText, cw - 60);
                            txt(g, t, cx, ny, DIM);
                            txt(g, "Tippe auf GUI-Farben, um eine Farbe zu waehlen.", cx, ny + 12, DIM);
                            txt(g, "Rechtsklick auf Player ESP / Block ESP: Einstellungen.", cx, ny + 24, DIM);
                        } }
            case 2 -> { int off = guardPickerOpen ? 34 : 0;
                        txt(g, "K zweimal mit dem Item druecken, um es zu markieren oder zu loesen.", cx, ny + off, DIM);
                        txt(g, "Kein Item markiert", cx, ny + off + 12, DIM); }
            case 4 -> { Gui.round(g, cx, ry, cx + cw, ry + 26, 8, ROW);
                        txt(g, "GUI-Groesse", cx + 10, ry + 9, 0xFFF0F2FF);
                        txt(g, c.guiScale + "%", x + w - 108, ry + 6, 0xFFFFFFFF);
                        txt(g, "Position", cx, ry + 44, 0xFFFFFFFF);
                        txt(g, "Pfeile nutzen oder das Fenster am oberen Rand ziehen.", cx, ry + 56, DIM);
                        txt(g, "Groesse: " + c.guiScale + "% - X " + c.guiX + " Y " + c.guiY, cx, ry + 130, DIM); }
            case 5 -> { txt(g, "Mehrere Adressen mit Komma trennen, der Port ist egal.", cx, ry + 28, DIM);
                        txt(g, "Leer = nur Einzelspieler.", cx, ry + 40, DIM); }
        }
        txt(g, "ESC Schließen", x + 12, y + h - 20, DIM);
        super.render(g, mx, my, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
