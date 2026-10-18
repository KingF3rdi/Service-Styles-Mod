package dev.hugoservice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.RotationAxis;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * First-person knife animations, built from keyframes: an inspect move per knife type (inspect key), a draw move when
 * you switch to the knife, and a slight idle sway. Only the view of your own hand changes, nothing goes to the server.
 */
public final class KnifeAnimations {
    /**
     * One pose: hand offset (tx, ty, tz) and turns in degrees. rx spins the knife in its own plane around the grip,
     * ry turns its flat side towards you, rz tilts it; both around the middle of the knife.
     */
    private record K(float t, float tx, float ty, float tz, float rx, float ry, float rz) {}

    private record Clip(long ms, List<K> keys) {}

    private static K k(float t, float tx, float ty, float tz, float rx, float ry, float rz) {
        return new K(t, tx, ty, tz, rx, ry, rz);
    }

    private static final K REST = k(0, 0, 0, 0, 0, 0, 0);

    private static final Map<Knives.Anim, Clip> INSPECT = Map.of(
        // karambit / talon: up into view, two spins round the finger ring, show the side, a last flick
        Knives.Anim.SPIN, new Clip(2400, List.of(REST,
            k(0.12f, -0.16f, 0.26f, 0, 0, 0, -8),
            k(0.42f, -0.16f, 0.26f, 0, 720, 0, -8),
            k(0.58f, -0.18f, 0.28f, 0.02f, 720, -75, -8),
            k(0.74f, -0.18f, 0.28f, 0.02f, 745, -80, -4),
            k(0.86f, -0.12f, 0.2f, 0, 1080, -20, 0),
            k(1, 0, 0, 0, 1080, 0, 0))),
        // butterfly: alternating flips over both axes
        Knives.Anim.FLIP, new Clip(2200, List.of(REST,
            k(0.1f, -0.14f, 0.22f, 0, 0, 0, 0),
            k(0.27f, -0.14f, 0.22f, 0, 0, 360, 0),
            k(0.44f, -0.14f, 0.25f, 0, 360, 360, 0),
            k(0.6f, -0.14f, 0.25f, 0, 360, 720, 0),
            k(0.76f, -0.14f, 0.25f, 0, 720, 720, 0),
            k(0.9f, -0.08f, 0.12f, 0, 720, 740, 0),
            k(1, 0, 0, 0, 720, 720, 0))),
        // bayonet / M9: show one side, turn it over, a quick spin in the hand
        Knives.Anim.TURN, new Clip(2700, List.of(REST,
            k(0.15f, -0.24f, 0.3f, 0.04f, 25, 60, 0),
            k(0.4f, -0.24f, 0.3f, 0.04f, 25, 72, 4),
            k(0.55f, -0.24f, 0.3f, 0.04f, 25, -80, -4),
            k(0.72f, -0.24f, 0.3f, 0.04f, 25, -90, 0),
            k(0.84f, -0.18f, 0.24f, 0.02f, 385, -40, 0),
            k(1, 0, 0, 0, 360, 0, 0))),
        // flip knife / stiletto: a fast flick spin, then show it off
        Knives.Anim.FLICK, new Clip(2000, List.of(REST,
            k(0.1f, -0.12f, 0.18f, 0, -25, 0, 0),
            k(0.32f, -0.12f, 0.22f, 0, 340, 0, 0),
            k(0.42f, -0.16f, 0.24f, 0.02f, 360, 50, 0),
            k(0.75f, -0.16f, 0.24f, 0.02f, 360, 62, 10),
            k(1, 0, 0, 0, 360, 0, 0))),
        // bowie / huntsman: weigh it, toss it up with a spin and catch it
        Knives.Anim.HEAVY, new Clip(2700, List.of(REST,
            k(0.15f, -0.2f, 0.24f, 0.03f, -30, 70, 0),
            k(0.38f, -0.2f, 0.26f, 0.03f, -30, 80, 0),
            k(0.5f, -0.2f, 0.55f, 0.03f, 150, 80, 0),
            k(0.6f, -0.2f, 0.28f, 0.03f, 330, 80, 0),
            k(0.66f, -0.2f, 0.24f, 0.03f, 330, 80, 0),
            k(0.82f, -0.2f, 0.26f, 0.03f, 330, -60, 0),
            k(1, 0, 0, 0, 360, 0, 0))),
        // gut knife: twirl over the side and around the grip
        Knives.Anim.TWIRL, new Clip(2200, List.of(REST,
            k(0.12f, -0.14f, 0.22f, 0, 0, 0, 0),
            k(0.36f, -0.14f, 0.22f, 0, 0, 0, 360),
            k(0.6f, -0.14f, 0.22f, 0, 0, 60, 360),
            k(0.85f, -0.1f, 0.15f, 0, 360, 0, 360),
            k(1, 0, 0, 0, 360, 0, 360))),
        // push dagger: two jabs and a turn
        Knives.Anim.PUSH, new Clip(1700, List.of(REST,
            k(0.15f, -0.1f, 0.18f, 0, 0, 40, 0),
            k(0.27f, -0.1f, 0.18f, -0.25f, 0, 40, 0),
            k(0.38f, -0.1f, 0.18f, 0, 0, 40, 0),
            k(0.5f, -0.1f, 0.18f, -0.25f, 0, 40, 0),
            k(0.66f, -0.1f, 0.18f, 0, 0, -45, 0),
            k(1, 0, 0, 0, 0, 0, 0)))
    );

