package dev.hugoservice;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HugoService implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("HugoService");
    public static KeyBinding OPEN, INSPECT;

    @Override
    public void onInitializeClient() {
        Config.load();
        dev.hugoservice.cosmetics.CosmeticCatalog.loadOwnCapes(Config.CAPES);
        Fx.init();
        dev.hugoservice.cosmetics.CosmeticSync.init();
        dev.hugoservice.cosmetics.CosmeticRendering.init();
        SpotifyHud.init();
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("hugoservice", "main"));
        OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hugoservice.open", InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        INSPECT = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hugoservice.inspect", InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_G, category));
        // index resource packs once in the background so the first menu open is fast
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MediaWatcher.stop());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            Thread t = new Thread(Styles::rescan, "HugoService-Scan");
            t.setDaemon(true);
            t.start();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new StyleScreen());
            }
            boolean inspect = false;
            while (INSPECT.wasPressed()) inspect = true;
            KnifeAnimations.tick(client, inspect);
            Spotify.tick();
        });
    }
}
