package dev.hugoservice;

import dev.hugoservice.Categories.Group;
import dev.hugoservice.KnifeMesh.Blade;
import dev.hugoservice.KnifeMesh.Path;
import dev.hugoservice.Styles.Style;

import net.minecraft.util.Identifier;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Knife skins: smooth 3D knives drawn by the mod itself ({@link KnifeRenderer}) in place of every normal sword, plus
 * generated finishes. No resource pack is involved, so server packs cannot cover them. All designs are original work
 * of this mod.
 *
 * <p>Models stand upright in model space (grip at the bottom, tip at the top, 0..16 like a block model); the display
 * transforms in assets/hugoservice/models/item/knife.json turn them so they lie in the hand and in the GUI like a sword.
 * Finish texture (TEX square): blade left half, handle u 0.5-0.69, metal u 0.75-1 top half, polished edge u 0.75-1 bottom half.
 */
public final class Knives {
    public static final String MODEL_GROUP = "knife", FINISH_GROUP = "knife_finish";
    private static final Set<String> SWORDS = Set.of("wooden_sword", "stone_sword", "copper_sword", "iron_sword",
        "golden_sword", "diamond_sword", "netherite_sword");

    public static boolean isKnifeGroup(Group g) {
        return g.id().equals(MODEL_GROUP) || g.id().equals(FINISH_GROUP);
    }

    // ================================================================ models

    /** How the knife moves when inspected or drawn. */
    public enum Anim { SPIN, FLIP, TURN, FLICK, HEAVY, TWIRL, PUSH }

    /** A knife; the butterfly keeps its two handles apart ({@code safe}, {@code bite}) so tricks can swing them. */
    public record Model(String id, String name, Anim anim, KnifeMesh mesh, KnifeMesh safe, KnifeMesh bite) {
        Model(String id, String name, Anim anim, KnifeMesh mesh) { this(id, name, anim, mesh, null, null); }
    }

    /** Pivot of each butterfly handle in model units: they swing around these, in the knife's plane. */
    public static final float[] BF_PIVOT_SAFE = {7.2f, 7.3f}, BF_PIVOT_BITE = {8.8f, 7.3f};

    private static final float[] H = KnifeMesh.HANDLE, M = KnifeMesh.METAL; // before MODELS, which uses them

    public static final List<Model> MODELS = List.of(karambit(), talon(), butterfly(), bayonet(), m9(), flip(),
        stiletto(), bowie(), huntsman(), gut(), push());

    public static Model model(String id) {
        for (Model m : MODELS) if (m.id.equals(id)) return m;
        return null;
    }

    /** The knife the player chose in the menu, or null when swords stay swords. */
    public static Model active() {
        String id = Config.INSTANCE.styleOf(MODEL_GROUP);
        return id.startsWith("knife:") ? model(id.substring(6)) : null;
    }

    /** The knife's item model (assets/hugoservice/items/knife.json, drawn by KnifeRenderer). */
    public static final Identifier KNIFE_ID = Identifier.of("hugoservice", "knife");
    private static final Identifier NO_MODEL = Identifier.of("hugoservice", "no_such_model");

    /**
     * True when this stack is one of the seven normal swords and drawn as the chosen knife. A fixed list instead of the
     * "swords" item tag, because servers can add other items to that tag; custom server items keep their look.
     */
    public static boolean replaces(net.minecraft.item.ItemStack stack) {
        if (active() == null) return false;
        Identifier id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
        return id.getNamespace().equals("minecraft") && SWORDS.contains(id.getPath())
            && id.equals(stack.get(net.minecraft.component.DataComponentTypes.ITEM_MODEL));
    }

    /** Item model to draw for a stack: the chosen knife for every normal sword, also over server resource packs. */
    public static Identifier swap(net.minecraft.item.ItemStack stack, Identifier id) {
        if (id == null || !replaces(stack)) return id;
        var models = net.minecraft.client.MinecraftClient.getInstance().getBakedModelManager();
        return models.getItemModel(KNIFE_ID) != models.getItemModel(NO_MODEL) ? KNIFE_ID : id;
    }

    private static Path line(float x0, float y0, float x1, float y1) { return KnifeMesh.line(x0, y0, x1, y1); }

    /** Straight grip along the knife, a little fuller in the middle. */
    private static void grip(KnifeMesh m, float y0, float y1, float width, float depth) {
        m.rod(line(8, y0, 8, y1), t -> width * (1 + 0.07f * (float) Math.sin(Math.PI * t)), depth, 3.2f, 0.18f, H);
    }

