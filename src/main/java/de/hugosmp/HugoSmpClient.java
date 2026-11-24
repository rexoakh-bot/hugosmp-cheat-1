package de.hugosmp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HugoSmpClient implements ClientModInitializer {
    public static KeyBinding openKey;
    private static boolean invDown = false, ahDown = false;
    private static String queueOpponent = null;
    private static long queueTime = 0;
    private static final long OPPONENT_MAX_MS = 10 * 60 * 1000L;
    private static final Pattern RTP_ACCEPT = Pattern.compile(
            "(?i)([A-Za-z0-9_]{3,16})\\s+hat\\s+die\\s+RTP[- ]?Anfrage\\s+angenommen");

    public static int openKeyCode() { return InputUtil.fromTranslationKey(openKey.getBoundKeyTranslationKey()).getCode(); }

    @Override
    public void onInitializeClient() {
        Config.load();
        BlockEspRenderer.init();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "cavevfx Mod oeffnen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> { if (!overlay) onServerMessage(msg.getString()); });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) mc.setScreen(new SettingsScreen());
            invSeeTick(mc);
            ahTick(mc);
        });
    }

    /** AH Sell Macro: pro Tastendruck genau EINE gespeicherte Nachricht/ein Befehl (keine Wiederholung). */
    private static void ahTick(MinecraftClient mc) {
        Config c = Config.I;
        if (mc.player == null || mc.currentScreen != null || !c.ahSellMacro || c.ahKey < 0 || c.ahText.isBlank()) {
            ahDown = false;
            return;
        }
        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), c.ahKey) == GLFW.GLFW_PRESS;
        if (down && !ahDown) {
            String t = c.ahText.trim();
            if (t.startsWith("/")) mc.player.networkHandler.sendChatCommand(t.substring(1));
            else mc.player.networkHandler.sendChatMessage(t);
        }
        ahDown = down;
    }

    /** Erkennt: "<Name> hat die RTP Anfrage angenommen" -> Queue-Gegner merken. */
    private static void onServerMessage(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Matcher m = RTP_ACCEPT.matcher(text);
        if (!m.find() || mc.player == null) return;
        String name = m.group(1);
        if (name.equalsIgnoreCase(mc.player.getName().getString())) return;
        queueOpponent = name;
        queueTime = System.currentTimeMillis();
        if (Config.I.autoInvSeeRtp && Config.I.invSeeKey >= 0)
            mc.player.sendMessage(Text.literal("Gegner erkannt: " + name), true);
    }

    /** InvSee: Taste druecken -> /invsee <Spieler> (anvisierter Spieler, sonst naechster). */
    private static void invSeeTick(MinecraftClient mc) {
        Config c = Config.I;
        if (mc.player == null || mc.world == null || mc.currentScreen != null || c.invSeeKey < 0 || !c.autoInvSeeRtp) {
            invDown = false;
            return;
        }
        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), c.invSeeKey) == GLFW.GLFW_PRESS;
        if (down && !invDown) runInvSee(mc);
        invDown = down;
    }

    private static void runInvSee(MinecraftClient mc) {
        if (queueOpponent != null && System.currentTimeMillis() - queueTime < OPPONENT_MAX_MS) {
            mc.player.networkHandler.sendChatCommand("invsee " + queueOpponent);
            mc.player.sendMessage(Text.literal("InvSee: " + queueOpponent), true);
            return;
        }
        PlayerEntity target = null;
        if (mc.targetedEntity instanceof PlayerEntity p && p != mc.player) {
            target = p;
        } else {
            double best = Double.MAX_VALUE;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                double d = mc.player.squaredDistanceTo(p);
                if (d < best) { best = d; target = p; }
            }
        }
        if (target == null) {
            mc.player.sendMessage(Text.literal("InvSee: Kein Spieler in der Naehe"), true);
            return;
        }
        String name = target.getName().getString();
        mc.player.networkHandler.sendChatCommand("invsee " + name);
        mc.player.sendMessage(Text.literal("InvSee: " + name), true);
    }
}
