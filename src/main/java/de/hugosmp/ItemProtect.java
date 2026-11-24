package de.hugosmp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;

public class ItemProtect {
    private static boolean on(String group) {
        Boolean b = Config.I.itemProtect.get(group);
        return b != null && b;
    }

    /** true, wenn das Item zu einer aktiven Schutz-Gruppe gehoert. */
    public static boolean isProtected(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        if (on("Mace") && s.isOf(Items.MACE)) return true;
        if (on("End Crystal") && s.isOf(Items.END_CRYSTAL)) return true;
        if (on("Lever") && s.isOf(Items.LEVER)) return true;
        if (on("Cobweb") && s.isOf(Items.COBWEB)) return true;
        if (on("Breeze Rod") && s.isOf(Items.BREEZE_ROD)) return true;
        if (on("Elytra") && s.isOf(Items.ELYTRA)) return true;
        if (on("Wind Charge") && s.isOf(Items.WIND_CHARGE)) return true;
        if (on("Sword") && s.isIn(ItemTags.SWORDS)) return true;
        if (on("Axe") && s.isIn(ItemTags.AXES)) return true;
        if (on("Pickaxe") && s.isIn(ItemTags.PICKAXES)) return true;
        if (on("Shield") && s.isOf(Items.SHIELD)) return true;
        if (on("Obsidian") && s.isOf(Items.OBSIDIAN)) return true;
        if (isStar(s)) {
            if (on("Star Pickaxe") && s.isIn(ItemTags.PICKAXES)) return true;
            if (on("Star Axe") && s.isIn(ItemTags.AXES)) return true;
            if (on("Star Schaufel") && s.isIn(ItemTags.SHOVELS)) return true;
            if (on("Star Eimer") && Registries.ITEM.getId(s.getItem()).getPath().endsWith("bucket")) return true;
        }
        return false;
    }

    /** Star-Items erkennt die Mod am Namen (enthaelt "Star"). */
    private static boolean isStar(ItemStack s) {
        return s.getName().getString().toLowerCase().contains("star");
    }

    public static void notifyBlocked() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal("Item geschuetzt"), true);
    }

    /** Kleines rotes Schloss; (x, y) = obere linke Ecke des Slots (16x16). */
    public static void drawLock(DrawContext g, int x, int y) {
        int lx = x + 9, ly = y - 1;
        int dark = 0xFF6E1212, red = 0xFFE03232, light = 0xFFFF6B6B;
        g.fill(lx + 1, ly, lx + 6, ly + 1, light);        // Buegel oben
        g.fill(lx + 1, ly + 1, lx + 2, ly + 4, light);    // Buegel links
        g.fill(lx + 5, ly + 1, lx + 6, ly + 4, light);    // Buegel rechts
        g.fill(lx, ly + 3, lx + 7, ly + 9, dark);         // Koerper Rand
        g.fill(lx + 1, ly + 4, lx + 6, ly + 8, red);      // Koerper
        g.fill(lx + 3, ly + 5, lx + 4, ly + 7, 0xFFFFFFFF); // Schluesselloch
    }
}
