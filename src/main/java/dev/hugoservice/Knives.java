package dev.hugoservice;

import dev.hugoservice.Categories.Group;
import dev.hugoservice.Styles.Style;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Knife skins: own 3D knife models (built from rotated boxes in the vanilla block-model format) that replace every
 * sword through the generated pack, plus generated finishes. All designs are original work of this mod.
 *
 * <p>Models stand upright in model space (grip at the bottom, tip at the top); the display transforms turn them
 * 45° so they lie in the hand and in the GUI like a sword sprite. Texture: 64x64, UV regions in 0..16 units:
 * blade u 0-8, handle u 8-11, fuller u 11-12, metal u 12-16 v 0-8, edge u 12-16 v 8-16.
 */
public final class Knives {
    public static final String MODEL_GROUP = "knife", FINISH_GROUP = "knife_finish";
    public static final String TEXTURE = "textures/item/hugoservice_knife.png";
    private static final String[] SWORDS = {"wooden", "stone", "copper", "iron", "golden", "diamond", "netherite"};

    /** Every sword model plus the knife texture (the texture defaults to Steel, the finish group overrides it). */
    public static List<String> modelFiles() {
        List<String> out = new ArrayList<>();
        for (String s : SWORDS) out.add("models/item/" + s + "_sword.json");
        out.add(TEXTURE);
        out.add("items/hugoservice_knife.json");         // the knife under its own name, see swap()
        out.add("models/item/hugoservice_knife.json");
        for (String p : BF_PARTS) { out.add("items/" + p + ".json"); out.add("models/item/" + p + ".json"); } // butterfly tricks
        return out;
    }

    public static boolean isKnifeGroup(Group g) {
        return g.id().equals(MODEL_GROUP) || g.id().equals(FINISH_GROUP);
    }

    // ================================================================ models

    enum Part { BLADE, HANDLE, METAL, EDGE, FULLER }

    /** How the knife moves when inspected or drawn. */
    public enum Anim { SPIN, FLIP, TURN, FLICK, HEAVY, TWIRL, PUSH }

    /** Box in model units, rotated by {@code angle} degrees about Z through (ox, oy); angle within ±45 in 22.5 steps. */
    record Box(Part part, float x0, float y0, float z0, float x1, float y1, float z1, float angle, float ox, float oy) {}

    public record Model(String id, String name, Anim anim, List<Box> boxes) {}

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

    /** The knife's own item model id; server packs that re-skin swords do not know it, so they cannot cover it. */
    public static final net.minecraft.util.Identifier KNIFE_ID = net.minecraft.util.Identifier.ofVanilla("hugoservice_knife");
    private static final net.minecraft.util.Identifier NO_MODEL = net.minecraft.util.Identifier.of("hugoservice", "no_such_model");
    private static final byte[] KNIFE_ITEM = "{\"model\": {\"type\": \"minecraft:model\", \"model\": \"minecraft:item/hugoservice_knife\"}}"
        .getBytes(StandardCharsets.UTF_8);

    /** True when this stack is a normal sword that is drawn as the chosen knife (custom server items keep their look). */
    public static boolean replaces(net.minecraft.item.ItemStack stack) {
        if (active() == null || !stack.isIn(net.minecraft.registry.tag.ItemTags.SWORDS)) return false;
        net.minecraft.util.Identifier model = stack.get(net.minecraft.component.DataComponentTypes.ITEM_MODEL);
        return model != null && model.equals(net.minecraft.registry.Registries.ITEM.getId(stack.getItem()));
    }

    /**
     * Item model to draw for a stack: the chosen knife for every normal sword, so it wins even over server resource
     * packs (they sit above every local pack). Only once the knife is loaded, else the sword stays.
     */
    public static net.minecraft.util.Identifier swap(net.minecraft.item.ItemStack stack, net.minecraft.util.Identifier id) {
        if (id == null || !replaces(stack)) return id;
        var models = net.minecraft.client.MinecraftClient.getInstance().getBakedModelManager();
        return models.getItemModel(KNIFE_ID) != models.getItemModel(NO_MODEL) ? KNIFE_ID : id;
    }

    private static void box(List<Box> b, Part p, float x0, float y0, float x1, float y1, float depth) {
        b.add(new Box(p, x0, y0, 8 - depth / 2, x1, y1, 8 + depth / 2, 0, 8, 8));
    }

