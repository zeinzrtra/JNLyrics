package net.jnlyrics.gui;

import net.jnlyrics.config.JNConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class JNConfigScreen extends Screen {
    private final Screen parent;
    private final JNConfig config = JNConfig.getInstance();

    // Temporary working state (buffered until user clicks "Simpan")
    private boolean tempWorldMode;
    private boolean tempHudMode;
    private boolean tempPopupMode;
    private boolean tempShowNextLine;
    private int tempGlowColor;
    private int tempSyncOffsetMs;
    private int colorIndex = 0;

    private static final int[] GLOW_COLORS = {
            0xFFD700, // Gold
            0x00E5FF, // Neon Cyan
            0xD000FF, // Neon Purple
            0x00FF88, // Emerald Green
            0xFFFFFF  // Crystal White
    };

    private static final String[] COLOR_NAMES = {
            "Emas Berkilau",
            "Biru Neon",
            "Ungu Neon",
            "Hijau Zamrud",
            "Putih Kristal"
    };

    public JNConfigScreen(Screen parent) {
        super(Text.literal("Pengaturan JNLyrics"));
        this.parent = parent;

        // Copy current config values into temporary session buffer
        this.tempWorldMode = config.worldMode;
        this.tempHudMode = config.hudMode;
        this.tempPopupMode = config.popupMode;
        this.tempShowNextLine = config.showNextLine;
        this.tempGlowColor = config.glowColor;
        this.tempSyncOffsetMs = config.syncOffsetMs;

        for (int i = 0; i < GLOW_COLORS.length; i++) {
            if (GLOW_COLORS[i] == tempGlowColor) {
                this.colorIndex = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        int cardWidth = 260;
        int cardHeight = 224;
        int cardX = (this.width - cardWidth) / 2;
        int cardY = (this.height - cardHeight) / 2;

        int widgetWidth = 230;
        int widgetHeight = 20;
        int widgetX = cardX + (cardWidth - widgetWidth) / 2;
        int startY = cardY + 35;
        int spacing = 24;

        // 1. Toggle 3D In-World Lyrics
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Lirik 3D Dunia: " + (tempWorldMode ? "§aAKTIF" : "§cNONAKTIF")),
                btn -> {
                    tempWorldMode = !tempWorldMode;
                    btn.setMessage(Text.literal("Lirik 3D Dunia: " + (tempWorldMode ? "§aAKTIF" : "§cNONAKTIF")));
                }
        ).dimensions(widgetX, startY, widgetWidth, widgetHeight).build());

        // 2. Toggle HUD Island Pill
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Bilah HUD Atas: " + (tempHudMode ? "§aAKTIF" : "§cNONAKTIF")),
                btn -> {
                    tempHudMode = !tempHudMode;
                    btn.setMessage(Text.literal("Bilah HUD Atas: " + (tempHudMode ? "§aAKTIF" : "§cNONAKTIF")));
                }
        ).dimensions(widgetX, startY + spacing, widgetWidth, widgetHeight).build());

        // 3. Toggle Dynamic Pop-up Mode (3D)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Animasi Pop-Up: " + (tempPopupMode ? "§bDinamis Melayang" : "§7Pusat")),
                btn -> {
                    tempPopupMode = !tempPopupMode;
                    btn.setMessage(Text.literal("Animasi Pop-Up: " + (tempPopupMode ? "§bDinamis Melayang" : "§7Pusat")));
                }
        ).dimensions(widgetX, startY + spacing * 2, widgetWidth, widgetHeight).build());

        // 4. Toggle Next Line Preview
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Pratinjau Baris Berikut: " + (tempShowNextLine ? "§aAKTIF" : "§cNONAKTIF")),
                btn -> {
                    tempShowNextLine = !tempShowNextLine;
                    btn.setMessage(Text.literal("Pratinjau Baris Berikut: " + (tempShowNextLine ? "§aAKTIF" : "§cNONAKTIF")));
                }
        ).dimensions(widgetX, startY + spacing * 3, widgetWidth, widgetHeight).build());

        // 5. Glow Color Cycle
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Warna Cahaya: §e" + COLOR_NAMES[colorIndex]),
                btn -> {
                    colorIndex = (colorIndex + 1) % GLOW_COLORS.length;
                    tempGlowColor = GLOW_COLORS[colorIndex];
                    btn.setMessage(Text.literal("Warna Cahaya: §e" + COLOR_NAMES[colorIndex]));
                }
        ).dimensions(widgetX, startY + spacing * 4, widgetWidth, widgetHeight).build());

        // 6. Interactive Sync Offset Slider (-1000ms to +1000ms)
        double initialSliderNorm = Math.min(1.0, Math.max(0.0, (tempSyncOffsetMs + 1000.0) / 2000.0));
        this.addDrawableChild(new OffsetSlider(widgetX, startY + spacing * 5, widgetWidth, widgetHeight, initialSliderNorm));

        // 7. Footer: Batal (Cancel) & Simpan (Save)
        int btnWidth = 110;
        int btnY = cardY + cardHeight - 28;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§cBatal"),
                btn -> {
                    if (this.client != null) {
                        this.client.setScreen(this.parent);
                    }
                }
        ).dimensions(widgetX, btnY, btnWidth, widgetHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§aSimpan"),
                btn -> {
                    config.worldMode = tempWorldMode;
                    config.hudMode = tempHudMode;
                    config.popupMode = tempPopupMode;
                    config.showNextLine = tempShowNextLine;
                    config.glowColor = tempGlowColor;
                    config.syncOffsetMs = tempSyncOffsetMs;
                    config.save();

                    if (this.client != null) {
                        if (this.client.player != null) {
                            this.client.player.sendMessage(Text.literal("§a[JNLyrics] Pengaturan berhasil disimpan!"), true);
                        }
                        this.client.setScreen(this.parent);
                    }
                }
        ).dimensions(widgetX + widgetWidth - btnWidth, btnY, btnWidth, widgetHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Frosted card background
        int cardWidth = 260;
        int cardHeight = 224;
        int cardX = (this.width - cardWidth) / 2;
        int cardY = (this.height - cardHeight) / 2;

        context.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0xE6101216);
        context.drawBorder(cardX, cardY, cardWidth, cardHeight, 0x4D00E5FF);

        // Header Title
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                "§6♪ §fPengaturan JNLyrics",
                this.width / 2,
                cardY + 14,
                0xFFFFFFFF
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private class OffsetSlider extends SliderWidget {
        public OffsetSlider(int x, int y, int width, int height, double value) {
            super(x, y, width, height, Text.empty(), value);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int ms = (int) Math.round((this.value * 2000.0) - 1000.0);
            String label = (ms > 0) ? ("+" + ms + "ms (Maju)")
                    : (ms < 0) ? (ms + "ms (Mundur)")
                    : "0ms (Realtime)";
            this.setMessage(Text.literal("Sinkronisasi: §b" + label));
        }

        @Override
        protected void applyValue() {
            tempSyncOffsetMs = (int) Math.round((this.value * 2000.0) - 1000.0);
        }
    }
}
