package de.hugosmp;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

public class Config {
    public static final String[] COLORS = {"Violett", "Cyan", "Gold", "Gruen", "Rot", "Pink", "Orange", "Blau", "Weiss", "Mix"};
    public static final int[] COLOR_RGB = {0xA99CFF, 0x4FD1E8, 0xE8B85C, 0x5CE8A0, 0xFF5C5C, 0xFF7ACD, 0xFF9F43, 0x5C9CFF, 0xF2F2F2, 0xC98CE8};
    public static final String[] GROUPS = {"Mace","End Crystal","Lever","Cobweb","Breeze Rod","Elytra",
            "Wind Charge","Sword","Axe","Pickaxe","Shield","Obsidian",
            "Star Pickaxe","Star Axe","Star Schaufel","Star Eimer"};

    public int invSeeKey = -1;      // -1 = noch nicht festgelegt
    public boolean autoInvSeeRtp = false;
    public boolean playerEsp = false;
    public boolean blockEsp = false;
    public int playerAlpha = 40;     // Deckkraft der Spieler-Farbe in Prozent (5-100)
    public boolean playerTracer = false;
    public int playerRange = 64;     // Reichweite fuer Player ESP in Bloecken (16/32/64/128)
    public int blockAlpha = 40;      // Deckkraft der Block-Farbe in Prozent (5-100)
    public boolean tracer = false;   // blaue Linie vom Fadenkreuz zu den ESP-Bloecken
    public int blockEspRange = 16;   // Reichweite in Bloecken (8/16/32/64)
    public String espServers = "";   // erlaubte Server fuer Block ESP, Komma-getrennt (leer = nur Einzelspieler)
    public java.util.List<String> blockEspBlocks = new java.util.ArrayList<>();
    public boolean ahSellMacro = false;
    public int ahKey = -1;            // -1 = noch nicht festgelegt
    public String ahText = "";        // Text/Befehl, der bei Tastendruck gesendet wird
    public int guiColor = 0;
    public boolean durabilityGuard = false;
    public int guardThreshold = 20;
    public Map<String, Boolean> itemProtect = new LinkedHashMap<>();
    public int guiScale = 65, guiX = 0, guiY = 0;

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hugosmp.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Config I = new Config();

    public int accent() { return 0xFF000000 | COLOR_RGB[guiColor]; }

    public static void load() {
        try { if (Files.exists(FILE)) I = GSON.fromJson(Files.readString(FILE), Config.class); } catch (Exception ignored) {}
        if (I == null) I = new Config();
        if (I.espServers == null) I.espServers = "";
        if (I.blockEspBlocks == null) I.blockEspBlocks = new java.util.ArrayList<>();
        if (I.itemProtect == null) I.itemProtect = new LinkedHashMap<>();
        for (String g : GROUPS) I.itemProtect.putIfAbsent(g, false);
    }
    public static void save() { try { Files.writeString(FILE, GSON.toJson(I)); } catch (Exception ignored) {} }
}
