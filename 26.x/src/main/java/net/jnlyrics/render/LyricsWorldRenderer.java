package net.jnlyrics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.jnlyrics.config.JNConfig;
import net.jnlyrics.lyrics.LyricLine;
import net.jnlyrics.lyrics.LyricsManager;
import net.jnlyrics.media.MediaManager;
import net.jnlyrics.media.TrackInfo;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

public class LyricsWorldRenderer {
    private static final PopupAnimator ANIMATOR = new PopupAnimator();
    private static String prevActiveText = "";

    public static void render(LevelRenderContext context) {
        JNConfig config = JNConfig.getInstance();
        if (!config.enabled || !config.worldMode) return;

        Minecraft client = Minecraft.getInstance();
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

        LevelRenderState levelState = context.levelState();
        if (levelState == null || levelState.cameraRenderState == null) return;
        CameraRenderState cameraState = levelState.cameraRenderState;

        SubmitNodeCollector collector = context.submitNodeCollector();
        if (collector == null) return;

        PoseStack poseStack = context.poseStack();
        if (poseStack == null) {
            poseStack = new PoseStack();
        }

        Camera camera = client.gameRenderer.mainCamera();
        if (!camera.isInitialized()) return;

        Vector3fc fwd = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3fc left = camera.leftVector(); // right is -left

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

        renderTextSentence(poseStack, collector, cameraState, activeText,
                posX, posY, posZ, activeScale, glowPrefix);

        // Render next line preview if enabled
        if (config.showNextLine && nextLine != null && !nextLine.text().isBlank()) {
            double nextY = posY - 0.32;
            renderTextSentence(poseStack, collector, cameraState, nextLine.text().trim(),
                    posX, nextY, posZ, activeScale * 0.85f, "§7");
        }
    }

    private static void renderTextSentence(PoseStack poseStack, SubmitNodeCollector collector,
                                          CameraRenderState cameraState, String text,
                                          double x, double y, double z,
                                          float scale, String colorPrefix) {
        List<String> lines = wrapText(text, 26);

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        for (int i = 0; i < lines.size(); i++) {
            poseStack.pushPose();
            // Explicit vertical separation in world space: 0.20 blocks between lines (never overlaps)
            poseStack.translate(0, -i * 0.20, 0);
            poseStack.scale(scale, scale, scale);
            Component comp = Component.literal(colorPrefix + lines.get(i));
            collector.submitNameTag(poseStack, Vec3.ZERO, 0, comp, true, 0xF000F0, cameraState);
            poseStack.popPose();
        }

        poseStack.popPose();
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
