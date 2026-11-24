package de.hugosmp;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Block ESP: markiert gewaehlte Bloecke mit durchsichtiger Farbe (Alpha), auch durch Waende.
 * Aktiv in Einzelspieler-Welten und auf Servern, die du selbst bei "Erlaubte Server" eintraegst
 * (z. B. dein privater Testserver). Auf allen anderen Servern passiert nichts.
 */
public class BlockEspRenderer {
    private static final int MAX_FOUND = 3000;
    private static final int BLUE = 0x4C9CFF;
    private static final int RED = 0xFF5C5C;
    private static final double HEAD = 0.5;   // Player ESP ohne Kopf: oberste 0,5 Bloecke bleiben frei
    private static final List<BlockPos> found = new ArrayList<>();
    private static int tick = 0;

    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("hugosmp", "pipeline/block_esp"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();
    private static final RenderLayer LAYER = RenderLayer.of("hugosmp_block_esp", 1536, false, true, PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().build());

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(BlockEspRenderer::scan);
        WorldRenderEvents.END_MAIN.register(BlockEspRenderer::render);
    }

    /** ESP laeuft nur in Einzelspieler-Welten und auf Servern aus "Erlaubte Server". */
    private static boolean allowed(MinecraftClient mc) {
        return mc.world != null && mc.player != null && (mc.isInSingleplayer() || serverAllowed(mc));
    }

    private static boolean active(MinecraftClient mc) {
        return allowed(mc) && Config.I.blockEsp && !Config.I.blockEspBlocks.isEmpty();
    }

    private static String host(String a) {
        a = a.trim().toLowerCase();
        int i = a.lastIndexOf(':');
        return i > 0 ? a.substring(0, i) : a;
    }

    private static boolean serverAllowed(MinecraftClient mc) {
        var info = mc.getCurrentServerEntry();
        if (info == null || info.address == null || Config.I.espServers.isBlank()) return false;
        String cur = host(info.address);
        for (String e : Config.I.espServers.split("[,;\\s]+")) {
            if (!e.isBlank() && host(e).equals(cur)) return true;
        }
        return false;
    }

