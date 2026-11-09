package net.jnlyrics.lyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.jnlyrics.JNLyricsMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SongOffsetManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Integer> SONG_OFFSETS = new ConcurrentHashMap<>();
    private static boolean loaded = false;

    private static File getFile() {
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "jnlyrics");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "song_offsets.json");
    }

    public static synchronized void load() {
        if (loaded) return;
        File file = getFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, Integer>>() {}.getType();
                Map<String, Integer> map = GSON.fromJson(reader, type);
                if (map != null) {
                    SONG_OFFSETS.putAll(map);
                }
            } catch (Exception e) {
                JNLyricsMod.LOGGER.warn("[JNLyrics] Gagal membaca song_offsets.json: {}", e.getMessage());
            }
        }
        loaded = true;
    }

    public static synchronized void save() {
        File file = getFile();
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(SONG_OFFSETS, writer);
        } catch (Exception e) {
            JNLyricsMod.LOGGER.warn("[JNLyrics] Gagal menyimpan song_offsets.json: {}", e.getMessage());
        }
    }

    public static String makeKey(String artist, String title) {
        if (artist == null) artist = "";
        if (title == null) title = "";
        return (artist.trim() + " - " + title.trim()).toLowerCase();
    }

    public static boolean hasCustomOffset(String artist, String title) {
        load();
        return SONG_OFFSETS.containsKey(makeKey(artist, title));
    }

    public static int getOffset(String artist, String title, int fallbackDefault) {
        load();
        String key = makeKey(artist, title);
        return SONG_OFFSETS.getOrDefault(key, fallbackDefault);
    }

    public static void setOffset(String artist, String title, int offsetMs) {
        load();
        String key = makeKey(artist, title);
        SONG_OFFSETS.put(key, offsetMs);
        save();
    }

    public static void removeOffset(String artist, String title) {
        load();
        String key = makeKey(artist, title);
        if (SONG_OFFSETS.remove(key) != null) {
            save();
        }
    }
}

