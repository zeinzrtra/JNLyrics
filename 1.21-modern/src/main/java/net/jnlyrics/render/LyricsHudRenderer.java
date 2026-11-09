package net.jnlyrics.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.jnlyrics.config.JNConfig;
import net.jnlyrics.media.MediaManager;
import net.jnlyrics.media.TrackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class LyricsHudRenderer {
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        JNConfig config = JNConfig.getInstance();
        if (!config.enabled || !config.hudMode) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        TrackInfo track = MediaManager.getInstance().getTrack();
        if (track == null || !track.isValidTrack()) return;

        TextRenderer font = mc.textRenderer;
        int screenWidth = context.getScaledWindowWidth();
        int centerX = screenWidth / 2;

        // Header: Only Title - Artist (No lyrics in HUD, exactly like 26.x)
        String songTitle = track.getArtist().isEmpty()
                ? "§a♪ §f" + track.getTitle()
                : "§a♪ §f" + track.getTitle() + " §7- " + track.getArtist();
        int textWidth = font.getWidth(songTitle);

        // Compact sleek pill dimensions
        int pillWidth = Math.max(130, Math.min(textWidth + 20, screenWidth - 20));
        int pillHeight = 17;
        int pillX = centerX - pillWidth / 2;
        int pillY = 5;

        // Translucent dark glass background & subtle crisp border
        context.fill(pillX, pillY, pillX + pillWidth, pillY + pillHeight, 0x90000000);
        context.drawBorder(pillX, pillY, pillWidth, pillHeight, 0x35FFFFFF);

        // Title - Artist centered
        context.drawCenteredTextWithShadow(font, songTitle, centerX, pillY + 3, 0xFFFFFFFF);

        // Real-time sleek progress bar at the bottom of the pill
        long cur = track.getEstimatedPositionMs();
        long dur = track.getDurationMs();
        float progress = dur > 0 ? Math.min(1.0f, Math.max(0.0f, (float) cur / dur)) : 0.0f;

        int barX = pillX + 6;
        int barY = pillY + pillHeight - 3;
        int barW = pillWidth - 12;

        // Background track (subtle gray)
        context.fill(barX, barY, barX + barW, barY + 2, 0x40FFFFFF);

        // Glowing filled progress bar
        int fillW = (int) (barW * progress);
        if (fillW > 0) {
            int barColor = (config.glowColor != 0) ? (config.glowColor | 0xFF000000) : 0xFF00E5FF;
            context.fill(barX, barY, barX + fillW, barY + 2, barColor);
        }
    }
}