    private static void scan(MinecraftClient mc) {
        if (!active(mc)) { found.clear(); return; }
        int range = Math.max(4, Math.min(64, Config.I.blockEspRange));
        int every = range <= 16 ? 10 : range <= 32 ? 20 : 40;   // grosse Reichweite: seltener suchen
        if (++tick % every != 0) return;
        Set<String> wanted = new HashSet<>(Config.I.blockEspBlocks);
        List<BlockPos> out = new ArrayList<>();
        BlockPos c = mc.player.getBlockPos();
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int x = -range; x <= range; x++)
            for (int y = -range; y <= range; y++)
                for (int z = -range; z <= range; z++) {
                    if (out.size() >= MAX_FOUND) break;
                    p.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    var block = mc.world.getBlockState(p).getBlock();
                    if (wanted.contains(Registries.BLOCK.getId(block).toString())) out.add(p.toImmutable());
                }
        out.sort(java.util.Comparator.comparingDouble(b -> b.getSquaredDistance(c)));
        found.clear();
        found.addAll(out);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!allowed(mc)) return;
        boolean blocks = Config.I.blockEsp && !Config.I.blockEspBlocks.isEmpty() && !found.isEmpty();
        boolean players = Config.I.playerEsp;
        if (!blocks && !players) return;
        Vec3d cam = ctx.camera().getPos();
        Matrix4f m = ctx.matrices().peek().getPositionMatrix();
        VertexConsumer vc = ctx.consumers().getBuffer(LAYER);
        int alpha = Math.max(5, Math.min(100, Config.I.blockAlpha)) * 255 / 100;
        if (blocks) {
            int argb = (alpha << 24) | BLUE;
            for (BlockPos bp : found) {
                float x1 = (float) (bp.getX() - cam.x) - 0.01f, y1 = (float) (bp.getY() - cam.y) - 0.01f, z1 = (float) (bp.getZ() - cam.z) - 0.01f;
                box(vc, m, argb, x1, y1, z1, x1 + 1.02f, y1 + 1.02f, z1 + 1.02f);
            }
            if (Config.I.tracer) drawTracers(ctx, vc, m, cam);
        }
        if (players) {
            int pc = (Math.max(5, Math.min(100, Config.I.playerAlpha)) * 255 / 100 << 24) | RED;
            double r2 = (double) Config.I.playerRange * Config.I.playerRange;
            for (var p : mc.world.getPlayers()) {
                if (p == mc.player || mc.player.squaredDistanceTo(p) > r2) continue;
                var bb = p.getBoundingBox();
                box(vc, m, pc, (float) (bb.minX - cam.x), (float) (bb.minY - cam.y), (float) (bb.minZ - cam.z),
                        (float) (bb.maxX - cam.x), (float) (bb.maxY - HEAD - cam.y), (float) (bb.maxZ - cam.z));
            }
            if (Config.I.playerTracer) drawPlayerTracers(ctx, vc, m, cam, mc);
        }
    }

    /** Gefuellter, durchsichtiger Quader (6 Flaechen). */
    private static void box(VertexConsumer vc, Matrix4f m, int argb, float x1, float y1, float z1, float x2, float y2, float z2) {
        quad(vc, m, argb, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2); // unten
        quad(vc, m, argb, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1); // oben
        quad(vc, m, argb, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1); // Nord
        quad(vc, m, argb, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2); // Sued
        quad(vc, m, argb, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1); // West
        quad(vc, m, argb, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2); // Ost
    }

    /** Blaue Linie (duennes Band) vom Fadenkreuz zur Mitte jedes gescannten ESP-Blocks (die 100 naechsten), auch durch Waende. */
    private static void drawTracers(WorldRenderContext ctx, VertexConsumer vc, Matrix4f m, Vec3d cam) {
        int tc = (0xDD << 24) | BLUE;
        int n = 0;
        for (BlockPos bp : found) {
            if (n++ >= 100) break;
            band(ctx, vc, m, tc, (float) (bp.getX() + 0.5 - cam.x), (float) (bp.getY() + 0.5 - cam.y), (float) (bp.getZ() + 0.5 - cam.z));
        }
    }

    /** Rote Linie vom Fadenkreuz zur Mitte jedes anderen Spielers. */
    private static void drawPlayerTracers(WorldRenderContext ctx, VertexConsumer vc, Matrix4f m, Vec3d cam, MinecraftClient mc) {
        int tc = (0xDD << 24) | RED;
        double r2 = (double) Config.I.playerRange * Config.I.playerRange;
        for (var p : mc.world.getPlayers()) {
            if (p == mc.player || mc.player.squaredDistanceTo(p) > r2) continue;
            var bb = p.getBoundingBox();
            band(ctx, vc, m, tc, (float) ((bb.minX + bb.maxX) / 2 - cam.x), (float) ((bb.minY + bb.maxY - HEAD) / 2 - cam.y), (float) ((bb.minZ + bb.maxZ) / 2 - cam.z));
        }
    }

    /** Duenne Linie (Band) vom Fadenkreuz zu (ex, ey, ez), alles relativ zur Kamera. */
    private static void band(WorldRenderContext ctx, VertexConsumer vc, Matrix4f m, int tc, float ex, float ey, float ez) {
        double yaw = Math.toRadians(ctx.camera().getYaw()), pitch = Math.toRadians(ctx.camera().getPitch());
        float lx = (float) (-Math.sin(yaw) * Math.cos(pitch)), ly = (float) -Math.sin(pitch), lz = (float) (Math.cos(yaw) * Math.cos(pitch));
        float sx = lx * 0.4f, sy = ly * 0.4f, sz = lz * 0.4f;
        float dx = ex - sx, dy = ey - sy, dz = ez - sz;
        float qx = dy * lz - dz * ly, qy = dz * lx - dx * lz, qz = dx * ly - dy * lx;
        float len = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
        if (len < 1e-4f) return;
        qx /= len; qy /= len; qz /= len;
        float w1 = 0.0003f, w2 = 0.003f;
        quad(vc, m, tc, sx - qx * w1, sy - qy * w1, sz - qz * w1, sx + qx * w1, sy + qy * w1, sz + qz * w1,
                ex + qx * w2, ey + qy * w2, ez + qz * w2, ex - qx * w2, ey - qy * w2, ez - qz * w2);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int c,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vc.vertex(m, ax, ay, az).color(c);
        vc.vertex(m, bx, by, bz).color(c);
        vc.vertex(m, cx, cy, cz).color(c);
        vc.vertex(m, dx, dy, dz).color(c);
    }
}
