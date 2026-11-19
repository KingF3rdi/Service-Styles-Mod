package dev.hugoservice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Extra FPS / explosion culling. Vanilla mixins only ({@code require = 0}), no Sodium/Vulkan class
 * references, so those mods keep their own renderers and a missed inject cannot crash the game.
 */
public final class Perf {
    private static int explosionKeep;

    public static boolean skipParticle(ParticleEffect type, double x, double y, double z) {
        try {
            Config cfg = Config.INSTANCE;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) {
                return false;
            }
            boolean explosion = type != null && isExplosion(type);
            if (cfg.explosionOptimize > 0 && explosion && type != null) {
                if (isExplosionEmitter(type) && cfg.explosionOptimize >= 1) {
                    return true;
                }
                double distSq = mc.player.squaredDistanceTo(x, y, z);
                int max = cfg.explosionOptimize >= 2 ? 24 : 48;
                if (distSq > (double) max * (double) max) {
                    return true;
                }
                if (cfg.explosionOptimize >= 2 && (explosionKeep++ & 1) == 0) {
                    return true;
                }
            }
            if (cfg.fpsCulling) {
                double max = Math.max(16, cfg.cullDistance);
                if (mc.player.squaredDistanceTo(x, y, z) > max * max) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean skipEntity(Entity entity) {
        try {
            if (!Config.INSTANCE.fpsCulling || entity == null) {
                return false;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || entity == mc.player) {
                return false;
            }
            if (mc.world == null || mc.world.getEntityById(entity.getId()) != entity) {
                return false;
            }
            double max = Math.max(16, Config.INSTANCE.cullDistance);
            return entity.squaredDistanceTo((Entity) mc.player) > max * max;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean isExplosion(ParticleEffect type) {
        if (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER) {
            return true;
        }
        try {
            Identifier id = Registries.PARTICLE_TYPE.getId(type.getType());
            if (id == null) {
                return false;
            }
            String path = id.getPath();
            return path.contains("explosion") || path.contains("gust_emitter");
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean isExplosionEmitter(ParticleEffect type) {
        if (type == ParticleTypes.EXPLOSION_EMITTER) {
            return true;
        }
        try {
            Identifier id = Registries.PARTICLE_TYPE.getId(type.getType());
            return id != null && id.getPath().contains("emitter") && isExplosion(type);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Perf() {
    }
}
