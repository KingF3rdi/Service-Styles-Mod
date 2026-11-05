package dev.hugoservice;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HugoService
implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger((String)"HugoService");
    public static KeyBinding OPEN;
    public static KeyBinding RESET_POPS;

    public void onInitializeClient() {
        Config.load();
        PackBuilder.dropOldKnifeFiles();
        Fx.init();
        SpotifyHud.init();
        TotemCounter.init();
        KillEffects.init();
        BlockRenderLayerMap.putBlock((Block)Blocks.RESPAWN_ANCHOR, (BlockRenderLayer)BlockRenderLayer.CUTOUT);
        Mc.registerKeys();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MediaWatcher.stop());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            BundledPacks.install();
            StylesCloud.refresh(true);
            Thread t = new Thread(Styles::rescan, "HugoService-Scan");
            t.setDaemon(true);
            t.start();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.wasPressed()) {
                if (client.currentScreen != null) continue;
                client.setScreen((Screen)new StyleMenu());
            }
            while (RESET_POPS.wasPressed()) {
                TotemCounter.reset();
                client.inGameHud.setOverlayMessage((Text)Text.literal((String)"Totem pops reset"), false);
            }
            Spotify.tick();
            Glint.tick(client);
        });
    }
}
