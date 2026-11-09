package net.jnlyrics.lyrics;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class LyricsCache {
    private static File getCacheDir() {
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "jnlyrics/cache");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static LyricsResult get(String key) {
        File file = new File(getCacheDir(), sanitize(key) + ".lrc");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                double duration = 0.0;
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#duration:")) {
                        try {
                            duration = Double.parseDouble(line.substring(10).trim());
                        } catch (Exception ignored) {}
                        continue;
                    }
                    sb.append(line).append("\n");
                }
                String text = sb.toString().trim();
                if (!text.isEmpty()) {
                    return new LyricsResult(text, duration);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void put(String key, LyricsResult result) {
        if (result == null || !result.hasLyrics()) return;
        File file = new File(getCacheDir(), sanitize(key) + ".lrc");
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            if (result.lrcDurationSec() > 0) {
                writer.write("#duration: " + result.lrcDurationSec() + "\n");
            }
            writer.write(result.syncedLyrics());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String sanitize(String key) {
        return key.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
