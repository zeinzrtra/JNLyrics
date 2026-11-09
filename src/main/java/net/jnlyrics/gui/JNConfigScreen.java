package net.jnlyrics.gui;

import net.jnlyrics.config.JNConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        super(Component.literal("Pengaturan JNLyrics"));
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
        this.addRenderableWidget(Button.builder(
                Component.literal("Lirik 3D Dunia: " + (tempWorldMode ? "AKTIF" : "NONAKTIF")),
                btn -> {
                    tempWorldMode = !tempWorldMode;
                    btn.setMessage(Component.literal("Lirik 3D Dunia: " + (tempWorldMode ? "AKTIF" : "NONAKTIF")));
                }
        ).bounds(widgetX, startY, widgetWidth, widgetHeight).build());

        // 2. Toggle HUD Island Pill
        this.addRenderableWidget(Button.builder(
                Component.literal("Bilah HUD Atas: " + (tempHudMode ? "AKTIF" : "NONAKTIF")),
                btn -> {
                    tempHudMode = !tempHudMode;
                    btn.setMessage(Component.literal("Bilah HUD Atas: " + (tempHudMode ? "AKTIF" : "NONAKTIF")));
                }
        ).bounds(widgetX, startY + spacing, widgetWidth, widgetHeight).build());

        // 3. Toggle Dynamic Pop-up Mode (3D)
        this.addRenderableWidget(Button.builder(
                Component.literal("Animasi Pop-Up: " + (tempPopupMode ? "Dinamis Melayang" : "Pusat")),
                btn -> {
                    tempPopupMode = !tempPopupMode;
                    btn.setMessage(Component.literal("Animasi Pop-Up: " + (tempPopupMode ? "Dinamis Melayang" : "Pusat")));
                }
        ).bounds(widgetX, startY + spacing * 2, widgetWidth, widgetHeight).build());

        // 4. Toggle Next Line Preview
        this.addRenderableWidget(Button.builder(
                Component.literal("Pratinjau Baris Berikut: " + (tempShowNextLine ? "AKTIF" : "NONAKTIF")),
                btn -> {
                    tempShowNextLine = !tempShowNextLine;
                    btn.setMessage(Component.literal("Pratinjau Baris Berikut: " + (tempShowNextLine ? "AKTIF" : "NONAKTIF")));
                }
        ).bounds(widgetX, startY + spacing * 3, widgetWidth, widgetHeight).build());

        // 5. Glow Color Selector
        this.addRenderableWidget(Button.builder(
                Component.literal("Warna Cahaya: " + COLOR_NAMES[colorIndex]),
                btn -> {
                    colorIndex = (colorIndex + 1) % GLOW_COLORS.length;
                    tempGlowColor = GLOW_COLORS[colorIndex];
                    btn.setMessage(Component.literal("Warna Cahaya: " + COLOR_NAMES[colorIndex]));
                }
        ).bounds(widgetX, startY + spacing * 4, widgetWidth, widgetHeight).build());

        // 6. Interactive Offset Slider (-1000ms to +1000ms, center is 0ms)
        this.addRenderableWidget(new OffsetSlider(
                widgetX, startY + spacing * 5, widgetWidth, widgetHeight, tempSyncOffsetMs
        ));

        // 7. Footer: Batal (Kiri) & Simpan (Kanan)
        int footerY = cardY + cardHeight - 27;
        int footerBtnWidth = 111;

        // Button Batal: discard temporary state and close
        this.addRenderableWidget(Button.builder(
                Component.literal("§cBatal"),
                btn -> this.closeScreen()
        ).bounds(cardX + 15, footerY, footerBtnWidth, widgetHeight).build());

        // Button Simpan: commit temporary state to config and save
        this.addRenderableWidget(Button.builder(
                Component.literal("§aSimpan"),
                btn -> this.saveAndClose()
        ).bounds(cardX + cardWidth - 15 - footerBtnWidth, footerY, footerBtnWidth, widgetHeight).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int cardWidth = 260;
        int cardHeight = 224;
        int cardX = (this.width - cardWidth) / 2;
        int cardY = (this.height - cardHeight) / 2;

        // 1. Modern dark frosted glass container card ("div di belakangnya")
        // Outer shadow
        graphics.fill(cardX - 1, cardY - 1, cardX + cardWidth + 1, cardY + cardHeight + 1, 0x50000000);
        // Card background body
        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0xE6101216);
        // Border outline
        graphics.outline(cardX, cardY, cardWidth, cardHeight, 0x35FFFFFF);

        // Header section of the card
        graphics.fill(cardX + 1, cardY + 1, cardX + cardWidth - 1, cardY + 28, 0x1AFFFFFF);
        graphics.horizontalLine(cardX, cardX + cardWidth, cardY + 28, 0x30FFFFFF);
        graphics.centeredText(this.font, "§a♪ §fPengaturan JNLyrics", this.width / 2, cardY + 10, 0xFFFFFFFF);

        // Footer separator line
        int footerSepY = cardY + cardHeight - 34;
        graphics.horizontalLine(cardX, cardX + cardWidth, footerSepY, 0x20FFFFFF);

        // 2. Render child widgets (buttons, slider)
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void saveAndClose() {
        config.worldMode = tempWorldMode;
        config.hudMode = tempHudMode;
        config.popupMode = tempPopupMode;
        config.showNextLine = tempShowNextLine;
        config.glowColor = tempGlowColor;
        config.syncOffsetMs = tempSyncOffsetMs;
        config.save();
        closeScreen();
    }

    @Override
    public void onClose() {
        // ESC acts as cancel (discards temp changes)
        closeScreen();
    }

    private void closeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    private class OffsetSlider extends AbstractSliderButton {
        private static final double MIN_OFFSET = -1000.0;
        private static final double MAX_OFFSET = 1000.0;

        public OffsetSlider(int x, int y, int width, int height, int initialOffsetMs) {
            super(x, y, width, height, Component.empty(), toNormalized(initialOffsetMs));
            updateMessage();
        }

        private static double toNormalized(int offsetMs) {
            double clamped = Math.clamp(offsetMs, MIN_OFFSET, MAX_OFFSET);
            return (clamped - MIN_OFFSET) / (MAX_OFFSET - MIN_OFFSET);
        }

        private int getOffsetMs() {
            double raw = MIN_OFFSET + this.value * (MAX_OFFSET - MIN_OFFSET);
            return (int) (Math.round(raw / 50.0) * 50);
        }

        @Override
        protected void updateMessage() {
            int ms = getOffsetMs();
            String label;
            if (ms == 0) {
                label = "§fSinkronisasi: §b0ms (Realtime)";
            } else if (ms > 0) {
                label = "§fSinkronisasi: §a+" + ms + "ms (Lebih Cepat)";
            } else {
                label = "§fSinkronisasi: §e" + ms + "ms (Lebih Lambat)";
            }
            this.setMessage(Component.literal(label));
        }

        @Override
        protected void applyValue() {
            tempSyncOffsetMs = getOffsetMs();
        }
    }
}
