package net.jnlyrics.lyrics;

public record LyricsResult(String syncedLyrics, double lrcDurationSec) {
    public boolean hasLyrics() {
        return syncedLyrics != null && !syncedLyrics.isBlank();
    }
}

