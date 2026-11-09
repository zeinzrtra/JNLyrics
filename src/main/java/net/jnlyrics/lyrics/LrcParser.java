package net.jnlyrics.lyrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcParser {
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]");
    private static final Pattern OFFSET_PATTERN = Pattern.compile("(?i)^\\[offset:\\s*([+-]?\\d+)\\]");

    public static List<LyricLine> parse(String lrcContent) {
        return parse(lrcContent, 0, 0, 0);
    }

    public static List<LyricLine> parse(String lrcContent, long externalOffsetMs) {
        return parse(lrcContent, externalOffsetMs, 0, 0);
    }

    public static List<LyricLine> parse(String lrcContent, long externalOffsetMs, double sourceDurationSec, double targetDurationSec) {
        List<LyricLine> list = new ArrayList<>();
        if (lrcContent == null || lrcContent.isBlank()) {
            return list;
        }

        long fileOffsetMs = 0;
        String[] lines = lrcContent.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            Matcher offsetMatcher = OFFSET_PATTERN.matcher(line);
            if (offsetMatcher.find()) {
                try {
                    fileOffsetMs = Long.parseLong(offsetMatcher.group(1));
                } catch (Exception ignored) {}
                continue;
            }

            if (line.startsWith("[ti:") || line.startsWith("[ar:") || line.startsWith("[al:") || line.startsWith("[by:")) {
                continue;
            }

            Matcher matcher = TIMESTAMP_PATTERN.matcher(line);
            List<Long> timestamps = new ArrayList<>();
            int lastMatchEnd = 0;

            while (matcher.find()) {
                long min = Long.parseLong(matcher.group(1));
                long sec = Long.parseLong(matcher.group(2));
                long ms = 0;
                String msStr = matcher.group(3);
                if (msStr != null) {
                    if (msStr.length() == 1) ms = Long.parseLong(msStr) * 100;
                    else if (msStr.length() == 2) ms = Long.parseLong(msStr) * 10;
                    else ms = Long.parseLong(msStr.substring(0, 3));
                }
                long totalMs = (min * 60 + sec) * 1000 + ms;
                // Standard LRC: positive offset means lyrics display earlier, so subtract fileOffset
                long adjustedMs = Math.max(0, totalMs - fileOffsetMs + externalOffsetMs);
                timestamps.add(adjustedMs);
                lastMatchEnd = matcher.end();
            }

            String text = line.substring(lastMatchEnd).trim();
            for (long time : timestamps) {
                list.add(new LyricLine(time, text));
            }
        }

        Collections.sort(list);
        return list;
    }
}