    /** Box of w x h centred at (cx, cy), turned by angle degrees (any multiple of 22.5). */
    private static void rot(List<Box> b, Part p, float cx, float cy, float w, float h, float angle, float depth) {
        while (angle > 45) { angle -= 90; float t = w; w = h; h = t; }
        while (angle < -45) { angle += 90; float t = w; w = h; h = t; }
        b.add(new Box(p, cx - w / 2, cy - h / 2, 8 - depth / 2, cx + w / 2, cy + h / 2, 8 + depth / 2, angle, cx, cy));
    }

    /** A segment of length len starting at (x, y) heading angle degrees from straight up (positive = to the left). */
    private static float[] seg(List<Box> b, Part p, float x, float y, float angle, float len, float w, float depth) {
        double a = Math.toRadians(angle);
        float dx = (float) -Math.sin(a), dy = (float) Math.cos(a);
        rot(b, p, x + dx * len / 2, y + dy * len / 2, w, len, angle, depth);
        return new float[] {x + dx * len, y + dy * len};
    }

    private static Model karambit() {
        List<Box> b = new ArrayList<>();
        // finger ring
        box(b, Part.METAL, 6.6f, 0.2f, 9.4f, 0.8f, 1.1f);
        box(b, Part.METAL, 6.6f, 2.6f, 9.4f, 3.2f, 1.1f);
        box(b, Part.METAL, 6.6f, 0.8f, 7.2f, 2.6f, 1.1f);
        box(b, Part.METAL, 8.8f, 0.8f, 9.4f, 2.6f, 1.1f);
        float[] p = seg(b, Part.HANDLE, 8, 3.2f, -22.5f, 4.2f, 1.5f, 1.4f);
        p = seg(b, Part.METAL, p[0], p[1], -22.5f, 0.6f, 1.8f, 1.6f);
        // the claw: each piece bends further to the left
        float[] angles = {0, 22.5f, 45, 67.5f}, lens = {2.6f, 2.4f, 2.0f, 1.6f}, widths = {1.5f, 1.35f, 1.1f, 0.7f};
        for (int i = 0; i < angles.length; i++) {
            double a = Math.toRadians(angles[i]);
            float lx = (float) -Math.cos(a), ly = (float) -Math.sin(a); // left of the heading
            float off = widths[i] / 2 - 0.15f;
            seg(b, Part.EDGE, p[0] + lx * off, p[1] + ly * off, angles[i], lens[i], 0.3f, 0.7f);
            p = seg(b, Part.BLADE, p[0], p[1], angles[i], lens[i], widths[i], 0.6f);
        }
        return new Model("karambit", "Karambit", Anim.SPIN, List.copyOf(b));
    }

    /** Bigger claw than the karambit with a longer, wider curve. */
    private static Model talon() {
        List<Box> b = new ArrayList<>();
        box(b, Part.METAL, 6.5f, 0.0f, 9.5f, 0.6f, 1.2f);
        box(b, Part.METAL, 6.5f, 2.6f, 9.5f, 3.2f, 1.2f);
        box(b, Part.METAL, 6.5f, 0.6f, 7.1f, 2.6f, 1.2f);
        box(b, Part.METAL, 8.9f, 0.6f, 9.5f, 2.6f, 1.2f);
        float[] p = seg(b, Part.HANDLE, 8, 3.2f, -22.5f, 3.6f, 1.7f, 1.5f);
        p = seg(b, Part.METAL, p[0], p[1], -22.5f, 0.6f, 2.0f, 1.7f);
        float[] angles = {0, 22.5f, 45, 67.5f, 90}, lens = {2.8f, 2.6f, 2.3f, 2.0f, 1.4f}, widths = {1.9f, 1.7f, 1.4f, 1.0f, 0.6f};
        for (int i = 0; i < angles.length; i++) {
            double a = Math.toRadians(angles[i]);
            float lx = (float) -Math.cos(a), ly = (float) -Math.sin(a);
            float off = widths[i] / 2 - 0.15f;
            seg(b, Part.EDGE, p[0] + lx * off, p[1] + ly * off, angles[i], lens[i], 0.3f, 0.7f);
            if (i < 3) seg(b, Part.FULLER, p[0] - lx * 0.1f, p[1] - ly * 0.1f, angles[i], lens[i], 0.3f, 0.66f);
            p = seg(b, Part.BLADE, p[0], p[1], angles[i], lens[i], widths[i], 0.6f);
        }
        return new Model("talon", "Talon", Anim.SPIN, List.copyOf(b));
    }