    /** Tricks when you pull the knife out: two per type, played in turn. They start below the view. */
    private static final Map<Knives.Anim, List<Clip>> DRAWS = Map.of(
        Knives.Anim.SPIN, List.of(
            // ring spin: two turns round the finger while it comes up, a look at the side, settle
            new Clip(1300, List.of(k(0, 0, -0.35f, 0, 0, 0, 0),
                k(0.5f, -0.2f, 0.3f, 0, 720, 0, 0),
                k(0.72f, -0.2f, 0.32f, 0, 720, -55, -6),
                k(1, 0, 0, 0, 720, 0, 0))),
            // reverse spin with a flick at the end
            new Clip(1250, List.of(k(0, 0, -0.35f, 0, 0, 0, 0),
                k(0.35f, -0.18f, 0.3f, 0, -540, 0, 0),
                k(0.55f, -0.2f, 0.34f, 0, -720, 0, 10),
                k(0.72f, -0.2f, 0.32f, 0, -720, -40, 0),
                k(1, 0, 0, 0, -720, 0, 0)))),
        Knives.Anim.FLIP, List.of(
            // balisong opening: flips over both axes in a row
            new Clip(1300, List.of(k(0, 0, -0.35f, 0, 0, 0, 0),
                k(0.2f, -0.18f, 0.2f, 0, 0, 180, 0),
                k(0.38f, -0.2f, 0.3f, 0, 180, 360, 0),
                k(0.56f, -0.2f, 0.32f, 0, 360, 540, 0),
                k(0.74f, -0.2f, 0.32f, 0, 360, 720, 0),
                k(1, 0, 0, 0, 360, 720, 0))),
            // aerial: tossed up flipping, caught
            new Clip(1300, List.of(k(0, 0, -0.35f, 0, 0, 0, 0),
                k(0.25f, -0.18f, 0.28f, 0, 360, 0, 0),
                k(0.45f, -0.2f, 0.52f, 0, 540, 180, 0),
                k(0.62f, -0.2f, 0.32f, 0, 720, 360, 0),
                k(1, 0, 0, 0, 720, 360, 0)))),
        Knives.Anim.TURN, List.of(
            // toss and catch: thrown up spinning backwards
            new Clip(1300, List.of(k(0, 0, -0.35f, 0, 0, 0, 0),
                k(0.3f, -0.2f, 0.52f, 0, -360, 0, 0),
                k(0.5f, -0.2f, 0.34f, 0, -720, 0, 0),
                k(0.7f, -0.2f, 0.34f, 0.02f, -720, 50, 0),
                k(1, 0, 0, 0, -720, 0, 0))),
            // comes up blade first and spins upright, then shows the side
            new Clip(1200, List.of(k(0, 0, -0.35f, 0, -540, 0, 0),
                k(0.45f, -0.22f, 0.32f, 0, 0, 0, 0),
                k(0.68f, -0.22f, 0.34f, 0, 0, 70, 0),
                k(1, 0, 0, 0, 0, 0, 0)))),
        Knives.Anim.FLICK, List.of(
            // flick open with an overshoot, then one fast spin
            new Clip(1150, List.of(k(0, 0, -0.35f, 0, -200, 0, 0),
                k(0.35f, -0.16f, 0.22f, 0, 20, 0, 0),
                k(0.5f, -0.18f, 0.24f, 0, 0, 30, 0),
                k(0.75f, -0.18f, 0.26f, 0, 360, 30, 0),
                k(1, 0, 0, 0, 360, 0, 0))),
            // double spin
            new Clip(1100, List.of(k(0, 0, -0.35f, 0, 0, 0, 0),
                k(0.55f, -0.18f, 0.28f, 0, 720, 0, 0),
                k(0.75f, -0.18f, 0.28f, 0, 720, 40, 8),
                k(1, 0, 0, 0, 720, 0, 0)))),
        Knives.Anim.HEAVY, List.of(
            // flipped over while it comes up, then a heavy turn
            new Clip(1350, List.of(k(0, 0, -0.35f, 0, 0, 180, 0),
                k(0.4f, -0.22f, 0.3f, 0, 360, 180, 0),
                k(0.65f, -0.22f, 0.32f, 0, 360, 360, 0),
                k(1, 0, 0, 0, 360, 360, 0))),
            // high toss with two turns and a catch
            new Clip(1400, List.of(k(0, 0, -0.35f, 0, 0, 0, 0),
                k(0.25f, -0.2f, 0.3f, 0, 0, 0, 0),
                k(0.45f, -0.2f, 0.6f, 0, 360, 0, 0),
                k(0.6f, -0.2f, 0.34f, 0, 720, 0, 0),
                k(0.8f, -0.2f, 0.32f, 0, 720, 60, 0),
                k(1, 0, 0, 0, 720, 0, 0)))),
        Knives.Anim.TWIRL, List.of(
            new Clip(1250, List.of(k(0, 0, -0.35f, 0, 0, 0, 0),
                k(0.45f, -0.2f, 0.3f, 0, 0, 0, 720),
                k(0.7f, -0.2f, 0.32f, 0, 360, 0, 720),
                k(1, 0, 0, 0, 360, 0, 720))),
            new Clip(1150, List.of(k(0, 0, -0.35f, 0, -360, 0, 0),
                k(0.5f, -0.2f, 0.3f, 0, 0, 0, 360),
                k(1, 0, 0, 0, 0, 0, 360)))),
        Knives.Anim.PUSH, List.of(
            // punched out of the sleeve, a second jab
            new Clip(1100, List.of(k(0, 0, -0.35f, 0.2f, 0, -90, 0),
                k(0.35f, -0.14f, 0.2f, -0.22f, 0, 0, 0),
                k(0.55f, -0.14f, 0.2f, 0, 0, 0, 0),
                k(0.75f, -0.14f, 0.2f, -0.15f, 0, 0, 0),
                k(1, 0, 0, 0, 0, 0, 0))),
            // spun out and jabbed
            new Clip(1150, List.of(k(0, 0, -0.35f, 0, 0, 0, 0),
                k(0.5f, -0.16f, 0.25f, 0, 0, 360, 0),
                k(0.7f, -0.16f, 0.25f, -0.18f, 0, 360, 0),
                k(1, 0, 0, 0, 0, 360, 0))))
    );

