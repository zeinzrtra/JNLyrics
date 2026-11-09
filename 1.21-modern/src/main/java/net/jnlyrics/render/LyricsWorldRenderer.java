package net.jnlyrics.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.jnlyrics.config.JNConfig;
import net.jnlyrics.lyrics.LyricLine;
import net.jnlyrics.lyrics.LyricsManager;
import net.jnlyrics.media.MediaManager;
import net.jnlyrics.media.TrackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class LyricsWorldRenderer {
    private static final PopupAnimator ANIMATOR = new PopupAnimator();
    private static String prevActiveText = "";

    public static void render(WorldRenderContext context) {
        JNConfig config = JNConfig.getInstance();
        if (!config.enabled || !config.worldMode) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TrackInfo track = MediaManager.getInstance().getTrack();
        if (track == null || !track.isValidTrack()) {
            prevActiveText = "";
            return;
        }

        LyricsManager lyricsMgr = LyricsManager.getInstance();
        lyricsMgr.updateTrack(track);

        long posMs = track.getEstimatedPositionMs();
        LyricLine currentLine = lyricsMgr.getCurrentLine(posMs);
        LyricLine nextLine = lyricsMgr.getNextLine(posMs);

        String activeText = currentLine != null ? currentLine.text().trim() : "";
        if (activeText.isEmpty()) {
            if (lyricsMgr.isLoading()) {
                activeText = "§7Memuat lirik...";
            } else if (!lyricsMgr.hasLyrics()) {
                activeText = "§6♪ §f" + track.getArtist() + " - " + track.getTitle();
            } else {
                prevActiveText = "";
                return;
            }
        }

        // Trigger animation on new line check
        if (!activeText.equals(prevActiveText)) {
            prevActiveText = activeText;
            ANIMATOR.onLineCheck(activeText, config.popupMode);
        }

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || camera == null || consumers == null) return;

        Quaternionf rotation = camera.getRotation();
        Vector3f fwd = rotation.transform(new Vector3f(0, 0, -1));
        Vector3f up = rotation.transform(new Vector3f(0, 1, 0));
        Vector3f right = rotation.transform(new Vector3f(1, 0, 0));

        float distance = config.distance > 0 ? config.distance : 3.2f;
        float baseScale = (config.scale > 0 ? config.scale : 0.56f) * 0.025f;
        String glowPrefix = getGlowPrefix(config.glowColor);

        // Render active line (Camera-relative, pops directly at designated left/right side)
        float sideX = config.popupMode ? ANIMATOR.getSideX() : 0f;
        float sideY = config.popupMode ? ANIMATOR.getSideY() : 0f;
        float bobbing = ANIMATOR.getBobbingY();
        float floatUp = ANIMATOR.getFloatUpY();

        double offX = (right.x() * sideX) + (up.x() * (sideY + bobbing + floatUp));
        double offY = (right.y() * sideX) + (up.y() * (sideY + bobbing + floatUp));
        double offZ = (right.z() * sideX) + (up.z() * (sideY + bobbing + floatUp));

        double posX = fwd.x() * distance + offX;
        double posY = fwd.y() * distance + offY - 0.12;
        double posZ = fwd.z() * distance + offZ;

        float activeScale = ANIMATOR.getScale(baseScale) * (distance / 2.8f);

        renderTextSentence(matrices, consumers, client.textRenderer, camera, activeText,
                posX, posY, posZ, activeScale, glowPrefix);

        // Render next line preview if enabled
        if (config.showNextLine && nextLine != null && !nextLine.text().isBlank()) {
            double nextY = posY - 0.32;
            renderTextSentence(matrices, consumers, client.textRenderer, camera, nextLine.text().trim(),
                    posX, nextY, posZ, activeScale * 0.85f, "§7");
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private static void renderTextSentence(MatrixStack matrices, VertexConsumerProvider consumers,
                                           TextRenderer textRenderer, Camera camera, String text,
                                           double x, double y, double z,
                                           float scale, String colorPrefix) {
        List<String> lines = wrapText(text, 26);

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());
        matrices.scale(scale, -scale, scale);

        for (int i = 0; i < lines.size(); i++) {
            matrices.push();
            matrices.translate(0, i * 11.0f, 0);
            String lineStr = colorPrefix + lines.get(i);
            float textX = -textRenderer.getWidth(lineStr) / 2.0f;
            Text textComponent = Text.literal(lineStr);

            // 1. Background plate (dark semi-transparent box)
            textRenderer.draw(
                    textComponent,
                    textX,
                    0,
                    0x20FFFFFF,
                    false,
                    matrices.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0x80000000,
                    0xF000F0
            );

            // 2. Solid crisp foreground text (exact normal nametag layer, fully opaque & vibrant, not washed out by water/sky)
            textRenderer.draw(
                    textComponent,
                    textX,
                    0,
                    0xFFFFFFFF,
                    false,
                    matrices.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    0xF000F0
            );

            matrices.pop();
        }

        matrices.pop();
    }

    public static List<String> wrapText(String text, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        if (text.length() <= maxCharsPerLine) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() + word.length() + 1 > maxCharsPerLine && current.length() > 0) {
                lines.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (current.length() > 0) current.append(" ");
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString().trim());
        }
        return lines;
    }

    private static String getGlowPrefix(int hexColor) {
        int r = (hexColor >> 16) & 0xFF;
        int g = (hexColor >> 8) & 0xFF;
        int b = hexColor & 0xFF;

        if (r > 200 && g > 180 && b < 100) return "§6"; // Gold
        if (r < 100 && g > 200 && b > 200) return "§b"; // Cyan
        if (r > 180 && g < 100 && b > 180) return "§d"; // Purple
        if (r < 100 && g > 200 && b < 150) return "§a"; // Green
        return "§f"; // White
    }
}