    /** Military bayonet with grip ribs, a ringed guard and a saw-toothed spine. */
    private static Model m9() {
        List<Box> b = new ArrayList<>();
        box(b, Part.METAL, 7.1f, 0.2f, 8.9f, 1.0f, 1.8f);
        box(b, Part.HANDLE, 7.2f, 1.0f, 8.8f, 5.6f, 1.7f);
        for (float y = 1.6f; y < 5.2f; y += 0.8f) box(b, Part.METAL, 7.15f, y, 8.85f, y + 0.2f, 1.75f);
        box(b, Part.METAL, 6.0f, 5.6f, 10.0f, 6.3f, 1.5f);
        rot(b, Part.METAL, 9.9f, 5.95f, 0.9f, 0.9f, 45, 1.5f);
        box(b, Part.BLADE, 7.0f, 6.3f, 8.9f, 12.6f, 0.6f);
        rot(b, Part.BLADE, 7.95f, 12.7f, 1.35f, 1.35f, 45, 0.6f);
        for (float y = 7.2f; y < 10.6f; y += 0.8f) rot(b, Part.METAL, 8.95f, y, 0.45f, 0.45f, 45, 0.62f);
        box(b, Part.FULLER, 7.85f, 7.0f, 8.15f, 11.2f, 0.66f);
        box(b, Part.EDGE, 7.0f, 6.3f, 7.25f, 12.6f, 0.7f);
        return new Model("m9", "M9 Bayonet", Anim.TURN, List.copyOf(b));
    }

    /** Long, slim blade with a thin cross guard. */
    private static Model stiletto() {
        List<Box> b = new ArrayList<>();
        box(b, Part.METAL, 7.4f, 0.2f, 8.6f, 0.8f, 1.4f);
        box(b, Part.HANDLE, 7.4f, 0.8f, 8.6f, 5.4f, 1.3f);
        box(b, Part.METAL, 7.3f, 5.4f, 8.7f, 5.9f, 1.4f);
        box(b, Part.METAL, 6.4f, 5.9f, 9.6f, 6.3f, 1.0f);
        box(b, Part.BLADE, 7.45f, 6.3f, 8.55f, 13.6f, 0.5f);
        rot(b, Part.BLADE, 8.0f, 13.6f, 0.78f, 0.78f, 45, 0.5f);
        box(b, Part.FULLER, 7.9f, 6.6f, 8.1f, 12.6f, 0.56f);
        box(b, Part.EDGE, 7.45f, 6.3f, 7.65f, 13.6f, 0.6f);
        return new Model("stiletto", "Stiletto", Anim.FLICK, List.copyOf(b));
    }

    /** Big survival knife: finger guard, sawback and a wide clipped blade. */
    private static Model huntsman() {
        List<Box> b = new ArrayList<>();
        box(b, Part.METAL, 7.0f, 0.2f, 9.0f, 0.9f, 1.8f);
        box(b, Part.HANDLE, 7.2f, 0.9f, 8.8f, 5.8f, 1.7f);
        box(b, Part.METAL, 6.4f, 5.8f, 9.8f, 6.4f, 1.6f);
        box(b, Part.METAL, 6.4f, 4.6f, 7.0f, 5.8f, 1.6f);
        box(b, Part.BLADE, 7.0f, 6.4f, 9.4f, 12.8f, 0.6f);
        rot(b, Part.BLADE, 8.2f, 12.9f, 1.7f, 1.7f, 45, 0.6f);
        for (float y = 7.0f; y < 10.4f; y += 0.8f) rot(b, Part.METAL, 9.4f, y, 0.5f, 0.5f, 45, 0.62f);
        box(b, Part.FULLER, 8.0f, 7.0f, 8.3f, 11.6f, 0.66f);
        box(b, Part.EDGE, 7.0f, 6.4f, 7.25f, 12.8f, 0.7f);
        return new Model("huntsman", "Huntsman", Anim.HEAVY, List.copyOf(b));
    }

