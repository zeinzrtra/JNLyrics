package net.jnlyrics.lyrics;

import net.jnlyrics.JNLyricsMod;
import net.jnlyrics.config.JNConfig;
import net.jnlyrics.media.TrackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LyricsManager {
    private static LyricsManager instance;
    private final List<LyricLine> currentLyrics = Collections.synchronizedList(new ArrayList<>());
    private volatile String currentTrackKey = "";
    private volatile String currentArtist = "";
    private volatile String currentTitle = "";
    private volatile boolean loading = false;
    private volatile boolean fetchFailed = false;

    public static synchronized LyricsManager getInstance() {
        if (instance == null) {
            instance = new LyricsManager();
        }
        return instance;
    }

    public void updateTrack(TrackInfo track) {
        if (track == null || !track.isValidTrack()) {
            currentLyrics.clear();
            currentTrackKey = "";
            currentArtist = "";
            currentTitle = "";
            loading = false;
            fetchFailed = false;
            return;
        }

        String key = track.getTrackKey();
        if (key.equals(currentTrackKey)) {
            return;
        }

        currentTrackKey = key;
        currentArtist = track.getArtist();
        currentTitle = track.getTitle();
        currentLyrics.clear();
        loading = true;
        fetchFailed = false;
        JNLyricsMod.LOGGER.info("[JNLyrics] Mengambil lirik dari LRCLIB: {} - {}", track.getArtist(), track.getTitle());

        double durationSec = track.getDurationMs() / 1000.0;
        LrclibClient.fetchSyncedLyrics(track.getTitle(), track.getArtist(), track.getAlbum(), durationSec)
                .thenAccept(result -> {
                    if (key.equals(currentTrackKey)) {
                        loading = false;
                        if (result != null && result.hasLyrics()) {
                            // Standard LRC timestamps are parsed with adaptive duration scaling if needed
                            List<LyricLine> parsed = LrcParser.parse(result.syncedLyrics(), 0, result.lrcDurationSec(), durationSec);
                            currentLyrics.clear();
                            currentLyrics.addAll(parsed);
                            fetchFailed = currentLyrics.isEmpty();
                            JNLyricsMod.LOGGER.info("[JNLyrics] Sukses memuat {} baris lirik untuk lagu: {}", currentLyrics.size(), track.getTitle());
                        } else {
                            fetchFailed = true;
                            JNLyricsMod.LOGGER.warn("[JNLyrics] Lirik tersinkronisasi tidak ditemukan di LRCLIB untuk: {}", track.getTitle());
                        }
                    }
                })
                .exceptionally(ex -> {
                    if (key.equals(currentTrackKey)) {
                        loading = false;
                        fetchFailed = true;
                        JNLyricsMod.LOGGER.warn("[JNLyrics] Gagal menghubungi LRCLIB: {}", ex.getMessage());
                    }
                    return null;
                });
    }

    public int getCurrentEffectiveOffsetMs() {
        return JNConfig.getInstance().syncOffsetMs;
    }

    public void reloadCurrentTrack() {
        currentTrackKey = "";
        currentLyrics.clear();
        TrackInfo track = net.jnlyrics.media.MediaManager.getInstance().getTrack();
        if (track != null && track.isValidTrack()) {
            updateTrack(track);
        }
    }

    public LyricLine getCurrentLine(long positionMs) {
        if (currentLyrics.isEmpty()) return null;
        LyricLine best = null;
        LyricLine next = null;

        synchronized (currentLyrics) {
            for (LyricLine line : currentLyrics) {
                if (line.timestampMs() <= positionMs) {
                    best = line;
                } else {
                    next = line;
                    break;
                }
            }
        }

        if (best == null || best.text().isBlank()) {
            return null;
        }

        // Outro fadeout: if this was the last line of the song, hide after 8s
        if (next == null) {
            long elapsedOnLine = positionMs - best.timestampMs();
            if (elapsedOnLine > 8000) {
                return null;
            }
        }

        return best;
    }

    public LyricLine getNextLine(long positionMs) {
        if (currentLyrics.isEmpty()) return null;
        synchronized (currentLyrics) {
            for (LyricLine line : currentLyrics) {
                if (line.timestampMs() > positionMs) {
                    return line;
                }
            }
        }
        return null;
    }

    public boolean hasLyrics() {
        return !currentLyrics.isEmpty();
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isFetchFailed() {
        return fetchFailed;
    }
}