    /** Short pull for when tricks are off. */
    private static final Clip DRAW_PLAIN = new Clip(450, List.of(
        k(0, 0, -0.32f, 0, -85, 0, 0), k(0.7f, 0, 0.02f, 0, 8, 0, 0), k(1, 0, 0, 0, 0, 0, 0)));

    private static Clip drawClip = DRAW_PLAIN;
    private static int drawCount, drawVariant;

    // grip and middle of the knife in this space, worked out from the knife display transform
    private static final float GX = 0.07f, GY = 0.01f, GZ = 0f, CX = 0.07f, CY = 0.22f, CZ = 0.07f;

    private static long inspectStart = -1, drawStart = -1;
    private static int lastSlot = -1;

    static boolean holdingKnife(MinecraftClient mc) {
        return mc.player != null && Knives.active() != null && mc.player.getMainHandStack().isIn(ItemTags.SWORDS);
    }

    /** Called every client tick with whether the inspect key was pressed. */
    static void tick(MinecraftClient mc, boolean inspectPressed) {
        if (mc.player == null) { lastSlot = -1; return; }
        int slot = mc.player.getInventory().getSelectedSlot();
        if (slot != lastSlot && lastSlot != -1 && holdingKnife(mc)) startDraw();
        lastSlot = slot;
        if (inspectPressed && holdingKnife(mc)) startInspect();
        if (mc.player.handSwinging) inspectStart = -1; // attacking cancels the show
    }

