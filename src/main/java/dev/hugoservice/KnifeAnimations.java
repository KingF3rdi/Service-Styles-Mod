package dev.hugoservice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.RotationAxis;

/**
 * First-person knife animations: an inspect move per knife type (press the inspect key) and a short draw when you
 * switch to a knife. Only the view of your own hand changes, nothing is sent to the server.
 */
public final class KnifeAnimations {
    private static final long INSPECT_MS = 2400, SPIN_MS = 1500, DRAW_MS = 450;
    private static long inspectStart = -1, drawStart = -1;
    private static int lastSlot = -1;

    static boolean holdingKnife(MinecraftClient mc) {
        return mc.player != null && Knives.active() != null && mc.player.getMainHandStack().isIn(ItemTags.SWORDS);
    }

    /** Called every client tick with the inspect key's presses. */
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

    /** Extra rotation for the main-hand item, applied right before it is drawn. */
    public static void apply(MatrixStack m, ItemStack stack, boolean mainHand) {
        Knives.Model knife = Knives.active();
        if (!mainHand || knife == null || !stack.isIn(ItemTags.SWORDS)) return;
        long now = System.currentTimeMillis();
        if (drawStart >= 0) {
            float t = (now - drawStart) / (float) DRAW_MS;
            if (t >= 1) drawStart = -1;
            else draw(m, knife.anim(), t);
        }
        if (inspectStart >= 0) {
            long len = knife.anim() == Knives.Anim.TURN ? INSPECT_MS : SPIN_MS;
            float t = (now - inspectStart) / (float) len;
            if (t >= 1) inspectStart = -1;
            else inspect(m, knife.anim(), t);
        }
    }

    // Where the knife sits in this space (worked out from the knife display transform): its grip and its middle.
    // The knife lies in the plane across the X axis, so turning about X spins it in its own plane.
    private static final float GX = 0.07f, GY = 0.01f, GZ = 0f, CX = 0.07f, CY = 0.22f, CZ = 0.07f;

    private static void draw(MatrixStack m, Knives.Anim anim, float t) {
        float e = 1 - ease(t);
        if (anim == Knives.Anim.SPIN) {
            pivot(m, RotationAxis.POSITIVE_X, 360 * e, GX, GY, GZ);
        } else {
            m.translate(0, -0.2f * e, 0);
            pivot(m, RotationAxis.POSITIVE_X, -60 * e, GX, GY, GZ);
        }
    }

    private static void inspect(MatrixStack m, Knives.Anim anim, float t) {
        // the hand comes up towards the middle of the screen for the show and goes back down at the end
        float lift = ease(Math.min(1, t / 0.15f)) * (1 - ease(Math.max(0, (t - 0.85f) / 0.15f)));
        switch (anim) {
            case SPIN -> {                                                           // karambit: two turns round the ring
                m.translate(-0.16f * lift, 0.26f * lift, 0);
                pivot(m, RotationAxis.POSITIVE_X, 720 * ease(t), GX, GY, GZ);
            }
            case FLIP -> {                                                           // butterfly: flips and back
                m.translate(-0.16f * lift, 0.22f * lift, 0);
                pivot(m, RotationAxis.POSITIVE_Y, 720 * ease(t), CX, CY, CZ);
            }
            case TURN -> {                                                           // show one side, then the other
                float flip = ease(clamp((t - 0.45f) / 0.25f));
                m.translate(-0.24f * lift, 0.3f * lift, 0.04f * lift);
                pivot(m, RotationAxis.POSITIVE_Y, (60 - 140 * flip) * lift, CX, CY, CZ);
                pivot(m, RotationAxis.POSITIVE_X, 25 * lift, GX, GY, GZ);
            }
        }
    }

    private static void pivot(MatrixStack m, RotationAxis axis, float degrees, float x, float y, float z) {
        m.translate(x, y, z);
        m.multiply(axis.rotationDegrees(degrees));
        m.translate(-x, -y, -z);
    }

    private static float ease(float t) {
        t = clamp(t);
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    private static float clamp(float t) { return Math.max(0, Math.min(1, t)); }

    private KnifeAnimations() {}
}
