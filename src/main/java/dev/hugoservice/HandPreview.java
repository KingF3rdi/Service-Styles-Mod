package dev.hugoservice;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** What the world shows while a menu slider is dragged: an item in hand, or a scene for the setting. */
public final class HandPreview {
    public enum Scene { NONE, FIRE, CRYSTALS, NAMETAG }

    private static volatile ItemStack stack;
    private static volatile Scene scene = Scene.NONE;
    private static final List<Integer> fakes = new ArrayList<>();
    private static Perspective before;

    private HandPreview() {
    }

    public static ItemStack stack() {
        return stack;
    }

    public static void show(ItemStack item) {
        stack = item;
    }

    public static Scene scene() {
        return scene;
    }

    /** FIRE sets you alight (EntityMixin), CRYSTALS places three in front of you, NAMETAG goes third person. */
    public static void scene(Scene s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (s == scene || mc.world == null || mc.player == null) {
            return;
        }
        endScene(mc);
        scene = s;
        if (s == Scene.CRYSTALS) {
            Vec3d at = Mc.pos(mc.player);
            Vec3d ahead = Vec3d.fromPolar(0.0f, mc.player.getYaw());
            Vec3d side = new Vec3d(-ahead.z, 0.0, ahead.x);
            for (int i = -1; i <= 1; ++i) {
                Vec3d c = at.add(ahead.multiply(5.0)).add(side.multiply(i * 2.4));
                EndCrystalEntity crystal = new EndCrystalEntity(mc.world, c.x, at.y, c.z);
                crystal.setShowBottom(false); // placed crystals have no bedrock base
                crystal.setId(-7_000_000 - i); // negative: never a server entity id
                mc.world.addEntity(crystal);
                fakes.add(crystal.getId());
            }
        } else if (s == Scene.NAMETAG) {
            before = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    public static void clear() {
        stack = null;
        endScene(MinecraftClient.getInstance());
    }

    private static void endScene(MinecraftClient mc) {
        if (before != null) {
            mc.options.setPerspective(before);
            before = null;
        }
        if (mc.world != null) {
            for (int id : fakes) {
                mc.world.removeEntity(id, Entity.RemovalReason.DISCARDED);
            }
        }
        fakes.clear();
        scene = Scene.NONE;
    }
}