    /** Guard, pommel or ring band: a rounded metal bar across the knife at height y. */
    private static void bar(KnifeMesh m, float y, float width, float height, float depth) {
        m.rod(line(8 - width / 2, y, 8 + width / 2, y), height, depth, 3, height / 2, M);
    }

    /** Saw teeth along the spine at x, from y0 to y1. */
    private static void teeth(KnifeMesh m, float x, float y0, float y1, float size, float depth) {
        for (float y = y0; y + size <= y1; y += size * 1.25f)
            m.prism(new float[] {x - 0.1f, y, x + size * 0.7f, y + size * 0.35f, x - 0.1f, y + size}, depth, KnifeMesh.BLADE, m.bladeBounds);
    }

    private static Model karambit() {
        KnifeMesh m = new KnifeMesh();
        m.ring(Path.circle(8.0f, 1.55f, 1.12f), 0.5f, 0.66f, M);
        m.rod(new Path(8.05f, 2.45f).quad(8.25f, 5.0f, 9.45f, 6.75f), t -> 1.5f + 0.2f * t, 1.36f, 3, 0.25f, H);
        m.rod(line(9.3f, 6.55f, 9.62f, 7.02f), 1.85f, 1.5f, 3, 0.14f, M);
        m.blade(new Path(10.09f, 6.44f).cubic(11.1f, 9.2f, 9.4f, 14.6f, 5.8f, 12.6f),
            new Path(8.91f, 7.26f).cubic(9.8f, 9.2f, 8.6f, 12.4f, 5.8f, 12.6f), Blade.of(0.55f, 0.4f));
        return new Model("karambit", "Karambit", Anim.SPIN, m);
    }

    /** Bigger claw than the karambit with a longer, wider curve. */
    private static Model talon() {
        KnifeMesh m = new KnifeMesh();
        m.ring(Path.circle(8.0f, 1.5f, 1.2f), 0.55f, 0.7f, M);
        m.rod(new Path(8.05f, 2.5f).quad(8.2f, 4.8f, 9.4f, 6.55f), t -> 1.6f + 0.2f * t, 1.42f, 3, 0.25f, H);
        m.rod(line(9.25f, 6.35f, 9.6f, 6.85f), 1.95f, 1.56f, 3, 0.15f, M);
        m.blade(new Path(10.15f, 6.2f).cubic(11.6f, 9.4f, 10.2f, 15.6f, 4.9f, 12.2f),
            new Path(8.75f, 7.05f).cubic(9.9f, 9.3f, 8.8f, 12.6f, 4.9f, 12.2f), Blade.of(0.6f, 0.42f).fuller(0.1f, 0.28f, 0.05f, 0.55f, 0.1f));
        return new Model("talon", "Talon", Anim.SPIN, m);
    }

    private static Model butterfly() {
        KnifeMesh blade = new KnifeMesh(), safe = new KnifeMesh(), bite = new KnifeMesh();
        blade.rod(line(6.85f, 7.3f, 9.15f, 7.3f), 0.8f, 0.78f, 3, 0.4f, M);   // tang
        for (float[] p : new float[][] {BF_PIVOT_SAFE, BF_PIVOT_BITE}) blade.pin(p[0], p[1], 0.42f, 1.62f, M);
        blade.blade(new Path(8.7f, 7.55f).to(8.7f, 11.5f).quad(8.7f, 12.7f, 8.0f, 13.4f),
            new Path(7.3f, 7.55f).to(7.3f, 11.6f).quad(7.28f, 12.9f, 8.0f, 13.4f), Blade.of(0.52f, 0.42f).fuller(0.14f, 0.34f, 0.05f, 0.6f, 0.11f));
        safe.rod(line(7.2f, 0.55f, 7.2f, 7.65f), 0.98f, 1.36f, 3.4f, 0.32f, H);
        safe.rod(line(7.2f, 0.25f, 7.2f, 0.9f), 1.06f, 1.44f, 3, 0.3f, M);
        bite.rod(line(8.8f, 0.55f, 8.8f, 7.65f), 0.98f, 1.36f, 3.4f, 0.32f, H);
        bite.rod(line(8.8f, 0.05f, 8.8f, 0.9f), 1.08f, 1.46f, 3, 0.3f, M);    // latch
        return new Model("butterfly", "Butterfly", Anim.FLIP, blade, safe, bite);
    }