    /** Push dagger: a T-grip in the fist and a short triangular blade. */
    private static Model push() {
        List<Box> b = new ArrayList<>();
        box(b, Part.HANDLE, 5.6f, 2.2f, 10.4f, 3.6f, 1.6f);
        box(b, Part.METAL, 5.4f, 2.0f, 5.9f, 3.8f, 1.7f);
        box(b, Part.METAL, 10.1f, 2.0f, 10.6f, 3.8f, 1.7f);
        box(b, Part.METAL, 7.4f, 3.6f, 8.6f, 4.4f, 1.4f);
        box(b, Part.BLADE, 7.0f, 4.4f, 9.0f, 6.4f, 0.6f);
        box(b, Part.BLADE, 7.3f, 6.4f, 8.7f, 8.0f, 0.6f);
        box(b, Part.BLADE, 7.6f, 8.0f, 8.4f, 9.2f, 0.6f);
        rot(b, Part.BLADE, 8.0f, 9.2f, 0.57f, 0.57f, 45, 0.6f);
        box(b, Part.FULLER, 7.9f, 4.6f, 8.1f, 8.4f, 0.66f);
        box(b, Part.EDGE, 7.0f, 4.4f, 7.2f, 6.4f, 0.7f);
        box(b, Part.EDGE, 8.8f, 4.4f, 9.0f, 6.4f, 0.7f);
        return new Model("push", "Push Dagger", Anim.PUSH, List.copyOf(b));
    }

    // Butterfly parts: blade (with the pivot pins) and the two handles, so tricks can swing the handles.
    static List<Box> BF_BLADE, BF_SAFE, BF_BITE;
    /** Pivot of each handle in model units: they swing around these, in the knife's plane. */
    public static final float[] BF_PIVOT_SAFE = {7.2f, 7.3f}, BF_PIVOT_BITE = {8.8f, 7.3f};

    private static Model butterfly() {
        List<Box> blade = new ArrayList<>(), safe = new ArrayList<>(), bite = new ArrayList<>();
        box(blade, Part.METAL, 6.6f, 7.0f, 9.4f, 7.6f, 1.7f);   // pivot pins
        box(blade, Part.BLADE, 7.3f, 7.6f, 8.7f, 12.6f, 0.6f);
        rot(blade, Part.BLADE, 8.0f, 12.6f, 1.0f, 1.0f, 45, 0.6f);
        box(blade, Part.FULLER, 7.85f, 8.0f, 8.15f, 11.8f, 0.66f);
        box(blade, Part.EDGE, 7.3f, 7.6f, 7.55f, 12.6f, 0.7f);
        box(safe, Part.HANDLE, 6.7f, 0.9f, 7.7f, 7.0f, 1.4f);   // safe handle
        box(safe, Part.METAL, 7.0f, 1.6f, 7.4f, 6.4f, 1.5f);    // its channel insert
        box(bite, Part.HANDLE, 8.3f, 0.9f, 9.3f, 7.0f, 1.4f);   // bite handle
        box(bite, Part.METAL, 8.6f, 1.6f, 9.0f, 6.4f, 1.5f);
        box(bite, Part.METAL, 8.3f, 0.2f, 9.3f, 0.9f, 1.6f);    // latch
        box(safe, Part.METAL, 6.7f, 0.2f, 7.7f, 0.9f, 1.6f);
        BF_BLADE = List.copyOf(blade);
        BF_SAFE = List.copyOf(safe);
        BF_BITE = List.copyOf(bite);
        List<Box> all = new ArrayList<>(blade);
        all.addAll(safe);
        all.addAll(bite);
        return new Model("butterfly", "Butterfly", Anim.FLIP, List.copyOf(all));
    }

    /** Item ids of the three butterfly parts (minecraft namespace, written into the pack with the butterfly). */
    public static final String[] BF_PARTS = {"hugoservice_bfly_blade", "hugoservice_bfly_safe", "hugoservice_bfly_bite"};

    /** items/ and models/ files of the parts: each part keeps the knife's display, so together they are the knife. */
    private static java.util.Map<String, byte[]> butterflyFiles() {
        java.util.Map<String, byte[]> out = new java.util.HashMap<>();
        List<List<Box>> parts = List.of(BF_BLADE, BF_SAFE, BF_BITE);
        for (int i = 0; i < 3; i++) {
            String id = BF_PARTS[i];
            out.put("items/" + id + ".json", ("{\"model\": {\"type\": \"minecraft:model\", \"model\": \"minecraft:item/" + id + "\"}}")
                .getBytes(StandardCharsets.UTF_8));
            out.put("models/item/" + id + ".json", json(new Model(id, id, Anim.FLIP, parts.get(i))).getBytes(StandardCharsets.UTF_8));
        }
        return out;
    }

