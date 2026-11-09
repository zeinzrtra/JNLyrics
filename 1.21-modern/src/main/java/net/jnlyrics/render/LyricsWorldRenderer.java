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

        // In 1.21: getHorizontalPlane() = fwd, getVerticalPlane() = up, getDiagonalPlane() = left
        Vector3f fwd = camera.getHorizontalPlane();
        Vector3f up = camera.getVerticalPlane();
        Vector3f left = camera.getDiagonalPlane(); // right is -left

        float distance = config.distance > 0 ? config.distance : 3.2f;
        float baseScale = config.scale > 0 ? config.scale : 0.56f;
        String glowPrefix = getGlowPrefix(config.glowColor);

        // Render active line (Camera-relative, pops directly at designated left/right side)
        float sideX = config.popupMode ? ANIMATOR.getSideX() : 0f;
        float sideY = config.popupMode ? ANIMATOR.getSideY() : 0f;
        float bobbing = ANIMATOR.getBobbingY();
        float floatUp = ANIMATOR.getFloatUpY();

        double offX = (-left.x() * sideX) + (up.x() * (sideY + bobbing + floatUp));
        double offY = (-left.y() * sideX) + (up.y() * (sideY + bobbing + floatUp));
        double offZ = (-left.z() * sideX) + (up.z() * (sideY + bobbing + floatUp));

        double posX = fwd.x() * distance + offX;
        double posY = fwd.y() * distance + offY - 0.12;
        double posZ = fwd.z() * distance + offZ;

        float activeScale = ANIMATOR.getScale(baseScale) * (distance / 2.8f);
        int bg = (int)(client.options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;

        renderTextSentence(matrices, consumers, client.textRenderer, camera, activeText,
                posX, posY, posZ, activeScale, glowPrefix, bg);

        // Render next line preview if enabled
        if (config.showNextLine && nextLine != null && !nextLine.text().isBlank()) {
            double nextY = posY - 0.32;
            renderTextSentence(matrices, consumers, client.textRenderer, camera, nextLine.text().trim(),
                    posX, nextY, posZ, activeScale * 0.85f, "§7", bg);
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private static void renderTextSentence(MatrixStack matrices, VertexConsumerProvider consumers,
                                           TextRenderer textRenderer, Camera camera, String text,
                                           double x, double y, double z,
                                           float scale, String colorPrefix, int bg) {
        List<String> lines = wrapText(text, 26);

        matrices.push();
        matrices.translate(x, y, z);

        for (int i = 0; i < lines.size(); i++) {
            matrices.push();
            // Explicit vertical separation in world space: 0.20 blocks between lines (never overlaps)
            matrices.translate(0, -i * 0.20, 0);
            matrices.multiply(camera.getRotation());
            matrices.scale(scale * 0.025f, -scale * 0.025f, scale * 0.025f);

            String lineStr = colorPrefix + lines.get(i);
            float textX = -textRenderer.getWidth(lineStr) / 2.0f;
            Text comp = Text.literal(lineStr);

            // Exact standard Minecraft nametag draw (1 clean pass with NORMAL layer and 25% background)
            textRenderer.draw(
                    comp,
                    textX,
                    0,
                    0xFFFFFFFF,
                    false,
                    matrices.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.NORMAL,
                    bg,
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

    public static String getGlowPrefix(int color) {
        return switch (color) {
            case 0x00E5FF -> "§b§l"; // Neon Cyan
            case 0xFFD700 -> "§6§l"; // Gold
            case 0xD000FF -> "§d§l"; // Neon Purple
            case 0x00FF88 -> "§a§l"; // Emerald Green
            default -> "§f§l";       // Crystal White
        };
    }
}
