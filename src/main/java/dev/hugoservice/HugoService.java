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
    public static KeyBinding OPEN;

    @Override
    public void onInitializeClient() {
        Config.load();
        Fx.init();
        dev.hugoservice.cosmetics.CosmeticSync.init();
        dev.hugoservice.cosmetics.CosmeticRendering.init();
        OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hugoservice.open", InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.create(Identifier.of("hugoservice", "main"))));
        // index resource packs once in the background so the first menu open is fast
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            Thread t = new Thread(Styles::rescan, "HugoService-Scan");
            t.setDaemon(true);
            t.start();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new StyleScreen());
            }
        });
    }
}
