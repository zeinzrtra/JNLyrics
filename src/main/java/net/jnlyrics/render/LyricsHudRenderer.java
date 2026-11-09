package net.jnlyrics.render;

import net.jnlyrics.config.JNConfig;
import net.jnlyrics.media.MediaManager;
import net.jnlyrics.media.TrackInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class LyricsHudRenderer {
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        JNConfig config = JNConfig.getInstance();
        if (!config.enabled || !config.hudMode) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (mc.gui != null && mc.gui.hud != null && mc.gui.hud.isHidden())) return;

        TrackInfo track = MediaManager.getInstance().getTrack();
        if (track == null || !track.isValidTrack()) return;

        Font font = mc.font;
        int screenWidth = graphics.guiWidth();
        int centerX = screenWidth / 2;

        // Header: Only Title - Artist (No lyrics in HUD)
        String songTitle = track.getArtist().isEmpty()
                ? "§a♪ §f" + track.getTitle()
                : "§a♪ §f" + track.getTitle() + " §7- " + track.getArtist();
        int textWidth = font.width(songTitle);

        // Compact sleek pill dimensions
        int pillWidth = Math.max(130, Math.min(textWidth + 20, screenWidth - 20));
        int pillHeight = 17;
        int pillX = centerX - pillWidth / 2;
        int pillY = 5;

        // Translucent dark glass background & subtle crisp border
        graphics.fill(pillX, pillY, pillX + pillWidth, pillY + pillHeight, 0x90000000);
        graphics.outline(pillX, pillY, pillWidth, pillHeight, 0x35FFFFFF);

        // Title - Artist centered
        graphics.centeredText(font, songTitle, centerX, pillY + 3, 0xFFFFFFFF);

        // Real-time sleek progress bar at the bottom of the pill
        long cur = track.getEstimatedPositionMs();
        long dur = track.getDurationMs();
        float progress = dur > 0 ? Math.min(1.0f, Math.max(0.0f, (float) cur / dur)) : 0.0f;

        int barX = pillX + 6;
        int barY = pillY + pillHeight - 3;
        int barW = pillWidth - 12;

        // Background track (subtle gray)
        graphics.fill(barX, barY, barX + barW, barY + 2, 0x40FFFFFF);

        // Glowing filled progress bar
        int fillW = (int) (barW * progress);
        if (fillW > 0) {
            int barColor = (config.glowColor != 0) ? (config.glowColor | 0xFF000000) : 0xFF00E5FF;
            graphics.fill(barX, barY, barX + fillW, barY + 2, barColor);
        }
    }
}