    private static Model bayonet() {
        List<Box> b = new ArrayList<>();
        box(b, Part.METAL, 7.1f, 0.3f, 8.9f, 1.1f, 1.7f);   // pommel
        box(b, Part.HANDLE, 7.3f, 1.1f, 8.7f, 5.6f, 1.5f);
        box(b, Part.METAL, 7.25f, 2.3f, 8.75f, 2.6f, 1.6f); // grip rings
        box(b, Part.METAL, 7.25f, 3.8f, 8.75f, 4.1f, 1.6f);
        box(b, Part.METAL, 6.2f, 5.6f, 9.8f, 6.3f, 1.4f);   // guard
        box(b, Part.BLADE, 7.2f, 6.3f, 8.8f, 13.0f, 0.6f);
        rot(b, Part.BLADE, 8.0f, 13.0f, 1.13f, 1.13f, 45, 0.6f);
        box(b, Part.FULLER, 7.85f, 7.0f, 8.15f, 11.6f, 0.66f);
        box(b, Part.EDGE, 7.2f, 6.3f, 7.45f, 13.0f, 0.7f);
        return new Model("bayonet", "Bayonet", Anim.TURN, List.copyOf(b));
    }

    private static Model flip() {
        List<Box> b = new ArrayList<>();
        box(b, Part.METAL, 7.2f, 0.3f, 8.8f, 0.9f, 1.6f);
        box(b, Part.HANDLE, 7.2f, 0.9f, 8.8f, 6.4f, 1.5f);
        box(b, Part.METAL, 8.8f, 6.0f, 9.5f, 6.9f, 0.8f);   // flipper
        box(b, Part.BLADE, 7.2f, 6.4f, 9.0f, 11.2f, 0.6f);
        rot(b, Part.BLADE, 8.1f, 11.3f, 1.27f, 1.27f, 45, 0.6f);
        box(b, Part.FULLER, 7.95f, 7.0f, 8.25f, 10.2f, 0.66f);
        box(b, Part.EDGE, 7.2f, 6.4f, 7.45f, 11.3f, 0.7f);
        return new Model("flip", "Flip Knife", Anim.FLICK, List.copyOf(b));
    }

    private static Model bowie() {
        List<Box> b = new ArrayList<>();
        box(b, Part.METAL, 7.0f, 0.2f, 9.0f, 1.0f, 1.8f);
        box(b, Part.HANDLE, 7.2f, 1.0f, 8.8f, 6.0f, 1.6f);
        box(b, Part.METAL, 6.0f, 6.0f, 10.0f, 6.7f, 1.5f);
        box(b, Part.BLADE, 7.0f, 6.7f, 9.2f, 13.0f, 0.6f);
        rot(b, Part.BLADE, 8.1f, 13.1f, 1.55f, 1.55f, 45, 0.6f);
        for (float y = 8; y <= 11; y++) rot(b, Part.METAL, 9.2f, y, 0.5f, 0.5f, 45, 0.62f); // spine teeth
        box(b, Part.FULLER, 7.9f, 7.3f, 8.25f, 12.0f, 0.66f);
        box(b, Part.EDGE, 7.0f, 6.7f, 7.25f, 13.0f, 0.7f);
        return new Model("bowie", "Bowie", Anim.HEAVY, List.copyOf(b));
    }

    private static Model gut() {
        List<Box> b = new ArrayList<>();
        box(b, Part.METAL, 7.2f, 0.3f, 8.8f, 0.9f, 1.6f);
        box(b, Part.HANDLE, 7.25f, 0.9f, 8.75f, 6.2f, 1.5f);
        box(b, Part.METAL, 7.0f, 6.2f, 9.0f, 6.8f, 1.6f);
        box(b, Part.BLADE, 7.2f, 6.8f, 8.9f, 12.0f, 0.6f);
        rot(b, Part.BLADE, 9.1f, 11.0f, 0.7f, 1.2f, -22.5f, 0.6f); // gut hook
        rot(b, Part.EDGE, 8.95f, 10.6f, 0.25f, 0.8f, -22.5f, 0.7f);
        rot(b, Part.BLADE, 8.05f, 12.0f, 1.2f, 1.2f, 45, 0.6f);
        box(b, Part.EDGE, 7.2f, 6.8f, 7.45f, 12.0f, 0.7f);
        return new Model("gut", "Gut Knife", Anim.TWIRL, List.copyOf(b));
    }

