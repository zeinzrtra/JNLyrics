package net.jnlyrics;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.jnlyrics.config.JNConfig;
import net.jnlyrics.gui.JNConfigScreen;
import net.jnlyrics.media.MediaManager;
import net.jnlyrics.render.LyricsHudRenderer;
import net.jnlyrics.render.LyricsWorldRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JNLyricsMod implements ClientModInitializer {
    public static final String MOD_ID = "jnlyrics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping toggleKey;
    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[JNLyrics] Memulai mod lirik 3D Spotify (Dango & LyricsPlus Architecture)...");

        // 1. Load Configuration
        JNConfig.getInstance();

        // 2. Initialize Media Manager (Windows GSMTC Provider)
        MediaManager.getInstance();

        // 3. Register Keymappings (Clean & Modpack-Friendly: Only Menu 'M' & Toggle 'L')
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.jnlyrics.toggle",
                GLFW.GLFW_KEY_L,
                category
        ));

        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.jnlyrics.menu",
                GLFW.GLFW_KEY_M,
                category
        ));

        // 4. Register Client Tick Event for Key Presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.consumeClick()) {
                JNConfig cfg = JNConfig.getInstance();
                cfg.enabled = !cfg.enabled;
                cfg.save();

                if (client.player != null) {
                    client.player.sendOverlayMessage(
                            Component.translatable(cfg.enabled ? "jnlyrics.toast.toggled_on" : "jnlyrics.toast.toggled_off")
                    );
                }
            }

            if (menuKey.consumeClick()) {
                client.setScreenAndShow(new JNConfigScreen(null));
            }
        });

        // 5. Register 3D In-World Rendering Event
        LevelRenderEvents.COLLECT_SUBMITS.register(LyricsWorldRenderer::render);

        // 6. Register HUD Dynamic Island Overlay
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "hud_overlay"),
                LyricsHudRenderer::render
        );

        LOGGER.info("[JNLyrics] Mod berhasil diinisialisasi untuk Minecraft 26.2!");
    }
}