    private static Model bayonet() {
        KnifeMesh m = new KnifeMesh();
        bar(m, 0.7f, 1.85f, 0.95f, 1.7f);
        grip(m, 1.0f, 5.8f, 1.42f, 1.32f);
        for (float y : new float[] {2.45f, 4.05f}) bar(m, y, 1.6f, 0.3f, 1.46f);
        bar(m, 5.95f, 3.8f, 0.72f, 1.36f);
        m.blade(new Path(8.8f, 6.1f).to(8.8f, 11.4f).quad(8.8f, 12.9f, 8.05f, 13.9f),
            new Path(7.2f, 6.1f).to(7.2f, 11.0f).quad(7.15f, 13.1f, 8.05f, 13.9f), Blade.of(0.56f, 0.42f).fuller(0.12f, 0.34f, 0.04f, 0.62f, 0.13f));
        return new Model("bayonet", "Bayonet", Anim.TURN, m);
    }

    /** Military bayonet with grip ribs, a ringed guard and a saw-toothed spine. */
    private static Model m9() {
        KnifeMesh m = new KnifeMesh();
        bar(m, 0.68f, 1.95f, 0.95f, 1.8f);
        grip(m, 1.0f, 5.7f, 1.5f, 1.42f);
        for (float y = 1.75f; y < 5.2f; y += 0.8f) bar(m, y, 1.66f, 0.22f, 1.56f);
        bar(m, 5.95f, 3.5f, 0.7f, 1.42f);
        m.ring(Path.circle(10.15f, 5.95f, 0.45f), 0.26f, 0.62f, M);
        m.blade(new Path(8.95f, 6.1f).to(8.95f, 11.6f).quad(8.95f, 13.0f, 8.0f, 13.95f),
            new Path(7.0f, 6.1f).to(7.0f, 11.2f).quad(6.95f, 13.2f, 8.0f, 13.95f), Blade.of(0.6f, 0.4f).fuller(0.1f, 0.28f, 0.04f, 0.58f, 0.13f));
        teeth(m, 8.95f, 7.0f, 10.6f, 0.48f, 0.5f);
        return new Model("m9", "M9 Bayonet", Anim.TURN, m);
    }

    private static Model flip() {
        KnifeMesh m = new KnifeMesh();
        m.rod(line(8, 0.25f, 8, 1.1f), 1.62f, 1.5f, 3, 0.42f, M);
        grip(m, 0.8f, 6.15f, 1.48f, 1.38f);
        m.rod(line(8, 5.95f, 8, 6.6f), 1.66f, 1.52f, 3, 0.22f, M);
        m.rod(line(8.8f, 6.5f, 9.55f, 6.8f), 0.42f, 0.46f, 2.6f, 0.21f, M); // flipper
        m.blade(new Path(8.85f, 6.4f).to(8.85f, 9.7f).cubic(8.85f, 10.7f, 8.65f, 11.3f, 8.15f, 11.8f),
            new Path(7.15f, 6.4f).to(7.15f, 9.3f).quad(7.15f, 11.3f, 8.15f, 11.8f), Blade.of(0.55f, 0.4f).fuller(0.14f, 0.32f, 0.05f, 0.55f, 0.11f));
        return new Model("flip", "Flip Knife", Anim.FLICK, m);
    }

    /** Long, slim blade with a thin cross guard. */
    private static Model stiletto() {
        KnifeMesh m = new KnifeMesh();
        bar(m, 0.55f, 1.5f, 0.75f, 1.45f);
        grip(m, 0.8f, 5.5f, 1.22f, 1.14f);
        m.rod(line(8, 5.35f, 8, 5.95f), 1.42f, 1.32f, 3, 0.2f, M);
        bar(m, 6.1f, 3.4f, 0.42f, 0.9f);
        m.blade(new Path(8.55f, 6.2f).to(8.55f, 12.3f).quad(8.55f, 13.4f, 8.05f, 14.1f),
            new Path(7.45f, 6.2f).to(7.45f, 12.3f).quad(7.45f, 13.4f, 8.05f, 14.1f), Blade.of(0.46f, 0.48f).fuller(0.16f, 0.4f, 0.03f, 0.72f, 0.1f));
        return new Model("stiletto", "Stiletto", Anim.FLICK, m);
    }

    /** Big clip point with a wide guard and sawback. */
    private static Model bowie() {
        KnifeMesh m = new KnifeMesh();
        bar(m, 0.68f, 2.0f, 0.95f, 1.85f);
        grip(m, 1.0f, 6.05f, 1.52f, 1.46f);
        bar(m, 6.35f, 4.2f, 0.76f, 1.44f);
        m.blade(new Path(9.2f, 6.5f).to(9.2f, 10.7f).quad(8.45f, 11.6f, 7.9f, 13.6f),
            new Path(7.0f, 6.5f).to(7.0f, 11.0f).quad(6.95f, 13.0f, 7.9f, 13.6f), Blade.of(0.62f, 0.4f).fuller(0.1f, 0.26f, 0.05f, 0.5f, 0.12f));
        teeth(m, 9.2f, 7.3f, 10.3f, 0.5f, 0.52f);
        return new Model("bowie", "Bowie", Anim.HEAVY, m);
    }