    // ================================================================ UVs and model JSON

    /** UV rect (u0, v0, u1, v1) of a face, in 0..16, picked from the box position so patterns flow along the knife. */
    private static float[] uv(Box b, char face) {
        float ua, ub, va, vb;
        switch (face) {
            case 'e', 'w' -> { ua = b.z0 + 4; ub = b.z1 + 4; } // thin sides: a narrow slice
            default -> { ua = b.x0; ub = b.x1; }
        }
        if (face == 'u' || face == 'd') { va = b.y1 - 0.5f; vb = b.y1; } else { va = b.y0; vb = b.y1; }
        float u0, u1, v0, v1;
        switch (b.part) {
            case BLADE -> { u0 = map(ua, 6.5f, 9.5f, 0, 8); u1 = map(ub, 6.5f, 9.5f, 0, 8); v0 = map(vb, 15, 6, 0, 16); v1 = map(va, 15, 6, 0, 16); }
            case HANDLE -> { u0 = map(ua, 6.5f, 9.5f, 8, 11); u1 = map(ub, 6.5f, 9.5f, 8, 11); v0 = map(vb, 8, 0, 0, 16); v1 = map(va, 8, 0, 0, 16); }
            case METAL -> { u0 = map(ua, 6, 10, 12, 16); u1 = map(ub, 6, 10, 12, 16); v0 = map(vb, 16, 0, 0, 8); v1 = map(va, 16, 0, 0, 8); }
            case FULLER -> { u0 = 11.1f; u1 = 11.9f; v0 = map(vb, 15, 6, 0, 16); v1 = map(va, 15, 6, 0, 16); }
            default -> { u0 = map(ua, 6.5f, 9.5f, 12, 16); u1 = map(ub, 6.5f, 9.5f, 12, 16); v0 = map(vb, 15, 6, 8, 16); v1 = map(va, 15, 6, 8, 16); }
        }
        if (u1 - u0 < 0.05f) u1 = Math.min(16, u0 + 0.25f);
        if (v1 - v0 < 0.05f) v1 = Math.min(16, v0 + 0.25f);
        return new float[] {u0, v0, u1, v1};
    }

    private static float map(float v, float a, float b, float c, float d) {
        float t = Math.max(0, Math.min(1, (v - a) / (b - a)));
        return Math.round((c + (d - c) * t) * 100) / 100f;
    }

    private static final String DISPLAY = """
          "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 10], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -100], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, -90, -20], "translation": [1.13, 3.6, 1.13], "scale": [0.6, 0.6, 0.6]},
            "firstperson_lefthand": {"rotation": [0, 90, -70], "translation": [1.13, 3.6, 1.13], "scale": [0.6, 0.6, 0.6]},
            "gui": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "ground": {"rotation": [0, 0, -45], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 180, -45], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}
          }
        """;

    static String json(Model m) {
        StringBuilder sb = new StringBuilder("{\n  \"textures\": {\"knife\": \"minecraft:item/hugoservice_knife\", \"particle\": \"minecraft:item/hugoservice_knife\"},\n");
        sb.append("  \"gui_light\": \"front\",\n  \"elements\": [\n");
        for (int i = 0; i < m.boxes.size(); i++) {
            Box b = m.boxes.get(i);
            sb.append("    {\"from\": [").append(n(b.x0)).append(", ").append(n(b.y0)).append(", ").append(n(b.z0))
                .append("], \"to\": [").append(n(b.x1)).append(", ").append(n(b.y1)).append(", ").append(n(b.z1)).append("], ");
            if (b.angle != 0) sb.append("\"rotation\": {\"angle\": ").append(n(b.angle)).append(", \"axis\": \"z\", \"origin\": [")
                .append(n(b.ox)).append(", ").append(n(b.oy)).append(", 8]}, ");
            sb.append("\"faces\": {");
            String[] names = {"north", "south", "east", "west", "up", "down"};
            for (int f = 0; f < 6; f++) {
                float[] r = uv(b, names[f].charAt(0) == 'n' || names[f].charAt(0) == 's' ? 'n' : names[f].charAt(0));
                sb.append(f == 0 ? "" : ", ").append('"').append(names[f]).append("\": {\"uv\": [")
                    .append(n(r[0])).append(", ").append(n(r[1])).append(", ").append(n(r[2])).append(", ").append(n(r[3]))
                    .append("], \"texture\": \"#knife\"}");
            }
            sb.append("}}").append(i + 1 < m.boxes.size() ? ",\n" : "\n");
        }
        sb.append("  ],\n").append(DISPLAY).append("}\n");
        return sb.toString();
    }

