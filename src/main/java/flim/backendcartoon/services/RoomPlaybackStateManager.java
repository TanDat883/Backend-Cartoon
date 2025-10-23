package flim.backendcartoon.services;

import flim.backendcartoon.entities.RoomPlaybackState;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý trạng thái playback của các phòng xem chung (in-memory)
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
@Service
public class RoomPlaybackStateManager {

    private final ConcurrentHashMap<String, RoomPlaybackState> roomStates = new ConcurrentHashMap<>();

    /**
     * Lấy hoặc tạo state mới cho phòng
     */
    public RoomPlaybackState getOrCreate(String roomId) {
        return roomStates.computeIfAbsent(roomId, k -> new RoomPlaybackState());
    }

    /**
     * Lấy state hiện tại (không tạo mới)
     */
    public RoomPlaybackState get(String roomId) {
        return roomStates.get(roomId);
    }

    /**
     * Cập nhật trạng thái PLAY
     */
    public void handlePlay(String roomId, long positionMs, double playbackRate) {
        RoomPlaybackState state = getOrCreate(roomId);
        state.play(positionMs, playbackRate);
    }

    /**
     * Cập nhật trạng thái PAUSE
     */
    public void handlePause(String roomId, long positionMs) {
        RoomPlaybackState state = getOrCreate(roomId);
        state.pause(positionMs);
    }

    /**
     * Cập nhật trạng thái SEEK
     */
    public void handleSeek(String roomId, long positionMs) {
        RoomPlaybackState state = getOrCreate(roomId);
        state.seek(positionMs);
    }

    /**
     * Xóa state khi phòng kết thúc
     */
    public void remove(String roomId) {
        roomStates.remove(roomId);
    }

    /**
     * Lấy sync state để gửi cho client mới join
     */
    public SyncStateDto getSyncState(String roomId) {
        RoomPlaybackState state = get(roomId);
        if (state == null) {
            state = getOrCreate(roomId);
        }

        SyncStateDto dto = new SyncStateDto();
        dto.setPlaying(state.isPlaying());
        dto.setPositionMs(state.getCurrentPositionMs());
        dto.setPlaybackRate(state.getPlaybackRate());
        dto.setServerTimeMs(System.currentTimeMillis());

        return dto;
    }

    /**
     * DTO cho SYNC_STATE
     */
    public static class SyncStateDto {
        private boolean playing;
        private long positionMs;
        private double playbackRate;
        private long serverTimeMs;

        public boolean isPlaying() {
            return playing;
        }

        public void setPlaying(boolean playing) {
            this.playing = playing;
        }

        public long getPositionMs() {
            return positionMs;
        }

        public void setPositionMs(long positionMs) {
            this.positionMs = positionMs;
        }

        public double getPlaybackRate() {
            return playbackRate;
        }

        public void setPlaybackRate(double playbackRate) {
            this.playbackRate = playbackRate;
        }

        public long getServerTimeMs() {
            return serverTimeMs;
        }

        public void setServerTimeMs(long serverTimeMs) {
            this.serverTimeMs = serverTimeMs;
        }
    }
}