    /** Big survival knife: finger guard, sawback and a wide clipped blade. */
    private static Model huntsman() {
        KnifeMesh m = new KnifeMesh();
        bar(m, 0.62f, 1.95f, 0.9f, 1.8f);
        grip(m, 0.9f, 5.9f, 1.52f, 1.46f);
        bar(m, 6.15f, 3.8f, 0.7f, 1.48f);
        m.rod(new Path(6.45f, 6.0f).quad(5.9f, 5.5f, 6.4f, 4.75f), 0.52f, 1.36f, 3, 0.26f, M); // finger guard
        m.blade(new Path(9.35f, 6.3f).to(9.35f, 11.0f).quad(9.3f, 12.6f, 8.2f, 13.5f),
            new Path(7.0f, 6.3f).to(7.0f, 11.3f).quad(6.98f, 13.0f, 8.2f, 13.5f), Blade.of(0.62f, 0.4f).fuller(0.1f, 0.27f, 0.05f, 0.55f, 0.12f));
        teeth(m, 9.35f, 7.1f, 10.4f, 0.46f, 0.52f);
        return new Model("huntsman", "Huntsman", Anim.HEAVY, m);
    }

    /** Drop point with a gut hook cut into the spine. */
    private static Model gut() {
        KnifeMesh m = new KnifeMesh();
        bar(m, 0.6f, 1.65f, 0.8f, 1.55f);
        grip(m, 0.85f, 6.15f, 1.46f, 1.38f);
        bar(m, 6.45f, 2.2f, 0.62f, 1.5f);
        m.blade(new Path(8.9f, 6.6f).to(8.9f, 9.7f).quad(8.48f, 9.75f, 8.5f, 10.2f).quad(8.55f, 10.55f, 9.15f, 10.15f)
                .quad(9.3f, 11.5f, 8.35f, 12.35f),
            new Path(7.2f, 6.6f).to(7.2f, 10.5f).quad(7.2f, 12.0f, 8.35f, 12.35f), Blade.of(0.56f, 0.42f));
        return new Model("gut", "Gut Knife", Anim.TWIRL, m);
    }

    /** Push dagger: a T-grip in the fist and a short two-edged blade. */
    private static Model push() {
        KnifeMesh m = new KnifeMesh();
        m.rod(line(5.5f, 2.9f, 10.5f, 2.9f), 1.35f, 1.55f, 3, 0.55f, H);
        for (float x : new float[] {5.75f, 10.25f}) m.rod(line(x - 0.28f, 2.9f, x + 0.28f, 2.9f), 1.48f, 1.66f, 3, 0.2f, M);
        m.rod(line(8, 3.3f, 8, 4.6f), 1.15f, 1.2f, 3, 0.3f, M);
        m.blade(new Path(9.0f, 4.4f).quad(9.1f, 7.2f, 8.0f, 9.6f), new Path(7.0f, 4.4f).quad(6.9f, 7.2f, 8.0f, 9.6f), Blade.dagger(0.62f));
        return new Model("push", "Push Dagger", Anim.PUSH, m);
    }

    // ================================================================ finishes

    /** Blade colour at (u, v) in 0..1 (v = 0 at the tip); {@code phase} 0..1 runs through the frames of an animated finish. */
    private interface Painter { int color(int part, float u, float v, Random r, float phase); }

    /** {@code frames} above 1 makes an animated finish (KnifeRenderer repaints it as it runs). */
    public record Finish(String id, String name, int handle, int metal, int edge, int frames, Painter blade) {
        Finish(String id, String name, int handle, int metal, int edge, Painter blade) { this(id, name, handle, metal, edge, 1, blade); }
    }

    private static final int P_BLADE = 0;

