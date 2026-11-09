package net.jnlyrics.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.jnlyrics.config.JNConfig;
import net.jnlyrics.lyrics.LyricLine;
import net.jnlyrics.lyrics.LyricsManager;
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

        long posMs = track.getEstimatedPositionMs();
        LyricsManager lyricsMgr = LyricsManager.getInstance();
        LyricLine currentLine = lyricsMgr.getCurrentLine(posMs);
        LyricLine nextLine = lyricsMgr.getNextLine(posMs);

        String lyricText = "";
        if (currentLine != null && !currentLine.text().isBlank()) {
            lyricText = currentLine.text().trim();
        } else if (lyricsMgr.isLoading()) {
            lyricText = "§7Memuat lirik...";
        } else if (lyricsMgr.hasLyrics() && nextLine != null && !nextLine.text().isBlank()) {
            lyricText = "§8♪ (Intro) §7" + nextLine.text().trim();
        }

        // Header: Title - Artist
        String songTitle = track.getArtist().isEmpty()
                ? "§a♪ §f" + track.getTitle()
                : "§a♪ §f" + track.getTitle() + " §7- " + track.getArtist();
        int titleWidth = font.getWidth(songTitle);
        int lyricWidth = lyricText.isEmpty() ? 0 : font.getWidth(lyricText);
        int maxContentWidth = Math.max(titleWidth, lyricWidth);

        // Sleek dynamic pill dimensions
        boolean hasLyricLine = !lyricText.isEmpty();
        int pillWidth = Math.max(140, Math.min(maxContentWidth + 24, screenWidth - 20));
        int pillHeight = hasLyricLine ? 29 : 17;
        int pillX = centerX - pillWidth / 2;
        int pillY = 5;

        // Translucent dark glass background & subtle crisp border
        context.fill(pillX, pillY, pillX + pillWidth, pillY + pillHeight, 0x90000000);
        context.drawBorder(pillX, pillY, pillWidth, pillHeight, 0x35FFFFFF);

        // Title - Artist centered
        context.drawCenteredTextWithShadow(font, songTitle, centerX, pillY + 3, 0xFFFFFFFF);

        // Active lyric line centered below title if available
        if (hasLyricLine) {
            int lyricColor = (config.glowColor != 0) ? (config.glowColor | 0xFF000000) : 0xFFFFD700;
            context.drawCenteredTextWithShadow(font, lyricText, centerX, pillY + 14, lyricColor);
        }

        // Real-time sleek progress bar at the bottom of the pill
        long cur = posMs;
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
