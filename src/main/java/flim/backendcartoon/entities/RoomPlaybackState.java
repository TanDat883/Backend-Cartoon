package flim.backendcartoon.entities;

/**
 * Trạng thái playback hiện tại của phòng (in-memory)
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
public class RoomPlaybackState {

    private long lastPositionMs;     // Vị trí video tại thời điểm lastUpdateAtMs
    private boolean playing;         // Đang phát hay pause
    private double playbackRate;     // Tốc độ phát (1.0 = bình thường)
    private long lastUpdateAtMs;     // Thời điểm server cập nhật (System.currentTimeMillis)

    // Constructor mặc định
    public RoomPlaybackState() {
        this.lastPositionMs = 0;
        this.playing = false;
        this.playbackRate = 1.0;
        this.lastUpdateAtMs = System.currentTimeMillis();
    }

    /**
     * Tính vị trí hiện tại dựa trên thời gian đã trôi qua
     */
    public long getCurrentPositionMs() {
        if (!playing) {
            return lastPositionMs;
        }
        long elapsed = System.currentTimeMillis() - lastUpdateAtMs;
        return lastPositionMs + (long) (elapsed * playbackRate);
    }

    /**
     * Cập nhật trạng thái PLAY
     */
    public void play(long positionMs, double playbackRate) {
        this.lastPositionMs = positionMs;
        this.playing = true;
        this.playbackRate = playbackRate;
        this.lastUpdateAtMs = System.currentTimeMillis();
    }

    /**
     * Cập nhật trạng thái PAUSE
     */
    public void pause(long positionMs) {
        this.lastPositionMs = positionMs;
        this.playing = false;
        this.lastUpdateAtMs = System.currentTimeMillis();
    }

    /**
     * Cập nhật trạng thái SEEK
     */
    public void seek(long positionMs) {
        this.lastPositionMs = positionMs;
        this.lastUpdateAtMs = System.currentTimeMillis();
        // Giữ nguyên trạng thái playing/paused
    }

    // Getters và Setters
    public long getLastPositionMs() {
        return lastPositionMs;
    }

    public void setLastPositionMs(long lastPositionMs) {
        this.lastPositionMs = lastPositionMs;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public double getPlaybackRate() {
        return playbackRate;
    }

    public void setPlaybackRate(double playbackRate) {
        this.playbackRate = playbackRate;
    }

    public long getLastUpdateAtMs() {
        return lastUpdateAtMs;
    }

    public void setLastUpdateAtMs(long lastUpdateAtMs) {
        this.lastUpdateAtMs = lastUpdateAtMs;
    }
}
