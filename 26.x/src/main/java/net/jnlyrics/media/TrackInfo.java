package net.jnlyrics.media;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackInfo {
    private static final Pattern NOISE_PATTERN = Pattern.compile(
            "(?i)\\s*([(\\[]?(?:official\\s+(?:music\\s+|lyric\\s+)?(?:video|audio)|music\\s+video|lyric\\s+video|lyrics|official|audio|video|visualizer|clip|video\\s+clip|mv|m/v|hd|hq|4k|live(?:\\s+performance)?|remastered|remaster|deluxe|version|bonus\\s+track|feat\\.?|ft\\.?)[^)|\\]]*[)\\]]?)+|\\s*[-|–—]\\s*(?:official\\s+(?:music\\s+|lyric\\s+)?(?:video|audio)|music\\s+video|lyric\\s+video|lyrics|official|audio|video|visualizer|clip|video\\s+clip|mv|m/v|hd|hq|4k|live|remastered).*"
    );

    private static final Pattern JP_BRACKET_PATTERN = Pattern.compile("^(.+?)[「『]([^」』]+)[」』]");

    private final String status; // Playing, Paused, NONE
    private final String title;
    private final String artist;
    private final String album;
    private final String rawTitle;
    private final String rawArtist;
    private final String sourceApp;
    private final long positionMs;
    private final long durationMs;
    private final long receiptNanoTime;

    public TrackInfo(String status, String rawTitle, String rawArtist, String album, String sourceApp, long positionMs, long durationMs) {
        this.status = status != null ? status : "NONE";
        this.rawTitle = rawTitle != null ? rawTitle.trim() : "";
        this.rawArtist = rawArtist != null ? rawArtist.trim() : "";
        this.album = album != null ? album.trim() : "";
        this.sourceApp = sourceApp != null ? sourceApp.trim() : "";
        this.positionMs = Math.max(0, positionMs);
        this.durationMs = Math.max(0, durationMs);
        this.receiptNanoTime = System.nanoTime();

        String[] parsed = parseMetadata(this.rawTitle, this.rawArtist, this.sourceApp);
        this.title = parsed[0];
        this.artist = parsed[1];
    }

    public TrackInfo(String status, String rawTitle, String rawArtist, String album, long positionMs, long durationMs) {
        this(status, rawTitle, rawArtist, album, "", positionMs, durationMs);
    }

    public static String cleanNoise(String s) {
        if (s == null) return "";
        String cleaned = NOISE_PATTERN.matcher(s).replaceAll("").trim();
        return cleaned.isEmpty() ? s.trim() : cleaned;
    }

    public static boolean isBrowserApp(String app) {
        if (app == null) return false;
        String s = app.toLowerCase();
        return s.contains("chrome") || s.contains("edge") || s.contains("firefox")
                || s.contains("brave") || s.contains("opera") || s.contains("vivaldi")
                || s.contains("browser") || s.contains("arc");
    }

    public static String[] parseMetadata(String rawTitle, String rawArtist, String sourceApp) {
        String t = rawTitle != null ? rawTitle.trim() : "";
        String a = rawArtist != null ? rawArtist.trim() : "";
        if (t.isEmpty()) {
            return new String[]{ "", a };
        }

        boolean isBrowser = isBrowserApp(sourceApp);

        // If it's a browser (e.g. YouTube in Chrome/Edge), or artist is empty / channel-like:
        if (isBrowser || a.isEmpty()) {
            // 1. Japanese bracket syntax: Artist「Title」
            Matcher mJp = JP_BRACKET_PATTERN.matcher(t);
            if (mJp.find()) {
                String artistExtracted = cleanNoise(mJp.group(1));
                String titleExtracted = cleanNoise(mJp.group(2));
                if (!artistExtracted.isEmpty() && !titleExtracted.isEmpty()) {
                    return new String[]{ titleExtracted, artistExtracted };
                }
            }

            // 2. Delimiter syntax: Artist - Title or Artist | Title
            String[] parts = t.split("\\s+[-–—|]\\s+", 2);
            if (parts.length == 2) {
                String candidateArtist = cleanNoise(parts[0]);
                String candidateTitle = cleanNoise(parts[1]);
                if (candidateTitle.startsWith("\"") && candidateTitle.endsWith("\"") && candidateTitle.length() > 2) {
                    candidateTitle = candidateTitle.substring(1, candidateTitle.length() - 1).trim();
                }
                if (candidateTitle.startsWith("“") && candidateTitle.endsWith("”") && candidateTitle.length() > 2) {
                    candidateTitle = candidateTitle.substring(1, candidateTitle.length() - 1).trim();
                }
                if (!candidateArtist.isEmpty() && !candidateTitle.isEmpty()) {
                    return new String[]{ candidateTitle, candidateArtist };
                }
            }

            // 3. Fallback when no delimiter: clean noise from title, keep artist
            return new String[]{ cleanNoise(t), a };
        }

        // For non-browser (e.g. Spotify): artist is already accurate
        return new String[]{ cleanNoise(t), a };
    }

    public boolean isPlaying() {
        return "Playing".equalsIgnoreCase(status);
    }

    public boolean isValidTrack() {
        return (!title.isEmpty() || !rawTitle.isEmpty()) && !"NONE".equalsIgnoreCase(status);
    }

    public long getEstimatedPositionMs() {
        int customOffset = net.jnlyrics.lyrics.LyricsManager.getInstance().getCurrentEffectiveOffsetMs();
        if (!isPlaying()) {
            return Math.max(0, positionMs + customOffset);
        }
        long elapsedMs = (System.nanoTime() - receiptNanoTime) / 1_000_000L;
        long estimated = positionMs + elapsedMs + customOffset;
        if (estimated < 0) {
            return 0;
        }
        if (durationMs > 0 && estimated > durationMs) {
            return durationMs;
        }
        return estimated;
    }

    public String getTrackKey() {
        return (artist.toLowerCase() + " - " + title.toLowerCase()).trim();
    }

    public String getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getRawTitle() {
        return rawTitle;
    }

    public String getRawArtist() {
        return rawArtist;
    }

    public String getSourceApp() {
        return sourceApp;
    }

    public String getAlbum() {
        return album;
    }

    public long getDurationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        return artist + " - " + title + " (" + (getEstimatedPositionMs() / 1000) + "s / " + (durationMs / 1000) + "s) [" + status + "]";
    }
}