    /** Pull-out move: the next trick of this knife type, or the short pull when tricks are off. */
    public static void startDraw() {
        Knives.Model knife = Knives.active();
        if (knife == null) return;
        List<Clip> tricks = DRAWS.get(knife.anim());
        drawVariant = drawCount++ % 2;
        drawClip = Config.INSTANCE.knifeTricks && tricks != null ? tricks.get(drawVariant % tricks.size()) : DRAW_PLAIN;
        drawStart = System.currentTimeMillis();
        inspectStart = -1;
    }

    public static void startInspect() {
        inspectStart = System.currentTimeMillis();
        drawStart = -1;
    }

    /** Extra movement for the main-hand item, applied right before it is drawn. */
    public static void apply(MatrixStack m, ItemStack stack, boolean mainHand, float equipProgress) {
        Knives.Model knife = Knives.active();
        if (!mainHand || knife == null || !stack.isIn(ItemTags.SWORDS)) return;
        long now = System.currentTimeMillis();
        K pose = null;
        if (drawStart >= 0) {
            float t = (now - drawStart) / (float) drawClip.ms();
            if (t >= 1) drawStart = -1; else pose = sample(drawClip, t);
            // the trick brings the knife up itself, so undo vanilla's lowering while switching (applyEquipOffset)
            if (pose != null && drawClip != DRAW_PLAIN) m.translate(0, equipProgress * 0.6f, 0);
        } else if (inspectStart >= 0) {
            Clip c = INSPECT.get(knife.anim());
            float t = (now - inspectStart) / (float) c.ms();
            if (t >= 1) inspectStart = -1; else pose = sample(c, t);
        }
        if (pose == null && !Config.INSTANCE.knifeSway) return;
        if (pose == null) { // idle: the knife breathes a little in the hand
            double s = now / 1000.0;
            pose = k(0, 0, (float) Math.sin(s * 1.7) * 0.006f, 0, (float) Math.sin(s * 1.1) * 1.2f, 0, 0);
        }
        m.translate(pose.tx(), pose.ty(), pose.tz());
        pivot(m, RotationAxis.POSITIVE_Y, pose.ry(), CX, CY, CZ);
        pivot(m, RotationAxis.POSITIVE_X, pose.rx(), GX, GY, GZ);
        pivot(m, RotationAxis.POSITIVE_Z, pose.rz(), CX, CY, CZ);
    }

    // ================================================================ butterfly tricks

    // first-person display of the knife models (see Knives DISPLAY): rotation [0, -90, -20], translation [1.13, 3.6, 1.13] px, scale 0.6
    private static final Quaternionf FP_ROT = new Quaternionf().rotationXYZ(0, (float) Math.toRadians(-90), (float) Math.toRadians(-20));
    private static final Vector3f FP_AXIS = FP_ROT.transform(new Vector3f(0, 0, 1)); // the knife's plane normal
    private static final ItemStack[][] PART_STACKS = new ItemStack[2][3];

    /** Model point (px) -> position in the space right before the item is drawn (undoes nothing, just maps it). */
    private static Vector3f beforeDisplay(float px, float py) {
        Vector3f p = new Vector3f(px / 16f - 0.5f, py / 16f - 0.5f, 0).mul(0.6f);
        return FP_ROT.transform(p).add(1.13f / 16, 3.6f / 16, 1.13f / 16);
    }