    public static final List<Finish> FINISHES = List.of(
        new Finish("steel", "Steel", 0xFF1E1F24, 0xFF9AA2AA, 0xFFEAF2FA, (p, u, v, r, ph) -> brushed(0xFFB8C0C8, 0xFF858D96, v, r)),
        new Finish("sunset", "Sunset Fade", 0xFF15151A, 0xFFB7BDC4, 0xFFFFE6F2, (p, u, v, r, ph) -> {
            float t = (u * 0.6f + (1 - v) * 0.9f) / 1.5f;
            return t < 0.5f ? mix(0xFFFFD24A, 0xFFFF4FA0, t * 2) : mix(0xFFFF4FA0, 0xFF6A3CFF, (t - 0.5f) * 2);
        }),
        new Finish("nebula", "Nebula", 0xFF101014, 0xFF8C8FA0, 0xFFF0E0FF, (p, u, v, r, ph) -> {
            double s = Math.sin(u * 9 + Math.sin(v * 7) * 2) * Math.cos(v * 11 - u * 3);
            int c = s > 0.35 ? mix(0xFF2A1450, 0xFFD24BFF, (float) s) : s < -0.4 ? mix(0xFF1A1440, 0xFF3A7BFF, (float) -s) : 0xFF1A0B2E;
            return r.nextInt(60) == 0 ? 0xFFFFFFFF : c;
        }),
        new Finish("ruby", "Ruby", 0xFF120A0C, 0xFFC0A0A6, 0xFFFFD0DA, (p, u, v, r, ph) -> gem(0xFF5E0614, 0xFFE0244A, u, v)),
        new Finish("sapphire", "Sapphire", 0xFF0A0C14, 0xFFA0AAC8, 0xFFD6E6FF, (p, u, v, r, ph) -> gem(0xFF071A5E, 0xFF2A7BFF, u, v)),
        new Finish("emerald", "Emerald", 0xFF0A120C, 0xFFA0C0AA, 0xFFD6FFE6, (p, u, v, r, ph) -> gem(0xFF04401E, 0xFF1FD27A, u, v)),
        new Finish("tiger", "Tiger Stripe", 0xFF1A120A, 0xFFB09A80, 0xFFFFF0D6, (p, u, v, r, ph) -> {
            double w = Math.sin((u * 2 + v * 14) + Math.sin(v * 9) * 1.2);
            return w > 0.55 ? 0xFF1A0E06 : mix(0xFFF08A1C, 0xFFFFB347, (float) (w + 1) / 2);
        }),
        new Finish("blued", "Blued Steel", 0xFF1A1A1E, 0xFFB09A50, 0xFFF5F0E0, (p, u, v, r, ph) -> {
            double s = Math.sin(u * 6.3 + v * 4.1) + Math.sin(u * 2.1 - v * 8.7) * 0.8;
            return s > 0.9 ? 0xFFC9A43A : s > 0.1 ? 0xFF2C4BB0 : s > -0.7 ? 0xFF6A4CA0 : 0xFF8E9AA6;
        }),
        new Finish("crimson_net", "Crimson Net", 0xFF140808, 0xFF9A8A8A, 0xFFFFD6D6, (p, u, v, r, ph) -> {
            double a = Math.atan2(v - 0.5, u - 0.2), d = Math.hypot(u - 0.2, v - 0.5);
            boolean line = Math.abs(Math.sin(a * 6)) < 0.12 || Math.abs(Math.sin(d * 28)) < 0.15;
            return line ? 0xFF140404 : mix(0xFF8A0C16, 0xFFD01A28, (float) Math.min(1, d * 1.4));
        }),
        new Finish("night", "Night Ops", 0xFF16181C, 0xFF3A3E45, 0xFF8E96A0, (p, u, v, r, ph) -> brushed(0xFF34373E, 0xFF24262B, v, r)),
        new Finish("marble", "Marble Candy", 0xFF141418, 0xFFC8C8D0, 0xFFFFFFFF, (p, u, v, r, ph) -> {
            double s = Math.sin(u * 7 + Math.sin(v * 6 + u * 3) * 3);
            return s > 0.6 ? 0xFFE8303C : s > 0.1 ? 0xFFF6F2EA : s > -0.4 ? 0xFF2E5BFF : 0xFFFFC83A;
        }),
        new Finish("frost", "Frost", 0xFF1A2230, 0xFFCFE6FF, 0xFFFFFFFF, (p, u, v, r, ph) -> {
            int c = mix(0xFFEAF6FF, 0xFF7EC8FF, v);
            return r.nextInt(40) == 0 || Math.abs(Math.sin(u * 23 + v * 5)) < 0.04 ? 0xFFFFFFFF : c;
        }),
        new Finish("damascus", "Damascus", 0xFF2A1C14, 0xFF7E858C, 0xFFE8EEF4, (p, u, v, r, ph) -> {
            double w = Math.sin(v * 38 + Math.sin(u * 9 + v * 6) * 2.4);
            return mix(0xFF4A5058, 0xFFB4BCC4, (float) (w * 0.5 + 0.5));
        }),
        new Finish("gilded", "Gilded", 0xFF2A1408, 0xFFE0B040, 0xFFFFF4D0, (p, u, v, r, ph) -> {
            int c = mix(0xFFFFE08A, 0xFFB07818, v * 0.8f + u * 0.3f);
            boolean engraving = Math.abs(Math.sin(u * 18 + Math.sin(v * 12) * 2)) < 0.12 && v > 0.2f;
            return engraving ? mix(c, 0xFF5A3A08, 0.6f) : c;
        }),
        new Finish("circuit", "Circuit", 0xFF0E0E10, 0xFF3A3A42, 0xFFFF6A6A, (p, u, v, r, ph) -> {
            int gx = (int) (u * 8), gy = (int) (v * 22);
            boolean line = (gx + gy * 3) % 5 == 0 && Math.abs(u * 8 - gx - 0.5f) < 0.18f || gy % 4 == 0 && (gx + gy) % 3 == 0;
            return line ? 0xFFFF2A3A : mix(0xFF141418, 0xFF26262C, v);
        }),
        new Finish("forest", "Forest Camo", 0xFF1E2416, 0xFF5A6048, 0xFFD8E0C8, (p, u, v, r, ph) -> {
            double n = Math.sin(u * 7 + Math.sin(v * 9) * 2) + Math.cos(v * 13 - u * 4);
            return n > 1.0 ? 0xFF2E3A1E : n > 0.2 ? 0xFF55663A : n > -0.6 ? 0xFF7A6A44 : 0xFF3A2E20;
        }),
        new Finish("arctic", "Arctic Camo", 0xFF2A3038, 0xFFB8C4CC, 0xFFFFFFFF, (p, u, v, r, ph) -> {
            double n = Math.sin(u * 8 + Math.sin(v * 10) * 2) + Math.cos(v * 12 - u * 5);
            return n > 0.9 ? 0xFF6E7A86 : n > 0 ? 0xFFDCE4EA : n > -0.8 ? 0xFFA4B0BA : 0xFFF4F8FA;
        }),
        new Finish("black_pearl", "Black Pearl", 0xFF0A0A10, 0xFF6A6A80, 0xFFE0D8FF, (p, u, v, r, ph) -> {
            double s = Math.sin(u * 6 + v * 9 + Math.sin(v * 5) * 2);
            int c = mix(0xFF0E0C1E, 0xFF4A2A8A, (float) Math.max(0, s));
            return mix(c, 0xFF3A6AB0, (float) Math.max(0, -s) * 0.6f);
        }),
        new Finish("toxic", "Toxic", 0xFF0A120A, 0xFF8AA08A, 0xFFEAFFD0, (p, u, v, r, ph) -> {
            double s = Math.sin(u * 9 + Math.sin(v * 7) * 2) * Math.cos(v * 10 - u * 2);
            int c = s > 0.3 ? mix(0xFF2A6A12, 0xFFB8FF3A, (float) s) : mix(0xFF0A2A12, 0xFF1A5A3A, (float) (s + 1) / 2);
            return r.nextInt(70) == 0 ? 0xFFF0FFD0 : c;
        }),
        new Finish("butcher", "Butcher", 0xFF140606, 0xFF9A8888, 0xFFFFE6E6, (p, u, v, r, ph) -> {
            double s = Math.sin(u * 5 + Math.sin(v * 8 + u * 4) * 2.5);
            return s > 0.45 ? 0xFFF2E8E4 : s > -0.2 ? 0xFFC0182A : 0xFF7A0A16;
        }),
        new Finish("violet", "Violet", 0xFF120E18, 0xFF5A4A70, 0xFFE6D6FF, (p, u, v, r, ph) -> brushed(0xFF6A3AA8, 0xFF3A1E62, v, r)),
        // animated finishes: 16 frames, the pack gets a .mcmeta so they move in game
        new Finish("prism", "Prism (animated)", 0xFF101014, 0xFFD0D4E0, 0xFFFFFFFF, 16, (p, u, v, r, ph) -> {
            float hue = (u * 0.35f + v * 0.9f + ph) % 1f;
            return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.75f, 1f) & 0xFFFFFF;
        }),
        new Finish("neon", "Neon (animated)", 0xFF08080C, 0xFF2A2A34, 0xFFFFFFFF, 16, (p, u, v, r, ph) -> {
            float pulse = (float) (0.5 + 0.5 * Math.sin((v * 2 - ph) * Math.PI * 2));
            int c = mix(0xFF0E0820, 0xFF101830, u);
            boolean stripe = Math.abs(Math.sin(v * 20 + u * 3)) < 0.18;
            return stripe ? mix(0xFF00E5FF, 0xFFFF2AD4, pulse) : mix(c, 0xFF00E5FF, pulse * 0.25f);
        })
    );

    private static int brushed(int light, int dark, float v, Random r) {
        int c = mix(light, dark, v);
        return mix(c, 0xFFFFFFFF, 0.1f * r.nextFloat()); // fine brushed grain
    }

    /** Deep gem colour with a bright diagonal shine. */
    private static int gem(int deep, int bright, float u, float v) {
        int c = mix(bright, deep, Math.min(1, v * 0.8f + u * 0.4f));
        float shine = 1 - Math.min(1, Math.abs((u + v) - 0.7f) * 6);
        return mix(c, 0xFFFFFFFF, Math.max(0, shine) * 0.55f);
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = 0;
        for (int sh = 0; sh <= 24; sh += 8) {
            int ca = a >>> sh & 255, cb = b >>> sh & 255;
            r |= Math.round(ca + (cb - ca) * t) << sh;
        }
        return r;
    }

    public static Finish finish(String id) {
        for (Finish f : FINISHES) if (f.id.equals(id)) return f;
        return FINISHES.getFirst();
    }

    /** Side of the finish texture in pixels; regions as in the class comment, as fractions of it. */
    static final int TEX = 128;

    /** Knife texture of a finish (first frame of an animated one). */
    static BufferedImage texture(Finish f) {
        return texture(f, 0f);
    }

    /** Texture at {@code phase} 0..1 through an animated finish. */
    static BufferedImage texture(Finish f, float phase) {
        int n = TEX;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(f.id.hashCode());
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            int c;
            float fx = x / (float) n, fy = y / (float) n;
            if (fx < 0.5f) {
                float u = x / (n / 2f - 1), v = y / (n - 1f);
                c = f.blade.color(P_BLADE, u, v, r, phase);
                float sheen = Math.max(0, 1 - Math.abs(u + v - 0.75f) * 5) * 0.16f; // polished highlight
                c = mix(c, 0xFFFFFFFF, sheen);
            } else if (fx < 0.6875f) {
                // wrapped grip: soft cord ridges winding round the handle (u goes round, v along)
                float s = (float) Math.sin(((fx - 0.5f) / 0.1875f * 2 + fy * 14) * Math.PI * 2);
                c = s > 0 ? mix(f.handle, 0xFFFFFFFF, 0.04f + 0.1f * s) : mix(f.handle, 0xFF000000, 0.3f * -s);
            } else if (fx < 0.75f) {
                c = mix(f.blade.color(P_BLADE, 0.5f, fy, r, phase), 0xFF000000, 0.35f);
            } else if (fy < 0.5f) {
                c = mix(f.metal, 0xFFFFFFFF, 0.07f * r.nextFloat());                                   // brushed metal
            } else {
                c = mix(f.edge, 0xFFFFFFFF, (fx - 0.75f) * 2.4f);                                      // bright bevel
            }
            img.setRGB(x, y, c);
        }
        return img;
    }


    // ================================================================ styles for the menu

    /** The knives and finishes; they write nothing into the pack, KnifeRenderer reads the choice from the config. */
    static List<Style> styles(Group g) {
        List<Style> out = new ArrayList<>();
        if (g.id().equals(MODEL_GROUP)) {
            for (Model m : MODELS) out.add(new Style("knife:" + m.id, m.name, "Knife · replaces every sword", false, false, null, 0, path -> null));
        } else {
            for (Finish f : FINISHES) {
                if (f == FINISHES.getFirst()) continue; // Steel is what "Vanilla" gives on a knife
                out.add(new Style("finish:" + f.id, f.name, f.frames > 1 ? "Animated knife finish" : "Knife finish", false, false, null, 0, path -> null));
            }
        }
        return out;
    }

    /** The finish chosen in the menu (Steel when none). */
    public static Finish chosenFinish() {
        String id = Config.INSTANCE.styleOf(FINISH_GROUP);
        return id.startsWith("finish:") ? finish(id.substring(7)) : FINISHES.getFirst();
    }

    // ================================================================ preview

    /** 3D preview: the model group shows each knife in Steel, the finish group shows each finish on a Karambit. */
    static BufferedImage preview(Group g, Style s) {
        Model m;
        Finish f;
        if (g.id().equals(MODEL_GROUP)) {
            m = s.id.startsWith("knife:") ? model(s.id.substring(6)) : null;
            f = FINISHES.getFirst();
            if (m == null) { // vanilla / packs: the plain sword
                byte[] png = s.read("textures/item/netherite_sword.png");
                if (png == null) png = Styles.vanilla("textures/item/netherite_sword.png");
                try { return png == null ? null : Images.decode(png); } catch (java.io.IOException e) { return null; }
            }
        } else {
            m = MODELS.getFirst();
            f = s.id.startsWith("finish:") ? finish(s.id.substring(7)) : FINISHES.getFirst();
        }
        return render(m, texture(f), Previews.SIZE);
    }

    /**
     * Software render of a knife for the menu: lies diagonally like a sword icon, slightly turned so the grind shows;
     * lit and drawn back to front at 3x, then scaled down for smooth edges.
     */
    static BufferedImage render(Model m, BufferedImage tex, int size) {
        record Quad(float[] xs, float[] ys, float z, int rgb) {}
        org.joml.Matrix3f view = new org.joml.Matrix3f().rotateX((float) Math.toRadians(14)).rotateY((float) Math.toRadians(-30))
            .rotateZ((float) Math.toRadians(-45));
        org.joml.Vector3f light = new org.joml.Vector3f(-0.35f, 0.8f, 0.6f).normalize();
        List<Quad> quads = new ArrayList<>();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (KnifeMesh mesh : new KnifeMesh[] {m.mesh, m.safe, m.bite}) {
            if (mesh == null) continue;
            for (int q = 0; q < mesh.vertices(); q += 4) {
                float[] xs = new float[4], ys = new float[4];
                float z = 0, u = 0, v = 0;
                org.joml.Vector3f n = new org.joml.Vector3f();
                for (int k = 0; k < 4; k++) {
                    int i = q + k;
                    org.joml.Vector3f p = view.transform(new org.joml.Vector3f(mesh.get(i, 0) - 8, mesh.get(i, 1) - 8, mesh.get(i, 2) - 8));
                    xs[k] = p.x;
                    ys[k] = -p.y;
                    z += p.z / 4;
                    n.add(mesh.get(i, 3), mesh.get(i, 4), mesh.get(i, 5));
                    u += mesh.get(i, 6) / 4;
                    v += mesh.get(i, 7) / 4;
                    minX = Math.min(minX, xs[k]); maxX = Math.max(maxX, xs[k]); minY = Math.min(minY, ys[k]); maxY = Math.max(maxY, ys[k]);
                }
                view.transform(n.normalize());
                if (n.z < -0.05f) continue; // facing away
                int c = tex.getRGB(Math.min(tex.getWidth() - 1, (int) (u * tex.getWidth())), Math.min(tex.getHeight() - 1, (int) (v * tex.getHeight())));
                c = mix(c, 0xFF000000, 1 - (mesh.color(q) & 255) / 255f);
                float diffuse = Math.max(0, n.dot(light));
                org.joml.Vector3f r = new org.joml.Vector3f(n).mul(2 * n.dot(light)).sub(light);
                boolean shiny = u < 0.5f || u >= 0.75f;
                float spec = (float) Math.pow(Math.max(0, r.z), shiny ? 18 : 6) * (shiny ? 0.6f : 0.12f);
                float shade = 0.4f + 0.6f * diffuse;
                int rgb = 0xFF000000;
                for (int sh = 0; sh <= 16; sh += 8) {
                    float ch = (c >> sh & 255) * shade + 255 * spec;
                    rgb |= Math.min(255, Math.round(ch)) << sh;
                }
                quads.add(new Quad(xs, ys, z, rgb));
            }
        }
        int ss = 3, big = size * ss;
        float sc = Math.min((big - 6 * ss) / (maxX - minX), (big - 6 * ss) / (maxY - minY));
        float ox = (big - (maxX - minX) * sc) / 2 - minX * sc, oy = (big - (maxY - minY) * sc) / 2 - minY * sc;
        quads.sort((a, b) -> Float.compare(a.z, b.z));
        BufferedImage hi = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = hi.createGraphics();
        for (Quad q : quads) {
            Path2D.Float p = new Path2D.Float();
            p.moveTo(q.xs[0] * sc + ox, q.ys[0] * sc + oy);
            for (int k = 1; k < 4; k++) p.lineTo(q.xs[k] * sc + ox, q.ys[k] * sc + oy);
            p.closePath();
            g.setColor(new java.awt.Color(q.rgb, true));
            g.fill(p);
        }
        g.dispose();
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D o = out.createGraphics();
        o.drawImage(hi.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        o.dispose();
        return out;
    }

    private Knives() {}
}
