package net.jnlyrics.media;

public class MediaManager {
    private static MediaManager instance;
    private final WindowsMediaProvider provider;

    private MediaManager() {
        this.provider = new WindowsMediaProvider();
        this.provider.init();
    }

    public static synchronized MediaManager getInstance() {
        if (instance == null) {
            instance = new MediaManager();
        }
        return instance;
    }

    public TrackInfo getTrack() {
        return provider.getCurrentTrack();
    }

    public void stop() {
        provider.close();
    }
}