    /**
     * Angles (degrees) of the safe and bite handle around their pivots while a butterfly trick runs, else null.
     * 0 = open (handles down as you hold it), 180 = closed over the blade; whole turns are the classic flips.
     */
    private static float[] butterflyHandles() {
        long now = System.currentTimeMillis();
        if (drawStart >= 0 && drawClip != DRAW_PLAIN) {
            float t = (now - drawStart) / (float) drawClip.ms();
            if (drawVariant == 0) // opening: starts closed, swings each handle out, then a fan flip each
                return new float[] {180 + turn(t, 0.15f, 0.35f, 0.5f) + turn(t, 0.6f, 0.8f, 1),
                    -180 - turn(t, 0.35f, 0.55f, 0.5f) - turn(t, 0.7f, 0.9f, 1)};
            return new float[] {180 + turn(t, 0.25f, 0.5f, 1) + turn(t, 0.62f, 0.85f, 0.5f),   // aerial
                -180 - turn(t, 0.35f, 0.62f, 1) - turn(t, 0.7f, 0.9f, 0.5f)};
        }
        if (inspectStart >= 0) {
            float t = (now - inspectStart) / (float) INSPECT.get(Knives.Anim.FLIP).ms();
            return new float[] {turn(t, 0.12f, 0.3f, 1) + turn(t, 0.46f, 0.62f, 1) + turn(t, 0.8f, 0.92f, 1),
                -turn(t, 0.28f, 0.46f, 1) - turn(t, 0.62f, 0.8f, 1)};
        }
        return null;
    }

    /** {@code turns} full turns spread over [a, b] of the clip, eased. */
    private static float turn(float t, float a, float b, float turns) {
        return ease((t - a) / (b - a)) * 360 * turns;
    }

    /**
     * Draws the butterfly in its three parts with the handles swung, in place of the one-piece knife, while a trick runs
     * in your main hand. Returns false when the normal item should be drawn.
     */
    public static boolean renderButterfly(ItemStack stack, net.minecraft.item.ItemDisplayContext context, MatrixStack m,
                                          java.util.function.Consumer<ItemStack> draw) {
        Knives.Model knife = Knives.active();
        if (knife == null || !knife.id().equals("butterfly") || !stack.isIn(ItemTags.SWORDS)
            || context != net.minecraft.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) return false;
        float[] h = butterflyHandles();
        if (h == null) return false;
        int glint = stack.hasGlint() ? 1 : 0;
        draw.accept(part(0, glint));
        handle(m, Knives.BF_PIVOT_SAFE, h[0], () -> draw.accept(part(1, glint)));
        handle(m, Knives.BF_PIVOT_BITE, h[1], () -> draw.accept(part(2, glint)));
        return true;
    }

    private static void handle(MatrixStack m, float[] pivot, float degrees, Runnable draw) {
        Vector3f p = beforeDisplay(pivot[0], pivot[1]);
        m.push();
        m.translate(p.x, p.y, p.z);
        m.multiply(new Quaternionf().rotationAxis((float) Math.toRadians(degrees), FP_AXIS));
        m.translate(-p.x, -p.y, -p.z);
        draw.run();
        m.pop();
    }

    /** A local-only stack that renders one butterfly part (never sent anywhere). */
    private static ItemStack part(int i, int glint) {
        ItemStack s = PART_STACKS[glint][i];
        if (s == null) {
            s = new ItemStack(net.minecraft.item.Items.STICK);
            s.set(net.minecraft.component.DataComponentTypes.ITEM_MODEL, net.minecraft.util.Identifier.ofVanilla(Knives.BF_PARTS[i]));
            s.set(net.minecraft.component.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint == 1);
            PART_STACKS[glint][i] = s;
        }
        return s;
    }

    /** The pose at time t (0..1), eased between the two keyframes around it. */
    private static K sample(Clip c, float t) {
        List<K> keys = c.keys();
        for (int i = 1; i < keys.size(); i++) {
            K a = keys.get(i - 1), b = keys.get(i);
            if (t <= b.t()) {
                float f = ease((t - a.t()) / Math.max(1e-4f, b.t() - a.t()));
                return k(t, lerp(a.tx(), b.tx(), f), lerp(a.ty(), b.ty(), f), lerp(a.tz(), b.tz(), f),
                    lerp(a.rx(), b.rx(), f), lerp(a.ry(), b.ry(), f), lerp(a.rz(), b.rz(), f));
            }
        }
        return keys.getLast();
    }

    private static float lerp(float a, float b, float f) { return a + (b - a) * f; }

    private static void pivot(MatrixStack m, RotationAxis axis, float degrees, float x, float y, float z) {
        if (degrees == 0) return;
        m.translate(x, y, z);
        m.multiply(axis.rotationDegrees(degrees));
        m.translate(-x, -y, -z);
    }

    /** Smooth in and out. */
    private static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private KnifeAnimations() {}
}
