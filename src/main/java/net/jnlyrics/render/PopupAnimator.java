package net.jnlyrics.render;

import java.util.Random;

public class PopupAnimator {
    private static final Random RANDOM = new Random();
    private String lastText = "";
    private long lineChangeTimeMs = 0;
    private boolean spawnOnLeft = false;

    private float currentSideX = 0f;
    private float currentSideY = 0f;

    public void onLineCheck(String currentText, boolean dynamicPopup) {
        if (currentText == null) currentText = "";
        if (!currentText.equals(lastText)) {
            lastText = currentText;
            lineChangeTimeMs = System.currentTimeMillis();

            if (dynamicPopup && !currentText.isEmpty()) {
                // Alternate between left and right side (completely clear of crosshair & player body)
                spawnOnLeft = !spawnOnLeft;
                float sideSign = spawnOnLeft ? -1.0f : 1.0f;
                // Side offset ~1.75 to ~2.05 blocks to the side (shifted slightly further out)
                currentSideX = sideSign * (1.75f + RANDOM.nextFloat() * 0.30f);
                // Subtle vertical variation (-0.04 to +0.04 blocks)
                currentSideY = -0.04f + RANDOM.nextFloat() * 0.08f;
            } else {
                currentSideX = 0f;
                currentSideY = 0f;
            }
        }
    }

    public float getScale(float baseScale) {
        long elapsed = System.currentTimeMillis() - lineChangeTimeMs;
        if (elapsed < 300) {
            float t = elapsed / 300.0f;
            // Elastic pop curve: starts at 0.60x, pops up to 1.12x, settles cleanly at 1.0x
            float bounce = (float) Math.sin(t * Math.PI * 0.68);
            return baseScale * (0.60f + 0.45f * bounce);
        }
        return baseScale;
    }

    public float getFloatUpY() {
        long elapsed = System.currentTimeMillis() - lineChangeTimeMs;
        if (elapsed < 320) {
            float t = elapsed / 320.0f;
            float smooth = t * t * (3.0f - 2.0f * t); // Smoothstep curve
            return (1.0f - smooth) * -0.15f; // starts -0.15 blocks down and rises up to settle
        }
        return 0f;
    }

    public float getBobbingY() {
        double time = System.currentTimeMillis() / 900.0;
        return (float) Math.sin(time) * 0.025f;
    }

    public float getSideX() {
        return currentSideX;
    }

    public float getSideY() {
        return currentSideY;
    }
}