    private static String n(float v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.3f", v).replaceAll("0+$", "");
    }

    // ================================================================ finishes

    /** Blade colour at (u, v) in 0..1 (v = 0 at the tip); {@code phase} 0..1 runs through the frames of an animated finish. */
    private interface Painter { int color(int part, float u, float v, Random r, float phase); }

    /** {@code frames} above 1 makes an animated texture (the pack gets a .mcmeta for it). */
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
        return r.nextInt(5) == 0 ? mix(c, 0xFFFFFFFF, 0.15f) : c;
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

    /** 64x64 knife texture of a finish (first frame of an animated one). */
    static BufferedImage texture(Finish f) {
        return texture(f, 0);
    }

    static BufferedImage texture(Finish f, int frame) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(f.id.hashCode());
        float phase = frame / (float) f.frames;
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            int c;
            float u = x / 31f, v = y / 63f;
            if (x < 32) {
                c = f.blade.color(P_BLADE, u, v, r, phase);
                float sheen = Math.max(0, 1 - Math.abs(u + v - 0.75f) * 5) * 0.16f; // polished highlight
                c = mix(c, 0xFFFFFFFF, sheen);
            } else if (x < 44) {
                c = (x + y) % 6 < 2 ? mix(f.handle, 0xFFFFFFFF, 0.12f) : f.handle;                    // wrapped grip
            } else if (x < 48) {
                c = mix(f.blade.color(P_BLADE, 0.5f, v, r, phase), 0xFF000000, 0.35f);                // fuller: darker groove
            } else if (y < 32) {
                c = mix(f.metal, 0xFF000000, (y % 8) / 40f);                                            // machined metal
            } else {
                c = mix(f.edge, 0xFFFFFFFF, (x - 48) / 40f);                                            // bright bevel
            }
            img.setRGB(x, y, c);
        }
        return img;
    }

    /** All frames of a finish stacked vertically, the layout Minecraft animates. */
    static BufferedImage strip(Finish f) {
        BufferedImage img = new BufferedImage(64, 64 * f.frames, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        for (int i = 0; i < f.frames; i++) g.drawImage(texture(f, i), 0, 64 * i, null);
        g.dispose();
        return img;
    }

    // ================================================================ styles for the menu

    static List<Style> styles(Group g) {
        List<Style> out = new ArrayList<>();
        if (g.id().equals(MODEL_GROUP)) {
            for (Model m : MODELS) {
                byte[] model = json(m).getBytes(StandardCharsets.UTF_8);
                java.util.Map<String, byte[]> parts = m.id.equals("butterfly") ? butterflyFiles() : java.util.Map.of();
                out.add(new Style("knife:" + m.id, m.name, "Knife · replaces every sword", false, false, null, 0,
                    path -> path.equals(TEXTURE) ? Images.encode(texture(FINISHES.getFirst()))
                        : path.endsWith("_sword.json") || path.equals("models/item/hugoservice_knife.json") ? model
                        : path.equals("items/hugoservice_knife.json") ? KNIFE_ITEM : parts.get(path)));
            }
        } else {
            for (Finish f : FINISHES) {
                if (f == FINISHES.getFirst()) continue; // Steel is what "Vanilla" gives on a knife
                byte[] meta = ("{\"animation\": {\"frametime\": 2, \"interpolate\": true}}").getBytes(StandardCharsets.UTF_8);
                out.add(new Style("finish:" + f.id, f.name, f.frames > 1 ? "Animated knife finish" : "Knife finish", false, false, null, 0,
                    path -> path.equals(TEXTURE) ? Images.encode(f.frames > 1 ? strip(f) : texture(f))
                        : path.equals(TEXTURE + ".mcmeta") && f.frames > 1 ? meta : null));
            }
        }
        return out;
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

    static BufferedImage render(Model m, BufferedImage tex, int size) {
        record Face(BufferedImage img, float[] o, float[] a, float[] b, float depth) {}
        List<Face> faces = new ArrayList<>();
        // knife upright -> diagonal like a sword icon, slightly turned so the thickness shows
        org.joml.Matrix3f view = new org.joml.Matrix3f().rotateX((float) Math.toRadians(12)).rotateY((float) Math.toRadians(-28))
            .rotateZ((float) Math.toRadians(-45));
        org.joml.Vector3f light = new org.joml.Vector3f(0.4f, 1f, 0.7f).normalize();
        float k = tex.getWidth() / 16f;
        for (Box b : m.boxes) {
            org.joml.Matrix3f rz = new org.joml.Matrix3f().rotateZ((float) Math.toRadians(b.angle));
            // faces as origin + two edges (local box space), texture face letter
            Object[][] spec = {
                {new float[] {b.x0, b.y1, b.z1}, new float[] {b.x1 - b.x0, 0, 0}, new float[] {0, b.y0 - b.y1, 0}, 'n'}, // south (front)
                {new float[] {b.x1, b.y1, b.z0}, new float[] {b.x0 - b.x1, 0, 0}, new float[] {0, b.y0 - b.y1, 0}, 'n'}, // north
                {new float[] {b.x1, b.y1, b.z1}, new float[] {0, 0, b.z0 - b.z1}, new float[] {0, b.y0 - b.y1, 0}, 'e'},
                {new float[] {b.x0, b.y1, b.z0}, new float[] {0, 0, b.z1 - b.z0}, new float[] {0, b.y0 - b.y1, 0}, 'w'},
                {new float[] {b.x0, b.y1, b.z0}, new float[] {b.x1 - b.x0, 0, 0}, new float[] {0, 0, b.z1 - b.z0}, 'u'},
                {new float[] {b.x0, b.y0, b.z1}, new float[] {b.x1 - b.x0, 0, 0}, new float[] {0, 0, b.z0 - b.z1}, 'd'},
            };
            for (Object[] sp : spec) {
                float[] o = (float[]) sp[0], ea = (float[]) sp[1], eb = (float[]) sp[2];
                org.joml.Vector3f po = new org.joml.Vector3f(o[0] - b.ox, o[1] - b.oy, o[2] - 8);
                rz.transform(po).add(b.ox - 8, b.oy - 8, 0);
                org.joml.Vector3f va = rz.transform(new org.joml.Vector3f(ea[0], ea[1], ea[2]));
                org.joml.Vector3f vb = rz.transform(new org.joml.Vector3f(eb[0], eb[1], eb[2]));
                float shade = 0.6f + 0.4f * Math.max(0, new org.joml.Vector3f(va).cross(vb).normalize().dot(light));
                view.transform(po);
                view.transform(va);
                view.transform(vb);
                float[] r = uv(b, (Character) sp[3]);
                int tx = Math.round(r[0] * k), ty = Math.round(r[1] * k);
                int tw = Math.max(1, Math.round((r[2] - r[0]) * k)), th = Math.max(1, Math.round((r[3] - r[1]) * k));
                tx = Math.min(tx, tex.getWidth() - tw);
                ty = Math.min(ty, tex.getHeight() - th);
                BufferedImage img = Previews.shade(tex.getSubimage(tx, ty, tw, th), shade);
                faces.add(new Face(img, new float[] {po.x, -po.y}, new float[] {va.x, -va.y}, new float[] {vb.x, -vb.y},
                    po.z + (va.z + vb.z) / 2));
            }
        }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Face f : faces) {
            float[][] pts = {f.o, {f.o[0] + f.a[0], f.o[1] + f.a[1]}, {f.o[0] + f.b[0], f.o[1] + f.b[1]}, {f.o[0] + f.a[0] + f.b[0], f.o[1] + f.a[1] + f.b[1]}};
            for (float[] p : pts) { minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]); minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]); }
        }
        float sc = Math.min((size - 6) / (maxX - minX), (size - 6) / (maxY - minY));
        float ox = (size - (maxX - minX) * sc) / 2 - minX * sc, oy = (size - (maxY - minY) * sc) / 2 - minY * sc;
        faces.sort(Comparator.comparingDouble(Face::depth));
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (Face f : faces) {
            float x0 = f.o[0] * sc + ox, y0 = f.o[1] * sc + oy;
            Previews.face(g, f.img, x0, y0, x0 + f.a[0] * sc, y0 + f.a[1] * sc, x0 + f.b[0] * sc, y0 + f.b[1] * sc);
        }
        g.dispose();
        return out;
    }

    private Knives() {}
}
