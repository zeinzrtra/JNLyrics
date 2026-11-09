package net.jnlyrics;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.jnlyrics.config.JNConfig;
import net.jnlyrics.gui.JNConfigScreen;
import net.jnlyrics.lyrics.LyricsManager;
import net.jnlyrics.media.MediaManager;
import net.jnlyrics.render.LyricsHudRenderer;
import net.jnlyrics.render.LyricsWorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JNLyricsMod implements ClientModInitializer {
    public static final String MOD_ID = "jnlyrics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding toggleKey;
    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[JNLyrics] Initializing JNLyrics Client (Fabric 1.21-modern)...");

        JNConfig.getInstance().load();
        MediaManager.getInstance();
        LyricsManager.getInstance();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.jnlyrics.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "key.category.jnlyrics.main"
        ));

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.jnlyrics.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "key.category.jnlyrics.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                JNConfig config = JNConfig.getInstance();
                config.enabled = !config.enabled;
                config.save();
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("§6[JNLyrics] §fMod " + (config.enabled ? "§aDiaktifkan" : "§cDinonaktifkan")),
                            true
                    );
                }
            }

            while (menuKey.wasPressed()) {
                client.setScreen(new JNConfigScreen(client.currentScreen));
            }
        });

        WorldRenderEvents.LAST.register(LyricsWorldRenderer::render);
        HudRenderCallback.EVENT.register(LyricsHudRenderer::render);

        LOGGER.info("[JNLyrics] JNLyrics initialized successfully!");
    }
}
