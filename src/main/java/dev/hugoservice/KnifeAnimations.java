package dev.hugoservice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.RotationAxis;

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

    private static final Clip DRAW_SPIN = new Clip(600, List.of(
        k(0, 0, -0.35f, 0, -360, 0, 0), k(0.75f, 0, 0.03f, 0, 10, 0, 0), k(1, 0, 0, 0, 0, 0, 0)));
    private static final Clip DRAW = new Clip(550, List.of(
        k(0, 0, -0.32f, 0, -85, 0, 0), k(0.7f, 0, 0.02f, 0, 8, 0, 0), k(1, 0, 0, 0, 0, 0, 0)));

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
        if (slot != lastSlot && lastSlot != -1 && holdingKnife(mc)) { drawStart = System.currentTimeMillis(); inspectStart = -1; }
        lastSlot = slot;
        if (inspectPressed && holdingKnife(mc)) startInspect();
        if (mc.player.handSwinging) inspectStart = -1; // attacking cancels the show
    }

    public static void startInspect() {
        inspectStart = System.currentTimeMillis();
        drawStart = -1;
    }

    /** Extra movement for the main-hand item, applied right before it is drawn. */
    public static void apply(MatrixStack m, ItemStack stack, boolean mainHand) {
        Knives.Model knife = Knives.active();
        if (!mainHand || knife == null || !stack.isIn(ItemTags.SWORDS)) return;
        long now = System.currentTimeMillis();
        K pose = null;
        if (drawStart >= 0) {
            Clip c = knife.anim() == Knives.Anim.SPIN || knife.anim() == Knives.Anim.FLIP ? DRAW_SPIN : DRAW;
            float t = (now - drawStart) / (float) c.ms();
            if (t >= 1) drawStart = -1; else pose = sample(c, t);
        } else if (inspectStart >= 0) {
            Clip c = INSPECT.get(knife.anim());
            float t = (now - inspectStart) / (float) c.ms();
            if (t >= 1) inspectStart = -1; else pose = sample(c, t);
        }
        if (pose == null) { // idle: the knife breathes a little in the hand
            double s = now / 1000.0;
            pose = k(0, 0, (float) Math.sin(s * 1.7) * 0.006f, 0, (float) Math.sin(s * 1.1) * 1.2f, 0, 0);
        }
        m.translate(pose.tx(), pose.ty(), pose.tz());
        pivot(m, RotationAxis.POSITIVE_Y, pose.ry(), CX, CY, CZ);
        pivot(m, RotationAxis.POSITIVE_X, pose.rx(), GX, GY, GZ);
        pivot(m, RotationAxis.POSITIVE_Z, pose.rz(), CX, CY, CZ);
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
