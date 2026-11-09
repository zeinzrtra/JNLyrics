package net.jnlyrics.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class JNConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JNConfig instance;

    public boolean enabled = true;
    public boolean worldMode = true; // 3D floating in-world lyrics
    public boolean hudMode = true;   // Top screen Dynamic Island pill
    public boolean popupMode = true; // true = Dynamic World Floating, false = Fixed Center Billboard
    public boolean showNextLine = false;
    public float scale = 0.56f;      // Aesthetic proportion (subtle, readable and sleek)
    public float distance = 3.2f;    // Distance from player
    public int glowIntensity = 2;   // 1 to 4 passes
    public int glowColor = 0xFFD700; // Gold default (0xFFD700), Cyan (0x00E5FF), Purple (0xD000FF), Green (0x00FF88)
    public int syncOffsetMs = 0;     // Pure realtime (0ms baseline) with adaptive per-song auto-learning

    public static synchronized JNConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static File getConfigFile() {
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "jnlyrics");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "config.json");
    }

    public static JNConfig load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JNConfig cfg = GSON.fromJson(reader, JNConfig.class);
                if (cfg != null) {
                    if (cfg.scale <= 0.1f || cfg.scale > 1.5f) {
                        cfg.scale = 0.56f;
                    }
                    if (cfg.distance < 2.0f || cfg.distance > 6.0f) {
                        cfg.distance = 3.2f;
                    }
                    return cfg;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        JNConfig cfg = new JNConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        File file = getConfigFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
