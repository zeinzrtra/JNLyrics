package net.jnlyrics.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.jnlyrics.JNLyricsMod;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrclibClient {
    private static final String API_BASE = "https://lrclib.net/api";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Pattern CLEAN_PATTERN = Pattern.compile(
            "(?i)\\s*([(\\[]?(?:official\\s+(?:music\\s+|lyric\\s+)?(?:video|audio)|music\\s+video|lyric\\s+video|lyrics|official|audio|video|visualizer|clip|video\\s+clip|mv|m/v|hd|hq|4k|live(?:\\s+performance)?|remastered|remaster|deluxe|version|bonus\\s+track|feat\\.?|ft\\.?)[^)|\\]]*[)\\]]?)+|\\s*[-|–—]\\s*(?:official\\s+(?:music\\s+|lyric\\s+)?(?:video|audio)|music\\s+video|lyric\\s+video|lyrics|official|audio|video|visualizer|clip|video\\s+clip|mv|m/v|hd|hq|4k|live|remastered).*"
    );

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]");

    public static String cleanTitle(String rawTitle) {
        if (rawTitle == null) return "";
        String cleaned = CLEAN_PATTERN.matcher(rawTitle).replaceAll("").trim();
        return cleaned.isEmpty() ? rawTitle.trim() : cleaned;
    }

    public static CompletableFuture<LyricsResult> fetchSyncedLyrics(String title, String artist, String album, double durationSec) {
        String cacheKey = (artist + " - " + title).trim().toLowerCase();
        LyricsResult cached = LyricsCache.get(cacheKey);
        if (cached != null && cached.hasLyrics()) {
            return CompletableFuture.completedFuture(cached);
        }

        String cleanedTitle = cleanTitle(title);
        String cleanedArtist = cleanTitle(artist);

        // 1. Search candidate tracks (structured search)
        return searchCandidates(cleanedTitle, cleanedArtist, durationSec)
                .thenCompose(res1 -> {
                    if (res1 != null && res1.hasLyrics()) {
                        return CompletableFuture.completedFuture(res1);
                    }
                    // 2. Fallback to freeform search query (q=artist+title)
                    return searchCandidatesFallback(cleanedTitle, cleanedArtist, durationSec);
                })
                .thenCompose(res2 -> {
                    if (res2 != null && res2.hasLyrics()) {
                        return CompletableFuture.completedFuture(res2);
                    }
                    // 3. Fallback swapped (artist, title) in case track and artist were swapped
                    return searchCandidates(cleanedArtist, cleanedTitle, durationSec);
                })
                .thenCompose(res3 -> {
                    if (res3 != null && res3.hasLyrics()) {
                        return CompletableFuture.completedFuture(res3);
                    }
                    // 4. Fallback to canonical /api/get without duration tag
                    return fetchCanonicalGet(cleanedTitle, cleanedArtist);
                })
                .thenApply(finalResult -> {
                    if (finalResult != null && finalResult.hasLyrics()) {
                        LyricsCache.put(cacheKey, finalResult);
                        JNLyricsMod.LOGGER.info("[JNLyrics] Lirik tersinkronisasi dimuat untuk: {} - {} (Durasi LRC: {}s, Media: {}s)",
                                artist, title, finalResult.lrcDurationSec(), durationSec);
                    }
                    return finalResult;
                })
                .exceptionally(ex -> {
                    JNLyricsMod.LOGGER.warn("[JNLyrics] Error saat mengambil lirik dari LRCLIB: {}", ex.getMessage());
                    return null;
                });
    }

    private static CompletableFuture<LyricsResult> searchCandidates(String trackName, String artistName, double durationSec) {
        try {
            String url = API_BASE + "/search?track_name=" + URLEncoder.encode(trackName, StandardCharsets.UTF_8)
                    + "&artist_name=" + URLEncoder.encode(artistName, StandardCharsets.UTF_8);
            return executeSearchRequest(url, durationSec);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static CompletableFuture<LyricsResult> searchCandidatesFallback(String trackName, String artistName, double durationSec) {
        try {
            String q = (artistName + " " + trackName).trim();
            String url = API_BASE + "/search?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
            return executeSearchRequest(url, durationSec);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static CompletableFuture<LyricsResult> executeSearchRequest(String url, double durationSec) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "JNLyrics-Minecraft-Mod/1.0.0")
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                            return pickBestSyncedCandidate(array, durationSec);
                        } catch (Exception ignored) {}
                    }
                    return null;
                });
    }

    private static LyricsResult pickBestSyncedCandidate(JsonArray array, double durationSec) {
        if (array == null || array.isEmpty()) return null;

        List<CandidateInfo> synced = new ArrayList<>();
        for (JsonElement el : array) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull()) {
                String lrc = obj.get("syncedLyrics").getAsString();
                if (lrc == null || lrc.isBlank()) continue;

                double dur = obj.has("duration") && !obj.get("duration").isJsonNull()
                        ? obj.get("duration").getAsDouble() : 0.0;
                Double t0 = extractFirstLyricTimestamp(lrc);
                if (t0 != null) {
                    synced.add(new CandidateInfo(obj, lrc, dur, t0));
                }
            }
        }

        if (synced.isEmpty()) return null;

        // 1. Prefer candidates whose duration is within 4.5s of track duration
        List<CandidateInfo> durationFiltered = new ArrayList<>();
        if (durationSec > 0) {
            for (CandidateInfo c : synced) {
                if (c.duration > 0 && Math.abs(c.duration - durationSec) <= 4.5) {
                    durationFiltered.add(c);
                }
            }
        }
        List<CandidateInfo> pool = !durationFiltered.isEmpty() ? durationFiltered : synced;

        // 2. Reject extreme outliers in T0 (intro time)
        if (pool.size() >= 3) {
            List<Double> t0s = new ArrayList<>();
            for (CandidateInfo c : pool) {
                t0s.add(c.t0);
            }
            Collections.sort(t0s);
            double medianT0 = t0s.get(t0s.size() / 2);

            List<CandidateInfo> t0Filtered = new ArrayList<>();
            for (CandidateInfo c : pool) {
                if (Math.abs(c.t0 - medianT0) <= 6.0) {
                    t0Filtered.add(c);
                }
            }
            if (!t0Filtered.isEmpty()) {
                pool = t0Filtered;
            }
        }

        // 3. Pick closest candidate to Spotify duration, breaking ties by preferring cleaner LRC without blank timestamps
        CandidateInfo best = pool.get(0);
        double minScore = Double.MAX_VALUE;
        for (CandidateInfo c : pool) {
            double diff = durationSec > 0 && c.duration > 0
                    ? Math.abs(c.duration - durationSec)
                    : 0.0;
            int blankCount = 0;
            for (String l : c.lrc.split("\\r?\\n")) {
                if (l.contains("]") && l.substring(l.lastIndexOf(']') + 1).trim().isEmpty()) {
                    blankCount++;
                }
            }
            double score = diff * 10.0 + blankCount * 0.5;
            if (score < minScore) {
                minScore = score;
                best = c;
            }
        }

        return new LyricsResult(best.lrc, best.duration);
    }

    private static Double extractFirstLyricTimestamp(String lrc) {
        String[] lines = lrc.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("[ti:") || line.startsWith("[ar:") || line.startsWith("[al:")) continue;
            Matcher m = TIMESTAMP_PATTERN.matcher(line);
            if (m.find()) {
                String text = line.substring(m.end()).trim();
                if (!text.isEmpty()) {
                    long min = Long.parseLong(m.group(1));
                    long sec = Long.parseLong(m.group(2));
                    long ms = 0;
                    String msStr = m.group(3);
                    if (msStr != null) {
                        if (msStr.length() == 1) ms = Long.parseLong(msStr) * 100;
                        else if (msStr.length() == 2) ms = Long.parseLong(msStr) * 10;
                        else ms = Long.parseLong(msStr.substring(0, 3));
                    }
                    return (min * 60 + sec) + (ms / 1000.0);
                }
            }
        }
        return null;
    }

    private static CompletableFuture<LyricsResult> fetchCanonicalGet(String trackName, String artistName) {
        try {
            String url = API_BASE + "/get?track_name=" + URLEncoder.encode(trackName, StandardCharsets.UTF_8)
                    + "&artist_name=" + URLEncoder.encode(artistName, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "JNLyrics-Minecraft-Mod/1.0.0")
                    .GET()
                    .build();

            return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                                if (json.has("syncedLyrics") && !json.get("syncedLyrics").isJsonNull()) {
                                    String lrc = json.get("syncedLyrics").getAsString();
                                    double dur = json.has("duration") && !json.get("duration").isJsonNull()
                                            ? json.get("duration").getAsDouble() : 0.0;
                                    return new LyricsResult(lrc, dur);
                                }
                            } catch (Exception ignored) {}
                        }
                        return null;
                    });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private record CandidateInfo(JsonObject raw, String lrc, double duration, double t0) {}
}
